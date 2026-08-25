#include "sr/d3d12/d3d12_runtime.h"

#include "sr/sr_api_types.h"

#include <dxgi1_6.h>

#include <algorithm>
#include <array>
#include <atomic>
#include <cstdio>
#include <cstring>
#include <limits>
#include <memory>
#include <mutex>
#include <new>
#include <sstream>
#include <string>
#include <string_view>
#include <unordered_map>
#include <utility>
#include <vector>

namespace sr::d3d12 {
    namespace {
        constexpr size_t LAST_ERROR_CAPACITY = 8192;
        thread_local std::array<char, LAST_ERROR_CAPACITY> g_lastError{};

        constexpr uint64_t OBJECT_MAGIC = 0x535244334431324FULL;
        constexpr uint32_t VALID_DEBUG_FLAGS = DEBUG_LAYER | DEBUG_GPU_VALIDATION |
                                               DEBUG_DRED;

        enum class ObjectKind : uint32_t {
            Device,
            SharedFence,
            Texture2D,
            Buffer,
            CommandAllocator,
            CommandList,
        };

        struct ObjectHeader {
            explicit ObjectHeader(ObjectKind objectKind) : kind(objectKind) {
            }

            uint64_t magic = OBJECT_MAGIC;
            ObjectKind kind;
        };

        template<typename T>
        class ComHandle {
        public:
            ComHandle() = default;

            explicit ComHandle(T *pointer) noexcept : pointer_(pointer) {
            }

            ~ComHandle() { reset(); }

            ComHandle(const ComHandle &) = delete;
            ComHandle &operator=(const ComHandle &) = delete;

            ComHandle(ComHandle &&other) noexcept : pointer_(other.detach()) {
            }

            ComHandle &operator=(ComHandle &&other) noexcept {
                if (this != &other) {
                    reset(other.detach());
                }
                return *this;
            }

            T *get() const noexcept { return pointer_; }
            T *operator->() const noexcept { return pointer_; }
            explicit operator bool() const noexcept { return pointer_ != nullptr; }

            T **put() noexcept {
                reset();
                return &pointer_;
            }

            T *detach() noexcept {
                T *pointer = pointer_;
                pointer_ = nullptr;
                return pointer;
            }

            void reset(T *pointer = nullptr) noexcept {
                if (pointer_) {
                    pointer_->Release();
                }
                pointer_ = pointer;
            }

            static ComHandle retain(T *pointer) noexcept {
                if (pointer) {
                    pointer->AddRef();
                }
                return ComHandle(pointer);
            }

        private:
            T *pointer_ = nullptr;
        };

        class UniqueHandle {
        public:
            UniqueHandle() = default;

            explicit UniqueHandle(HANDLE handle) noexcept : handle_(handle) {
            }

            ~UniqueHandle() { reset(); }

            UniqueHandle(const UniqueHandle &) = delete;
            UniqueHandle &operator=(const UniqueHandle &) = delete;

            UniqueHandle(UniqueHandle &&other) noexcept : handle_(other.detach()) {
            }

            UniqueHandle &operator=(UniqueHandle &&other) noexcept {
                if (this != &other) {
                    reset(other.detach());
                }
                return *this;
            }

            HANDLE get() const noexcept { return handle_; }
            explicit operator bool() const noexcept {
                return handle_ != nullptr && handle_ != INVALID_HANDLE_VALUE;
            }

            HANDLE *put() noexcept {
                reset();
                return &handle_;
            }

            HANDLE detach() noexcept {
                HANDLE handle = handle_;
                handle_ = nullptr;
                return handle;
            }

            void reset(HANDLE handle = nullptr) noexcept {
                if (handle_ && handle_ != INVALID_HANDLE_VALUE) {
                    CloseHandle(handle_);
                }
                handle_ = handle;
            }

        private:
            HANDLE handle_ = nullptr;
        };

        struct QuarantinedSubmission {
            ComHandle<ID3D12CommandAllocator> allocator;
            ComHandle<ID3D12GraphicsCommandList> commandList;
            std::vector<ComHandle<IUnknown>> retainedObjects;

            void abandon() noexcept {
                allocator.detach();
                commandList.detach();
                for (auto &object : retainedObjects) {
                    object.detach();
                }
                retainedObjects.clear();
            }
        };

        struct DeviceState {
            ~DeviceState();

            ComHandle<IDXGIAdapter1> adapter;
            ComHandle<ID3D12Device> device;
            ComHandle<ID3D12CommandQueue> queue;
            ComHandle<ID3D12Fence> completionFence;
            UniqueHandle completionEvent;
            uint64_t adapterLuid = 0;
            uint32_t debugFlags = 0;
            std::mutex submitMutex;
            std::mutex completionWaitMutex;
            uint64_t nextCompletionValue = 0;
            std::atomic<uint64_t> lastSubmitted{0};
            std::vector<QuarantinedSubmission> quarantinedSubmissions;
        };

        struct AllocatorState;

        struct TextureStateCell {
            std::mutex mutex;
            ResourceState committed = ResourceState::Common;
            uint64_t revision = 0;
        };

        struct PendingTextureState {
            std::shared_ptr<TextureStateCell> cell;
            ResourceState base = ResourceState::Common;
            ResourceState current = ResourceState::Common;
            uint64_t baseRevision = 0;
        };

        enum class CommandState {
            Closed,
            Recording,
            Executable,
            Submitted,
            Poisoned,
        };

        struct ResourceView {
            std::shared_ptr<DeviceState> owner;
            ID3D12Resource *resource = nullptr;
        };

#if defined(SR_D3D12_TEST_HOOKS)
        std::atomic<HRESULT> g_nextFenceWaitFailure{S_OK};
        std::atomic<HRESULT> g_nextInternalCompletionSignalFailure{S_OK};
        std::atomic<HRESULT> g_nextQueueSharedFenceSignalFailure{S_OK};
        std::atomic<HRESULT> g_nextCpuSharedFenceSignalFailure{S_OK};
#endif

        std::string hresultText(HRESULT hr) {
            char *message = nullptr;
            const DWORD length = FormatMessageA(
                FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
                    FORMAT_MESSAGE_IGNORE_INSERTS,
                nullptr, static_cast<DWORD>(hr),
                MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                reinterpret_cast<char *>(&message), 0, nullptr);
            std::string result;
            if (length != 0 && message) {
                result.assign(message, length);
                while (!result.empty() &&
                       (result.back() == '\r' || result.back() == '\n' ||
                        result.back() == ' ' || result.back() == '.')) {
                    result.pop_back();
                }
            }
            if (message) {
                LocalFree(message);
            }
            return result;
        }

        void clearError() noexcept { g_lastError[0] = '\0'; }

        void setError(std::string_view message) noexcept {
            const size_t length = std::min(message.size(), g_lastError.size() - 1);
            if (length != 0) {
                std::memcpy(g_lastError.data(), message.data(), length);
            }
            g_lastError[length] = '\0';
        }

        void setErrorParts(const char *prefix, const char *context,
                           const char *suffix) noexcept {
            char message[512] = {};
            std::snprintf(message, sizeof(message), "%s%s%s",
                          prefix ? prefix : "", context ? context : "",
                          suffix ? suffix : "");
            setError(message);
        }

        void appendDredDiagnostics(std::ostringstream &stream,
                                   ID3D12Device *device) {
            if (!device) {
                return;
            }

            const HRESULT removedReason = device->GetDeviceRemovedReason();
            if (SUCCEEDED(removedReason)) {
                return;
            }

            stream << " DeviceRemovedReason=0x" << std::hex
                   << static_cast<uint32_t>(removedReason) << std::dec;
            const std::string removedText = hresultText(removedReason);
            if (!removedText.empty()) {
                stream << " (" << removedText << ")";
            }

#if defined(__ID3D12DeviceRemovedExtendedData1_INTERFACE_DEFINED__)
            ComHandle<ID3D12DeviceRemovedExtendedData1> dred;
            if (FAILED(device->QueryInterface(IID_PPV_ARGS(dred.put())))) {
                return;
            }

            D3D12_DRED_AUTO_BREADCRUMBS_OUTPUT1 breadcrumbs = {};
            if (SUCCEEDED(dred->GetAutoBreadcrumbsOutput1(&breadcrumbs))) {
                const D3D12_AUTO_BREADCRUMB_NODE1 *node =
                    breadcrumbs.pHeadAutoBreadcrumbNode;
                uint32_t nodeCount = 0;
                while (node && nodeCount < 8) {
                    const uint32_t last = node->pLastBreadcrumbValue
                                              ? *node->pLastBreadcrumbValue
                                              : 0;
                    stream << " Breadcrumb[" << nodeCount << "]="
                           << (node->pCommandListDebugNameA
                                   ? node->pCommandListDebugNameA
                                   : "<unnamed>")
                           << ':' << last << '/' << node->BreadcrumbCount;
                    node = node->pNext;
                    ++nodeCount;
                }
            }

            D3D12_DRED_PAGE_FAULT_OUTPUT1 pageFault = {};
            if (SUCCEEDED(dred->GetPageFaultAllocationOutput1(&pageFault)) &&
                pageFault.PageFaultVA != 0) {
                stream << " PageFaultVA=0x" << std::hex << pageFault.PageFaultVA
                       << std::dec;
                const D3D12_DRED_ALLOCATION_NODE1 *allocation =
                    pageFault.pHeadExistingAllocationNode;
                if (!allocation) {
                    allocation = pageFault.pHeadRecentFreedAllocationNode;
                }
                if (allocation) {
                    stream << " Allocation="
                           << (allocation->ObjectNameA ? allocation->ObjectNameA
                                                       : "<unnamed>");
                }
            }
#endif
        }

        void setHresultError(const char *operation, HRESULT hr,
                             ID3D12Device *device = nullptr) noexcept {
            try {
                std::ostringstream stream;
                stream << (operation ? operation : "D3D12 operation")
                       << " failed with HRESULT 0x" << std::hex
                       << static_cast<uint32_t>(hr) << std::dec;
                const std::string text = hresultText(hr);
                if (!text.empty()) {
                    stream << " (" << text << ')';
                }
                stream << '.';
                appendDredDiagnostics(stream, device);
                setError(stream.str());
            } catch (...) {
                setError("D3D12 operation failed and diagnostics could not be built.");
            }
        }

        HRESULT invalidArgument(const char *message) noexcept {
            setError(message ? message : "Invalid D3D12 runtime argument.");
            return E_INVALIDARG;
        }

        uint64_t packLuid(const LUID &luid) noexcept {
            return static_cast<uint64_t>(luid.LowPart) |
                   (static_cast<uint64_t>(static_cast<uint32_t>(luid.HighPart))
                    << 32);
        }

        LUID unpackLuid(uint64_t value) noexcept {
            LUID luid = {};
            luid.LowPart = static_cast<DWORD>(value & 0xFFFFFFFFULL);
            luid.HighPart = static_cast<LONG>(
                static_cast<uint32_t>((value >> 32) & 0xFFFFFFFFULL));
            return luid;
        }

        bool mapResourceState(ResourceState state,
                              D3D12_RESOURCE_STATES &nativeState) noexcept {
            switch (state) {
                case ResourceState::Common:
                    nativeState = D3D12_RESOURCE_STATE_COMMON;
                    return true;
                case ResourceState::ComputeRead:
                    nativeState = D3D12_RESOURCE_STATE_NON_PIXEL_SHADER_RESOURCE;
                    return true;
                case ResourceState::UnorderedAccess:
                    nativeState = D3D12_RESOURCE_STATE_UNORDERED_ACCESS;
                    return true;
                case ResourceState::CopySource:
                    nativeState = D3D12_RESOURCE_STATE_COPY_SOURCE;
                    return true;
                case ResourceState::CopyDestination:
                    nativeState = D3D12_RESOURCE_STATE_COPY_DEST;
                    return true;
                case ResourceState::RenderTarget:
                    nativeState = D3D12_RESOURCE_STATE_RENDER_TARGET;
                    return true;
                case ResourceState::DepthWrite:
                    nativeState = D3D12_RESOURCE_STATE_DEPTH_WRITE;
                    return true;
                case ResourceState::Present:
                    nativeState = D3D12_RESOURCE_STATE_PRESENT;
                    return true;
                default:
                    return false;
            }
        }

        DXGI_FORMAT mapSurfaceFormat(int32_t format) noexcept {
            switch (format) {
                case SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS:
                    return DXGI_FORMAT_R32G32B32A32_TYPELESS;
                case SR_TEXTURE_FORMAT_R32G32B32A32_UINT:
                    return DXGI_FORMAT_R32G32B32A32_UINT;
                case SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT:
                    return DXGI_FORMAT_R32G32B32A32_FLOAT;
                case SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT:
                    return DXGI_FORMAT_R16G16B16A16_FLOAT;
                case SR_TEXTURE_FORMAT_R32G32B32_FLOAT:
                    return DXGI_FORMAT_R32G32B32_FLOAT;
                case SR_TEXTURE_FORMAT_R32G32_FLOAT:
                    return DXGI_FORMAT_R32G32_FLOAT;
                case SR_TEXTURE_FORMAT_R8_UINT:
                    return DXGI_FORMAT_R8_UINT;
                case SR_TEXTURE_FORMAT_R32_UINT:
                    return DXGI_FORMAT_R32_UINT;
                case SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS:
                    return DXGI_FORMAT_R8G8B8A8_TYPELESS;
                case SR_TEXTURE_FORMAT_R8G8B8A8_UNORM:
                    return DXGI_FORMAT_R8G8B8A8_UNORM;
                case SR_TEXTURE_FORMAT_R8G8B8A8_SNORM:
                    return DXGI_FORMAT_R8G8B8A8_SNORM;
                case SR_TEXTURE_FORMAT_R8G8B8A8_SRGB:
                    return DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
                case SR_TEXTURE_FORMAT_B8G8R8A8_TYPELESS:
                    return DXGI_FORMAT_B8G8R8A8_TYPELESS;
                case SR_TEXTURE_FORMAT_B8G8R8A8_UNORM:
                    return DXGI_FORMAT_B8G8R8A8_UNORM;
                case SR_TEXTURE_FORMAT_B8G8R8A8_SRGB:
                    return DXGI_FORMAT_B8G8R8A8_UNORM_SRGB;
                case SR_TEXTURE_FORMAT_R11G11B10_FLOAT:
                    return DXGI_FORMAT_R11G11B10_FLOAT;
                case SR_TEXTURE_FORMAT_R10G10B10A2_UNORM:
                    return DXGI_FORMAT_R10G10B10A2_UNORM;
                case SR_TEXTURE_FORMAT_R16G16_FLOAT:
                    return DXGI_FORMAT_R16G16_FLOAT;
                case SR_TEXTURE_FORMAT_R16G16_UINT:
                    return DXGI_FORMAT_R16G16_UINT;
                case SR_TEXTURE_FORMAT_R16G16_SINT:
                    return DXGI_FORMAT_R16G16_SINT;
                case SR_TEXTURE_FORMAT_R16_FLOAT:
                    return DXGI_FORMAT_R16_FLOAT;
                case SR_TEXTURE_FORMAT_R16_UINT:
                    return DXGI_FORMAT_R16_UINT;
                case SR_TEXTURE_FORMAT_R16_UNORM:
                    return DXGI_FORMAT_R16_UNORM;
                case SR_TEXTURE_FORMAT_R16_SNORM:
                    return DXGI_FORMAT_R16_SNORM;
                case SR_TEXTURE_FORMAT_R8_UNORM:
                    return DXGI_FORMAT_R8_UNORM;
                case SR_TEXTURE_FORMAT_R8G8_UNORM:
                    return DXGI_FORMAT_R8G8_UNORM;
                case SR_TEXTURE_FORMAT_R8G8_UINT:
                    return DXGI_FORMAT_R8G8_UINT;
                case SR_TEXTURE_FORMAT_R32_FLOAT:
                    return DXGI_FORMAT_R32_FLOAT;
                case SR_TEXTURE_FORMAT_R9G9B9E5_SHAREDEXP:
                    return DXGI_FORMAT_R9G9B9E5_SHAREDEXP;
                case SR_TEXTURE_FORMAT_R16G16B16A16_TYPELESS:
                    return DXGI_FORMAT_R16G16B16A16_TYPELESS;
                case SR_TEXTURE_FORMAT_R32G32_TYPELESS:
                    return DXGI_FORMAT_R32G32_TYPELESS;
                case SR_TEXTURE_FORMAT_R10G10B10A2_TYPELESS:
                    return DXGI_FORMAT_R10G10B10A2_TYPELESS;
                case SR_TEXTURE_FORMAT_R16G16_TYPELESS:
                    return DXGI_FORMAT_R16G16_TYPELESS;
                case SR_TEXTURE_FORMAT_R16_TYPELESS:
                    return DXGI_FORMAT_R16_TYPELESS;
                case SR_TEXTURE_FORMAT_R8_TYPELESS:
                    return DXGI_FORMAT_R8_TYPELESS;
                case SR_TEXTURE_FORMAT_R8G8_TYPELESS:
                    return DXGI_FORMAT_R8G8_TYPELESS;
                case SR_TEXTURE_FORMAT_R32_TYPELESS:
                    return DXGI_FORMAT_R32_TYPELESS;
                case SR_TEXTURE_FORMAT_D32_SFLOAT:
                    return DXGI_FORMAT_D32_FLOAT;
                case SR_TEXTURE_FORMAT_R16G16B16A16_SNORM:
                    return DXGI_FORMAT_R16G16B16A16_SNORM;
                default:
                    return DXGI_FORMAT_UNKNOWN;
            }
        }

        uint32_t bytesPerPixel(int32_t format) noexcept {
            switch (format) {
                case SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS:
                case SR_TEXTURE_FORMAT_R32G32B32A32_UINT:
                case SR_TEXTURE_FORMAT_R32G32B32A32_FLOAT:
                    return 16;
                case SR_TEXTURE_FORMAT_R32G32B32_FLOAT:
                    return 12;
                case SR_TEXTURE_FORMAT_R16G16B16A16_FLOAT:
                case SR_TEXTURE_FORMAT_R32G32_FLOAT:
                case SR_TEXTURE_FORMAT_R16G16B16A16_TYPELESS:
                case SR_TEXTURE_FORMAT_R32G32_TYPELESS:
                case SR_TEXTURE_FORMAT_R16G16B16A16_SNORM:
                    return 8;
                case SR_TEXTURE_FORMAT_R8_UINT:
                case SR_TEXTURE_FORMAT_R8_UNORM:
                case SR_TEXTURE_FORMAT_R8_TYPELESS:
                    return 1;
                case SR_TEXTURE_FORMAT_R16_FLOAT:
                case SR_TEXTURE_FORMAT_R16_UINT:
                case SR_TEXTURE_FORMAT_R16_UNORM:
                case SR_TEXTURE_FORMAT_R16_SNORM:
                case SR_TEXTURE_FORMAT_R16_TYPELESS:
                case SR_TEXTURE_FORMAT_R8G8_UNORM:
                case SR_TEXTURE_FORMAT_R8G8_UINT:
                case SR_TEXTURE_FORMAT_R8G8_TYPELESS:
                    return 2;
                default:
                    return mapSurfaceFormat(format) == DXGI_FORMAT_UNKNOWN ? 0 : 4;
            }
        }

        DXGI_FORMAT renderTargetViewFormat(int32_t format) noexcept {
            switch (format) {
                case SR_TEXTURE_FORMAT_R32G32B32A32_TYPELESS:
                    return DXGI_FORMAT_R32G32B32A32_FLOAT;
                case SR_TEXTURE_FORMAT_R8G8B8A8_TYPELESS:
                    return DXGI_FORMAT_R8G8B8A8_UNORM;
                case SR_TEXTURE_FORMAT_B8G8R8A8_TYPELESS:
                    return DXGI_FORMAT_B8G8R8A8_UNORM;
                case SR_TEXTURE_FORMAT_R16G16B16A16_TYPELESS:
                    return DXGI_FORMAT_R16G16B16A16_FLOAT;
                case SR_TEXTURE_FORMAT_R32G32_TYPELESS:
                    return DXGI_FORMAT_R32G32_FLOAT;
                case SR_TEXTURE_FORMAT_R10G10B10A2_TYPELESS:
                    return DXGI_FORMAT_R10G10B10A2_UNORM;
                case SR_TEXTURE_FORMAT_R16G16_TYPELESS:
                    return DXGI_FORMAT_R16G16_FLOAT;
                case SR_TEXTURE_FORMAT_R16_TYPELESS:
                    return DXGI_FORMAT_R16_FLOAT;
                case SR_TEXTURE_FORMAT_R8_TYPELESS:
                    return DXGI_FORMAT_R8_UNORM;
                case SR_TEXTURE_FORMAT_R8G8_TYPELESS:
                    return DXGI_FORMAT_R8G8_UNORM;
                case SR_TEXTURE_FORMAT_R32_TYPELESS:
                    return DXGI_FORMAT_R32_FLOAT;
                case SR_TEXTURE_FORMAT_D32_SFLOAT:
                    return DXGI_FORMAT_UNKNOWN;
                default:
                    return mapSurfaceFormat(format);
            }
        }

        HRESULT configureDebug(uint32_t flags) noexcept {
            if ((flags & ~VALID_DEBUG_FLAGS) != 0) {
                return invalidArgument("Unknown D3D12 debug flag bits.");
            }

            if ((flags & (DEBUG_LAYER | DEBUG_GPU_VALIDATION)) != 0) {
                ComHandle<ID3D12Debug> debug;
                HRESULT hr = D3D12GetDebugInterface(IID_PPV_ARGS(debug.put()));
                if (FAILED(hr)) {
                    setHresultError("D3D12GetDebugInterface(ID3D12Debug)", hr);
                    return hr;
                }
                debug->EnableDebugLayer();

                if ((flags & DEBUG_GPU_VALIDATION) != 0) {
                    ComHandle<ID3D12Debug1> debug1;
                    hr = debug->QueryInterface(IID_PPV_ARGS(debug1.put()));
                    if (FAILED(hr)) {
                        setHresultError("ID3D12Debug::QueryInterface(ID3D12Debug1)",
                                        hr);
                        return hr;
                    }
                    debug1->SetEnableGPUBasedValidation(TRUE);
                }
            }

            if ((flags & DEBUG_DRED) != 0) {
#if defined(__ID3D12DeviceRemovedExtendedDataSettings_INTERFACE_DEFINED__)
                ComHandle<ID3D12DeviceRemovedExtendedDataSettings> settings;
                const HRESULT hr =
                    D3D12GetDebugInterface(IID_PPV_ARGS(settings.put()));
                if (FAILED(hr)) {
                    setHresultError(
                        "D3D12GetDebugInterface(DRED settings)", hr);
                    return hr;
                }
                settings->SetAutoBreadcrumbsEnablement(
                    D3D12_DRED_ENABLEMENT_FORCED_ON);
                settings->SetPageFaultEnablement(D3D12_DRED_ENABLEMENT_FORCED_ON);
#else
                return invalidArgument(
                    "This Windows SDK does not expose DRED settings.");
#endif
            }
            return S_OK;
        }

        HRESULT waitForFence(ID3D12Fence *fence, HANDLE eventHandle,
                             std::mutex &eventMutex, uint64_t value,
                             uint32_t timeoutMilliseconds,
                             ID3D12Device *device) noexcept {
            if (!fence || !eventHandle) {
                return invalidArgument("The D3D12 fence or wait event is null.");
            }
            if (value == 0) {
                return S_OK;
            }

#if defined(SR_D3D12_TEST_HOOKS)
            const HRESULT injected =
                g_nextFenceWaitFailure.exchange(S_OK, std::memory_order_acq_rel);
            if (FAILED(injected)) {
                setHresultError("Injected D3D12 fence wait", injected, device);
                return injected;
            }
#endif

            const uint64_t completed = fence->GetCompletedValue();
            if (completed == std::numeric_limits<uint64_t>::max()) {
                const HRESULT removed = device ? device->GetDeviceRemovedReason()
                                               : DXGI_ERROR_DEVICE_REMOVED;
                setHresultError("ID3D12Fence::GetCompletedValue", removed, device);
                return removed;
            }
            if (completed >= value) {
                return S_OK;
            }

            std::lock_guard<std::mutex> lock(eventMutex);
            HRESULT hr = fence->SetEventOnCompletion(value, eventHandle);
            if (FAILED(hr)) {
                setHresultError("ID3D12Fence::SetEventOnCompletion", hr, device);
                return hr;
            }

            const DWORD waitResult = WaitForSingleObject(eventHandle,
                                                         timeoutMilliseconds);
            if (waitResult == WAIT_OBJECT_0) {
                return S_OK;
            }
            if (waitResult == WAIT_TIMEOUT) {
                hr = HRESULT_FROM_WIN32(ERROR_TIMEOUT);
                setHresultError("WaitForSingleObject(D3D12 fence)", hr, device);
                return hr;
            }

            hr = HRESULT_FROM_WIN32(GetLastError());
            setHresultError("WaitForSingleObject(D3D12 fence)", hr, device);
            return hr;
        }

        HRESULT waitForInternal(const std::shared_ptr<DeviceState> &state,
                                 uint64_t value,
                                 uint32_t timeoutMilliseconds) noexcept {
            if (!state || !state->device || !state->completionFence ||
                !state->completionEvent) {
                return invalidArgument("The D3D12 device state is incomplete.");
            }
            return waitForFence(state->completionFence.get(),
                                state->completionEvent.get(),
                                state->completionWaitMutex, value,
                                timeoutMilliseconds, state->device.get());
        }

        HRESULT signalSubmissionCompletion(DeviceState *state,
                                           uint64_t value) noexcept {
#if defined(SR_D3D12_TEST_HOOKS)
            const HRESULT injected =
                g_nextInternalCompletionSignalFailure.exchange(
                    S_OK, std::memory_order_acq_rel);
            if (FAILED(injected)) {
                return injected;
            }
#endif
            return state->queue->Signal(state->completionFence.get(), value);
        }

        HRESULT signalSharedFenceOnQueue(DeviceState *state,
                                         ID3D12Fence *fence,
                                         uint64_t value) noexcept {
#if defined(SR_D3D12_TEST_HOOKS)
            const HRESULT injected =
                g_nextQueueSharedFenceSignalFailure.exchange(
                    S_OK, std::memory_order_acq_rel);
            if (FAILED(injected)) {
                return injected;
            }
#endif
            return state->queue->Signal(fence, value);
        }

        HRESULT signalCpuSharedFence(ID3D12Fence *fence,
                                     uint64_t value) noexcept {
#if defined(SR_D3D12_TEST_HOOKS)
            const HRESULT injected =
                g_nextCpuSharedFenceSignalFailure.exchange(
                    S_OK, std::memory_order_acq_rel);
            if (FAILED(injected)) {
                return injected;
            }
#endif
            return fence->Signal(value);
        }

        HRESULT proveQueuedWorkCompletedLocked(
            const std::shared_ptr<DeviceState> &state,
            const char *signalOperation) noexcept {
            if (state->quarantinedSubmissions.empty()) {
                const uint64_t value =
                    state->lastSubmitted.load(std::memory_order_acquire);
                return value == 0
                           ? S_OK
                           : waitForInternal(state, value, WAIT_INFINITE);
            }
            if (state->nextCompletionValue >=
                static_cast<uint64_t>(std::numeric_limits<int64_t>::max())) {
                setError(
                    "The internal D3D12 completion fence value space is exhausted while proving queued work completion.");
                return E_FAIL;
            }

            const uint64_t value = ++state->nextCompletionValue;
            HRESULT hr = signalSubmissionCompletion(state.get(), value);
            if (FAILED(hr)) {
                setHresultError(signalOperation, hr, state->device.get());
                return hr;
            }
            state->lastSubmitted.store(value, std::memory_order_release);
            hr = waitForInternal(state, value, WAIT_INFINITE);
            if (SUCCEEDED(hr)) {
                state->quarantinedSubmissions.clear();
            }
            return hr;
        }

        void abandonQuarantinedSubmissions(DeviceState *state) noexcept {
            for (auto &submission : state->quarantinedSubmissions) {
                submission.abandon();
            }
            state->quarantinedSubmissions.clear();

            // No fence can prove that the queued work has stopped. Retain the
            // queue and device for the rest of the process rather than release
            // objects that may still be referenced by the GPU.
            state->completionFence.detach();
            state->queue.detach();
            state->device.detach();
            state->adapter.detach();
        }

        DeviceState::~DeviceState() {
            if (!queue || !completionFence || !completionEvent || !device) {
                return;
            }
            std::lock_guard<std::mutex> submitLock(submitMutex);
            bool drained = false;
            if (nextCompletionValue <
                static_cast<uint64_t>(std::numeric_limits<int64_t>::max())) {
                const uint64_t value = ++nextCompletionValue;
                if (SUCCEEDED(queue->Signal(completionFence.get(), value))) {
                    lastSubmitted.store(value, std::memory_order_release);
                    drained = SUCCEEDED(waitForFence(
                        completionFence.get(), completionEvent.get(),
                        completionWaitMutex, value, WAIT_INFINITE, device.get()));
                }
            }
            if (drained) {
                quarantinedSubmissions.clear();
            } else if (!quarantinedSubmissions.empty()) {
                abandonQuarantinedSubmissions(this);
            }
        }

        template<typename T, typename Create>
        HRESULT createCom(const char *operation, ComHandle<T> &result,
                          ID3D12Device *diagnosticDevice,
                          Create &&create) noexcept {
            const HRESULT hr = create(result.put());
            if (FAILED(hr)) {
                setHresultError(operation, hr, diagnosticDevice);
            }
            return hr;
        }

        HRESULT createSharedHandle(ID3D12Device *device, ID3D12DeviceChild *object,
                                   UniqueHandle &handle) noexcept {
            const HRESULT hr = device->CreateSharedHandle(
                object, nullptr, GENERIC_ALL, nullptr, handle.put());
            if (FAILED(hr)) {
                setHresultError("ID3D12Device::CreateSharedHandle", hr, device);
            }
            return hr;
        }

        template<typename T>
        T *requireObject(T *object, ObjectKind expected,
                         const char *name) noexcept {
            if (!object || object->header.magic != OBJECT_MAGIC ||
                object->header.kind != expected) {
                setErrorParts("Invalid ", name ? name : "D3D12 object", " handle.");
                return nullptr;
            }
            return object;
        }

        HRESULT retainObject(std::vector<ComHandle<IUnknown>> &retained,
                             IUnknown *object) noexcept {
            if (!object) {
                return invalidArgument("Cannot retain a null D3D12 object.");
            }
            ComHandle<IUnknown> hold = ComHandle<IUnknown>::retain(object);
            try {
                retained.push_back(std::move(hold));
            } catch (const std::bad_alloc &) {
                setError("Could not retain a D3D12 command resource.");
                return E_OUTOFMEMORY;
            }
            return S_OK;
        }

        uint32_t mipExtent(uint32_t base, uint32_t mip) noexcept {
            return std::max(1u, base >> mip);
        }

        bool rangeFits(uint64_t offset, uint64_t size, uint64_t capacity) noexcept {
            return offset <= capacity && size <= capacity - offset;
        }

        bool rectangleFits(uint32_t x, uint32_t y, uint32_t width,
                           uint32_t height, uint32_t extentWidth,
                           uint32_t extentHeight) noexcept {
            return width != 0 && height != 0 && x <= extentWidth &&
                   y <= extentHeight && width <= extentWidth - x &&
                   height <= extentHeight - y;
        }

        uint64_t alignUp(uint64_t value, uint64_t alignment) noexcept {
            if (alignment == 0 || value >
                                      std::numeric_limits<uint64_t>::max() -
                                          (alignment - 1)) {
                return 0;
            }
            return (value + alignment - 1) & ~(alignment - 1);
        }
    } // namespace

    struct Device {
        ObjectHeader header{ObjectKind::Device};
        std::shared_ptr<DeviceState> state;
    };

    struct SharedFence {
        ObjectHeader header{ObjectKind::SharedFence};
        std::shared_ptr<DeviceState> owner;
        ComHandle<ID3D12Fence> fence;
        UniqueHandle sharedHandle;
        UniqueHandle waitEvent;
        std::mutex waitMutex;
        std::mutex valueMutex;
        uint64_t reservedValue = 0;
        uint64_t lastNativeSignal = 0;
    };

    struct Texture2D {
        ObjectHeader header{ObjectKind::Texture2D};
        std::shared_ptr<DeviceState> owner;
        ComHandle<ID3D12Resource> resource;
        UniqueHandle sharedHandle;
        ComHandle<ID3D12DescriptorHeap> renderTargetHeap;
        D3D12_CPU_DESCRIPTOR_HANDLE renderTargetView = {};
        uint64_t allocationSize = 0;
        uint32_t width = 0;
        uint32_t height = 0;
        uint16_t mipLevels = 0;
        int32_t surfaceFormat = SR_TEXTURE_FORMAT_UNKNOWN;
        uint32_t resourceFlags = 0;
        ResourceState initialState = ResourceState::Common;
        std::shared_ptr<TextureStateCell> stateCell;
    };

    struct Buffer {
        ObjectHeader header{ObjectKind::Buffer};
        std::shared_ptr<DeviceState> owner;
        ComHandle<ID3D12Resource> resource;
        UniqueHandle sharedHandle;
        std::mutex mapMutex;
        uint64_t size = 0;
        uint64_t mappedOffset = 0;
        uint64_t mappedSize = 0;
        BufferHeap heap = BufferHeap::Default;
        uint32_t resourceFlags = 0;
        ResourceState initialState = ResourceState::Common;
        bool mapped = false;
    };

    struct AllocatorState {
        std::shared_ptr<DeviceState> owner;
        ComHandle<ID3D12CommandAllocator> allocator;
        std::mutex mutex;
        CommandList *recordedOwner = nullptr;
        uint64_t lastCompletionValue = 0;
        bool poisoned = false;
    };

    struct CommandAllocator {
        ObjectHeader header{ObjectKind::CommandAllocator};
        std::shared_ptr<AllocatorState> state;
    };

    struct CommandList {
        ObjectHeader header{ObjectKind::CommandList};
        std::shared_ptr<DeviceState> owner;
        std::shared_ptr<AllocatorState> allocator;
        ComHandle<ID3D12GraphicsCommandList> commandList;
        std::mutex mutex;
        CommandState state = CommandState::Closed;
        uint64_t completionValue = 0;
        std::vector<ComHandle<IUnknown>> retainedObjects;
        std::unordered_map<TextureStateCell *, PendingTextureState>
            pendingTextureStates;
    };

    const char *lastError() noexcept { return g_lastError.data(); }

    void setLastError(const char *message) noexcept {
        clearError();
        setError(message ? message : "D3D12 runtime error.");
    }

    HRESULT setObjectName(void *object, const wchar_t *name) noexcept {
        clearError();
        if (!object || !name) {
            return invalidArgument("Invalid D3D12 debug-name arguments.");
        }

        auto *header = static_cast<ObjectHeader *>(object);
        if (header->magic != OBJECT_MAGIC) {
            return invalidArgument("Invalid D3D12 object handle for debug name.");
        }

        try {
            const std::wstring base(name);
            auto apply = [&](ID3D12Object *nativeObject,
                             const std::wstring &nativeName,
                             ID3D12Device *diagnosticDevice) -> HRESULT {
                if (!nativeObject) {
                    return E_INVALIDARG;
                }
                const HRESULT hr = nativeObject->SetName(nativeName.c_str());
                if (FAILED(hr)) {
                    setHresultError("ID3D12Object::SetName", hr,
                                    diagnosticDevice);
                }
                return hr;
            };

            switch (header->kind) {
                case ObjectKind::Device: {
                    auto *device = static_cast<Device *>(object);
                    HRESULT hr = apply(device->state->device.get(), base,
                                       device->state->device.get());
                    if (FAILED(hr)) {
                        return hr;
                    }
                    hr = apply(device->state->queue.get(), base + L" Direct Queue",
                               device->state->device.get());
                    if (FAILED(hr)) {
                        return hr;
                    }
                    return apply(device->state->completionFence.get(),
                                 base + L" Internal Completion Fence",
                                 device->state->device.get());
                }
                case ObjectKind::SharedFence: {
                    auto *fence = static_cast<SharedFence *>(object);
                    return apply(fence->fence.get(), base,
                                 fence->owner->device.get());
                }
                case ObjectKind::Texture2D: {
                    auto *texture = static_cast<Texture2D *>(object);
                    HRESULT hr = apply(texture->resource.get(), base,
                                       texture->owner->device.get());
                    if (FAILED(hr) || !texture->renderTargetHeap) {
                        return hr;
                    }
                    return apply(texture->renderTargetHeap.get(),
                                 base + L" RTV Heap",
                                 texture->owner->device.get());
                }
                case ObjectKind::Buffer: {
                    auto *buffer = static_cast<Buffer *>(object);
                    return apply(buffer->resource.get(), base,
                                 buffer->owner->device.get());
                }
                case ObjectKind::CommandAllocator: {
                    auto *allocator = static_cast<CommandAllocator *>(object);
                    return apply(allocator->state->allocator.get(), base,
                                 allocator->state->owner->device.get());
                }
                case ObjectKind::CommandList: {
                    auto *commandList = static_cast<CommandList *>(object);
                    return apply(commandList->commandList.get(), base,
                                 commandList->owner->device.get());
                }
                default:
                    return invalidArgument(
                        "Unsupported D3D12 object type for debug name.");
            }
        } catch (const std::bad_alloc &) {
            setError("Could not allocate the D3D12 debug name.");
            return E_OUTOFMEMORY;
        } catch (...) {
            setError("Unexpected exception while setting a D3D12 debug name.");
            return E_FAIL;
        }
    }

    Device *createDevice(uint64_t adapterLuid, uint32_t debugFlags) noexcept {
        clearError();
        if (adapterLuid == 0) {
            invalidArgument("The D3D12 adapter LUID must be nonzero.");
            return nullptr;
        }
        if (FAILED(configureDebug(debugFlags))) {
            return nullptr;
        }

        try {
            ComHandle<IDXGIFactory6> factory;
            HRESULT hr = createCom<IDXGIFactory6>(
                "CreateDXGIFactory1", factory, nullptr,
                [](IDXGIFactory6 **result) {
                    return CreateDXGIFactory1(IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            ComHandle<IDXGIAdapter1> adapter;
            const LUID luid = unpackLuid(adapterLuid);
            hr = factory->EnumAdapterByLuid(luid, IID_PPV_ARGS(adapter.put()));
            if (FAILED(hr)) {
                setHresultError("IDXGIFactory4::EnumAdapterByLuid", hr);
                return nullptr;
            }

            DXGI_ADAPTER_DESC1 adapterDesc = {};
            hr = adapter->GetDesc1(&adapterDesc);
            if (FAILED(hr)) {
                setHresultError("IDXGIAdapter1::GetDesc1", hr);
                return nullptr;
            }
            if ((adapterDesc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) != 0) {
                invalidArgument("The requested D3D12 adapter is a software adapter.");
                return nullptr;
            }

            ComHandle<ID3D12Device> nativeDevice;
            hr = createCom<ID3D12Device>(
                "D3D12CreateDevice(FL12_0)", nativeDevice, nullptr,
                [&](ID3D12Device **result) {
                    return D3D12CreateDevice(adapter.get(), D3D_FEATURE_LEVEL_12_0,
                                             IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            D3D12_COMMAND_QUEUE_DESC queueDescription = {};
            queueDescription.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
            queueDescription.Priority = D3D12_COMMAND_QUEUE_PRIORITY_NORMAL;
            queueDescription.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
            queueDescription.NodeMask = 0;

            ComHandle<ID3D12CommandQueue> queue;
            hr = createCom<ID3D12CommandQueue>(
                "ID3D12Device::CreateCommandQueue", queue, nativeDevice.get(),
                [&](ID3D12CommandQueue **result) {
                    return nativeDevice->CreateCommandQueue(&queueDescription,
                                                            IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            ComHandle<ID3D12Fence> completionFence;
            hr = createCom<ID3D12Fence>(
                "ID3D12Device::CreateFence(internal)", completionFence,
                nativeDevice.get(), [&](ID3D12Fence **result) {
                    return nativeDevice->CreateFence(0, D3D12_FENCE_FLAG_NONE,
                                                     IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            UniqueHandle completionEvent(
                CreateEventW(nullptr, FALSE, FALSE, nullptr));
            if (!completionEvent) {
                hr = HRESULT_FROM_WIN32(GetLastError());
                setHresultError("CreateEventW(internal completion)", hr,
                                nativeDevice.get());
                return nullptr;
            }

            auto state = std::make_shared<DeviceState>();
            state->adapter = std::move(adapter);
            state->device = std::move(nativeDevice);
            state->queue = std::move(queue);
            state->completionFence = std::move(completionFence);
            state->completionEvent = std::move(completionEvent);
            state->adapterLuid = packLuid(adapterDesc.AdapterLuid);
            state->debugFlags = debugFlags;
            state->device->SetName(L"SR D3D12 Device");
            state->queue->SetName(L"SR D3D12 Direct Queue");
            state->completionFence->SetName(
                L"SR D3D12 Internal Completion Fence");

            auto *device = new Device();
            device->state = std::move(state);
            return device;
        } catch (const std::bad_alloc &) {
            setError("Could not allocate the D3D12 device runtime.");
            return nullptr;
        } catch (...) {
            setError("Unexpected exception while creating the D3D12 device.");
            return nullptr;
        }
    }

    void destroyDevice(Device *device) noexcept {
        if (!device) {
            return;
        }
        clearError();
        if (!requireObject(device, ObjectKind::Device, "D3D12 device")) {
            return;
        }
        device->header.magic = 0;
        delete device;
    }

    ID3D12Device *nativeDevice(Device *device) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        return device && device->state ? device->state->device.get() : nullptr;
    }

    ID3D12CommandQueue *nativeQueue(Device *device) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        return device && device->state ? device->state->queue.get() : nullptr;
    }

    uint64_t deviceAdapterLuid(Device *device) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        return device && device->state ? device->state->adapterLuid : 0;
    }

    uint64_t completedSubmissionValue(Device *device) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        if (!device || !device->state || !device->state->completionFence) {
            return 0;
        }
        const uint64_t completed =
            device->state->completionFence->GetCompletedValue();
        if (completed == std::numeric_limits<uint64_t>::max()) {
            const HRESULT removed = device->state->device->GetDeviceRemovedReason();
            setHresultError("ID3D12Fence::GetCompletedValue", removed,
                            device->state->device.get());
            return 0;
        }
        return completed;
    }

    uint64_t lastSubmittedValue(Device *device) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        return device && device->state
                   ? device->state->lastSubmitted.load(std::memory_order_acquire)
                   : 0;
    }

    HRESULT waitIdle(Device *device, uint32_t timeoutMilliseconds) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        if (!device || !device->state) {
            return E_INVALIDARG;
        }

        const std::shared_ptr<DeviceState> state = device->state;
        std::unique_lock<std::mutex> submitLock(state->submitMutex);
        if (state->quarantinedSubmissions.empty()) {
            const uint64_t value =
                state->lastSubmitted.load(std::memory_order_acquire);
            submitLock.unlock();
            return waitForInternal(state, value, timeoutMilliseconds);
        }
        if (state->nextCompletionValue >=
            static_cast<uint64_t>(std::numeric_limits<int64_t>::max())) {
            setError(
                "The internal D3D12 completion fence value space is exhausted while draining an untracked submission.");
            return E_FAIL;
        }

        const uint64_t value = ++state->nextCompletionValue;
        HRESULT hr = state->queue->Signal(state->completionFence.get(), value);
        if (FAILED(hr)) {
            setHresultError("ID3D12CommandQueue::Signal(quarantine drain)", hr,
                            state->device.get());
            return hr;
        }
        state->lastSubmitted.store(value, std::memory_order_release);
        hr = waitForInternal(state, value, timeoutMilliseconds);
        if (SUCCEEDED(hr)) {
            state->quarantinedSubmissions.clear();
        }
        return hr;
    }

#if defined(SR_D3D12_TEST_HOOKS)
    namespace testing {
        void failNextFenceWait(HRESULT failure) noexcept {
            g_nextFenceWaitFailure.store(
                FAILED(failure) ? failure : E_FAIL, std::memory_order_release);
        }

        void failNextInternalCompletionSignal(HRESULT failure) noexcept {
            g_nextInternalCompletionSignalFailure.store(
                FAILED(failure) ? failure : E_FAIL, std::memory_order_release);
        }

        void failNextQueueSharedFenceSignal(HRESULT failure) noexcept {
            g_nextQueueSharedFenceSignalFailure.store(
                FAILED(failure) ? failure : E_FAIL, std::memory_order_release);
        }

        void failNextCpuSharedFenceSignal(HRESULT failure) noexcept {
            g_nextCpuSharedFenceSignalFailure.store(
                FAILED(failure) ? failure : E_FAIL, std::memory_order_release);
        }

        size_t quarantinedSubmissionCount(Device *device) noexcept {
            device = requireObject(device, ObjectKind::Device, "D3D12 device");
            if (!device || !device->state) {
                return 0;
            }
            std::lock_guard<std::mutex> lock(device->state->submitMutex);
            return device->state->quarantinedSubmissions.size();
        }
    } // namespace testing
#endif

    SharedFence *createSharedFence(Device *device,
                                   uint64_t initialValue) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        if (!device || !device->state) {
            return nullptr;
        }

        try {
            ComHandle<ID3D12Fence> fence;
            HRESULT hr = createCom<ID3D12Fence>(
                "ID3D12Device::CreateFence(shared)", fence,
                device->state->device.get(), [&](ID3D12Fence **result) {
                    return device->state->device->CreateFence(
                        initialValue, D3D12_FENCE_FLAG_SHARED,
                        IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            UniqueHandle handle;
            hr = createSharedHandle(device->state->device.get(), fence.get(), handle);
            if (FAILED(hr)) {
                return nullptr;
            }

            UniqueHandle event(CreateEventW(nullptr, FALSE, FALSE, nullptr));
            if (!event) {
                hr = HRESULT_FROM_WIN32(GetLastError());
                setHresultError("CreateEventW(shared fence)", hr,
                                device->state->device.get());
                return nullptr;
            }

            auto *result = new SharedFence();
            result->owner = device->state;
            result->fence = std::move(fence);
            result->sharedHandle = std::move(handle);
            result->waitEvent = std::move(event);
            result->reservedValue = initialValue;
            result->lastNativeSignal = initialValue;
            result->fence->SetName(L"SR D3D12 Shared Fence");
            return result;
        } catch (const std::bad_alloc &) {
            setError("Could not allocate the D3D12 shared fence.");
            return nullptr;
        } catch (...) {
            setError("Unexpected exception while creating the D3D12 shared fence.");
            return nullptr;
        }
    }

    void destroySharedFence(SharedFence *fence) noexcept {
        if (!fence) {
            return;
        }
        clearError();
        if (!requireObject(fence, ObjectKind::SharedFence,
                           "D3D12 shared fence")) {
            return;
        }
        fence->header.magic = 0;
        delete fence;
    }

    ID3D12Fence *nativeFence(SharedFence *fence) noexcept {
        clearError();
        fence = requireObject(fence, ObjectKind::SharedFence,
                              "D3D12 shared fence");
        return fence ? fence->fence.get() : nullptr;
    }

    HANDLE sharedFenceHandle(SharedFence *fence) noexcept {
        clearError();
        fence = requireObject(fence, ObjectKind::SharedFence,
                              "D3D12 shared fence");
        return fence ? fence->sharedHandle.get() : nullptr;
    }

    uint64_t reserveSharedFenceValue(SharedFence *fence) noexcept {
        clearError();
        fence = requireObject(fence, ObjectKind::SharedFence,
                              "D3D12 shared fence");
        if (!fence) {
            return 0;
        }
        std::lock_guard<std::mutex> lock(fence->valueMutex);
        if (fence->reservedValue >=
            static_cast<uint64_t>(std::numeric_limits<int64_t>::max())) {
            setError("The D3D12 shared fence value space is exhausted.");
            return 0;
        }
        return ++fence->reservedValue;
    }

    uint64_t completedSharedFenceValue(SharedFence *fence) noexcept {
        clearError();
        fence = requireObject(fence, ObjectKind::SharedFence,
                              "D3D12 shared fence");
        if (!fence) {
            return 0;
        }
        const uint64_t completed = fence->fence->GetCompletedValue();
        if (completed == std::numeric_limits<uint64_t>::max()) {
            const HRESULT removed = fence->owner->device->GetDeviceRemovedReason();
            setHresultError("ID3D12Fence::GetCompletedValue", removed,
                            fence->owner->device.get());
            return 0;
        }
        return completed;
    }

    HRESULT signalSharedFence(SharedFence *fence, uint64_t value) noexcept {
        clearError();
        fence = requireObject(fence, ObjectKind::SharedFence,
                              "D3D12 shared fence");
        if (!fence || value == 0) {
            return invalidArgument("Invalid D3D12 shared fence signal value.");
        }

        std::lock_guard<std::mutex> lock(fence->valueMutex);
        if (value > fence->reservedValue || value <= fence->lastNativeSignal) {
            return invalidArgument(
                "The D3D12 shared fence signal value was not reserved or is stale.");
        }

        const HRESULT hr = fence->fence->Signal(value);
        if (FAILED(hr)) {
            setHresultError("ID3D12Fence::Signal(CPU)", hr,
                            fence->owner->device.get());
            return hr;
        }
        fence->lastNativeSignal = value;
        return S_OK;
    }

    HRESULT waitSharedFence(SharedFence *fence, uint64_t value,
                            uint32_t timeoutMilliseconds) noexcept {
        clearError();
        fence = requireObject(fence, ObjectKind::SharedFence,
                              "D3D12 shared fence");
        if (!fence) {
            return E_INVALIDARG;
        }
        {
            std::lock_guard<std::mutex> lock(fence->valueMutex);
            if (value > fence->reservedValue) {
                return invalidArgument(
                    "Cannot wait for an unreserved D3D12 shared fence value.");
            }
        }
        return waitForFence(fence->fence.get(), fence->waitEvent.get(),
                            fence->waitMutex, value, timeoutMilliseconds,
                            fence->owner->device.get());
    }

    Texture2D *createTexture2D(Device *device, uint32_t width, uint32_t height,
                               uint16_t mipLevels, int32_t srSurfaceFormat,
                               uint32_t resourceFlags, ResourceState initialState,
                               bool shared) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        if (!device || !device->state || width == 0 || height == 0 ||
            mipLevels == 0) {
            invalidArgument("Invalid D3D12 Texture2D description.");
            return nullptr;
        }

        const DXGI_FORMAT format = mapSurfaceFormat(srSurfaceFormat);
        if (format == DXGI_FORMAT_UNKNOWN) {
            invalidArgument("Unsupported SRSurfaceFormat for D3D12 Texture2D.");
            return nullptr;
        }

        D3D12_RESOURCE_STATES nativeInitialState = {};
        if (!mapResourceState(initialState, nativeInitialState)) {
            invalidArgument("Invalid D3D12 Texture2D initial state code.");
            return nullptr;
        }

        const auto flags = static_cast<D3D12_RESOURCE_FLAGS>(resourceFlags);
        if (initialState == ResourceState::RenderTarget &&
            (flags & D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET) == 0) {
            invalidArgument(
                "A render-target initial state requires ALLOW_RENDER_TARGET.");
            return nullptr;
        }
        if (initialState == ResourceState::DepthWrite &&
            (flags & D3D12_RESOURCE_FLAG_ALLOW_DEPTH_STENCIL) == 0) {
            invalidArgument(
                "A depth-write initial state requires ALLOW_DEPTH_STENCIL.");
            return nullptr;
        }
        if (initialState == ResourceState::UnorderedAccess &&
            (flags & D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS) == 0) {
            invalidArgument(
                "A UAV initial state requires ALLOW_UNORDERED_ACCESS.");
            return nullptr;
        }

        try {
            D3D12_HEAP_PROPERTIES heap = {};
            heap.Type = D3D12_HEAP_TYPE_DEFAULT;
            heap.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
            heap.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
            heap.CreationNodeMask = 1;
            heap.VisibleNodeMask = 1;

            D3D12_RESOURCE_DESC description = {};
            description.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
            description.Alignment = 0;
            description.Width = width;
            description.Height = height;
            description.DepthOrArraySize = 1;
            description.MipLevels = mipLevels;
            description.Format = format;
            description.SampleDesc.Count = 1;
            description.SampleDesc.Quality = 0;
            description.Layout = D3D12_TEXTURE_LAYOUT_UNKNOWN;
            description.Flags = flags;

            ComHandle<ID3D12Resource> resource;
            const D3D12_HEAP_FLAGS heapFlags =
                shared ? D3D12_HEAP_FLAG_SHARED : D3D12_HEAP_FLAG_NONE;
            HRESULT hr = createCom<ID3D12Resource>(
                "ID3D12Device::CreateCommittedResource(Texture2D)", resource,
                device->state->device.get(), [&](ID3D12Resource **result) {
                    return device->state->device->CreateCommittedResource(
                        &heap, heapFlags, &description, nativeInitialState, nullptr,
                        IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            UniqueHandle handle;
            if (shared) {
                hr = createSharedHandle(device->state->device.get(), resource.get(),
                                        handle);
                if (FAILED(hr)) {
                    return nullptr;
                }
            }

            const D3D12_RESOURCE_ALLOCATION_INFO allocation =
                device->state->device->GetResourceAllocationInfo(0, 1,
                                                                 &description);
            if (allocation.SizeInBytes == 0 ||
                allocation.SizeInBytes == std::numeric_limits<uint64_t>::max()) {
                setError("D3D12 returned an invalid Texture2D allocation size.");
                return nullptr;
            }

            ComHandle<ID3D12DescriptorHeap> renderTargetHeap;
            D3D12_CPU_DESCRIPTOR_HANDLE renderTargetView = {};
            if ((flags & D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET) != 0) {
                const DXGI_FORMAT viewFormat =
                    renderTargetViewFormat(srSurfaceFormat);
                if (viewFormat == DXGI_FORMAT_UNKNOWN) {
                    invalidArgument(
                        "The Texture2D format cannot have an RGBA clear view.");
                    return nullptr;
                }

                D3D12_FEATURE_DATA_FORMAT_SUPPORT support = {viewFormat};
                hr = device->state->device->CheckFeatureSupport(
                    D3D12_FEATURE_FORMAT_SUPPORT, &support, sizeof(support));
                if (FAILED(hr) ||
                    (support.Support1 & D3D12_FORMAT_SUPPORT1_RENDER_TARGET) == 0) {
                    if (FAILED(hr)) {
                        setHresultError("ID3D12Device::CheckFeatureSupport(format)",
                                        hr, device->state->device.get());
                    } else {
                        setError("The Texture2D format does not support RTV clears.");
                    }
                    return nullptr;
                }

                D3D12_DESCRIPTOR_HEAP_DESC heapDescription = {};
                heapDescription.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
                heapDescription.NumDescriptors = 1;
                heapDescription.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_NONE;
                hr = createCom<ID3D12DescriptorHeap>(
                    "ID3D12Device::CreateDescriptorHeap(RTV)", renderTargetHeap,
                    device->state->device.get(),
                    [&](ID3D12DescriptorHeap **result) {
                        return device->state->device->CreateDescriptorHeap(
                            &heapDescription, IID_PPV_ARGS(result));
                    });
                if (FAILED(hr)) {
                    return nullptr;
                }

                D3D12_RENDER_TARGET_VIEW_DESC viewDescription = {};
                viewDescription.Format = viewFormat;
                viewDescription.ViewDimension = D3D12_RTV_DIMENSION_TEXTURE2D;
                viewDescription.Texture2D.MipSlice = 0;
                viewDescription.Texture2D.PlaneSlice = 0;
                renderTargetView =
                    renderTargetHeap->GetCPUDescriptorHandleForHeapStart();
                device->state->device->CreateRenderTargetView(
                    resource.get(), &viewDescription, renderTargetView);
            }

            auto stateCell = std::make_shared<TextureStateCell>();
            stateCell->committed = initialState;

            auto *texture = new Texture2D();
            texture->owner = device->state;
            texture->resource = std::move(resource);
            texture->sharedHandle = std::move(handle);
            texture->renderTargetHeap = std::move(renderTargetHeap);
            texture->renderTargetView = renderTargetView;
            texture->allocationSize = allocation.SizeInBytes;
            texture->width = width;
            texture->height = height;
            texture->mipLevels = mipLevels;
            texture->surfaceFormat = srSurfaceFormat;
            texture->resourceFlags = resourceFlags;
            texture->initialState = initialState;
            texture->stateCell = std::move(stateCell);
            texture->resource->SetName(L"SR D3D12 Texture2D");
            if (texture->renderTargetHeap) {
                texture->renderTargetHeap->SetName(
                    L"SR D3D12 Texture2D RTV Heap");
            }
            return texture;
        } catch (const std::bad_alloc &) {
            setError("Could not allocate the D3D12 Texture2D wrapper.");
            return nullptr;
        } catch (...) {
            setError("Unexpected exception while creating a D3D12 Texture2D.");
            return nullptr;
        }
    }

    void destroyTexture2D(Texture2D *texture) noexcept {
        if (!texture) {
            return;
        }
        clearError();
        if (!requireObject(texture, ObjectKind::Texture2D,
                           "D3D12 Texture2D")) {
            return;
        }
        texture->header.magic = 0;
        delete texture;
    }

    ID3D12Resource *nativeTextureResource(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->resource.get() : nullptr;
    }

    HANDLE textureSharedHandle(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->sharedHandle.get() : nullptr;
    }

    uint64_t textureAllocationSize(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->allocationSize : 0;
    }

    uint32_t textureWidth(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->width : 0;
    }

    uint32_t textureHeight(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->height : 0;
    }

    uint16_t textureMipLevels(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->mipLevels : 0;
    }

    int32_t textureSurfaceFormat(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->surfaceFormat : SR_TEXTURE_FORMAT_UNKNOWN;
    }

    uint32_t textureResourceFlags(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->resourceFlags : 0;
    }

    ResourceState textureInitialState(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        return texture ? texture->initialState : ResourceState::Common;
    }

    ResourceState textureCommittedState(Texture2D *texture) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        if (!texture || !texture->stateCell) {
            return ResourceState::Common;
        }
        std::lock_guard<std::mutex> lock(texture->stateCell->mutex);
        return texture->stateCell->committed;
    }

    HRESULT setTextureCommittedState(Texture2D *texture,
                                     ResourceState state) noexcept {
        clearError();
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        D3D12_RESOURCE_STATES ignored = {};
        if (!texture || !texture->stateCell ||
            !mapResourceState(state, ignored)) {
            return invalidArgument("Invalid committed texture state code.");
        }

        std::lock_guard<std::mutex> submitLock(texture->owner->submitMutex);
        std::lock_guard<std::mutex> stateLock(texture->stateCell->mutex);
        texture->stateCell->committed = state;
        ++texture->stateCell->revision;
        return S_OK;
    }

    Buffer *createBuffer(Device *device, uint64_t size, BufferHeap heap,
                         uint32_t resourceFlags, ResourceState initialState,
                         bool shared) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        if (!device || !device->state || size == 0) {
            invalidArgument("Invalid D3D12 buffer description.");
            return nullptr;
        }

        D3D12_HEAP_TYPE nativeHeap = D3D12_HEAP_TYPE_DEFAULT;
        D3D12_RESOURCE_STATES nativeState = D3D12_RESOURCE_STATE_COMMON;
        switch (heap) {
            case BufferHeap::Default:
                if (!mapResourceState(initialState, nativeState) ||
                    initialState == ResourceState::RenderTarget ||
                    initialState == ResourceState::DepthWrite ||
                    initialState == ResourceState::Present) {
                    invalidArgument("Invalid default-buffer initial state code.");
                    return nullptr;
                }
                if (initialState == ResourceState::UnorderedAccess &&
                    (static_cast<D3D12_RESOURCE_FLAGS>(resourceFlags) &
                     D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS) == 0) {
                    invalidArgument(
                        "A UAV buffer initial state requires ALLOW_UNORDERED_ACCESS.");
                    return nullptr;
                }
                break;
            case BufferHeap::Upload:
                if (shared || resourceFlags != 0 ||
                    (initialState != ResourceState::Common &&
                     initialState != ResourceState::ComputeRead &&
                     initialState != ResourceState::CopySource)) {
                    invalidArgument(
                        "Upload buffers must be nonshared, flagless, and read-only.");
                    return nullptr;
                }
                nativeHeap = D3D12_HEAP_TYPE_UPLOAD;
                nativeState = D3D12_RESOURCE_STATE_GENERIC_READ;
                break;
            case BufferHeap::Readback:
                if (shared || resourceFlags != 0 ||
                    (initialState != ResourceState::Common &&
                     initialState != ResourceState::CopyDestination)) {
                    invalidArgument(
                        "Readback buffers must be nonshared, flagless copy destinations.");
                    return nullptr;
                }
                nativeHeap = D3D12_HEAP_TYPE_READBACK;
                nativeState = D3D12_RESOURCE_STATE_COPY_DEST;
                break;
            default:
                invalidArgument("Invalid D3D12 buffer heap code.");
                return nullptr;
        }

        try {
            D3D12_HEAP_PROPERTIES heapProperties = {};
            heapProperties.Type = nativeHeap;
            heapProperties.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
            heapProperties.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
            heapProperties.CreationNodeMask = 1;
            heapProperties.VisibleNodeMask = 1;

            D3D12_RESOURCE_DESC description = {};
            description.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
            description.Width = size;
            description.Height = 1;
            description.DepthOrArraySize = 1;
            description.MipLevels = 1;
            description.Format = DXGI_FORMAT_UNKNOWN;
            description.SampleDesc.Count = 1;
            description.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
            description.Flags = static_cast<D3D12_RESOURCE_FLAGS>(resourceFlags);

            ComHandle<ID3D12Resource> resource;
            const D3D12_HEAP_FLAGS heapFlags =
                shared ? D3D12_HEAP_FLAG_SHARED : D3D12_HEAP_FLAG_NONE;
            HRESULT hr = createCom<ID3D12Resource>(
                "ID3D12Device::CreateCommittedResource(Buffer)", resource,
                device->state->device.get(), [&](ID3D12Resource **result) {
                    return device->state->device->CreateCommittedResource(
                        &heapProperties, heapFlags, &description, nativeState,
                        nullptr, IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            UniqueHandle handle;
            if (shared) {
                hr = createSharedHandle(device->state->device.get(), resource.get(),
                                        handle);
                if (FAILED(hr)) {
                    return nullptr;
                }
            }

            auto *buffer = new Buffer();
            buffer->owner = device->state;
            buffer->resource = std::move(resource);
            buffer->sharedHandle = std::move(handle);
            buffer->size = size;
            buffer->heap = heap;
            buffer->resourceFlags = resourceFlags;
            buffer->initialState = initialState;
            buffer->resource->SetName(L"SR D3D12 Buffer");
            return buffer;
        } catch (const std::bad_alloc &) {
            setError("Could not allocate the D3D12 buffer wrapper.");
            return nullptr;
        } catch (...) {
            setError("Unexpected exception while creating a D3D12 buffer.");
            return nullptr;
        }
    }

    void destroyBuffer(Buffer *buffer) noexcept {
        if (!buffer) {
            return;
        }
        clearError();
        if (!requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer")) {
            return;
        }
        {
            std::lock_guard<std::mutex> lock(buffer->mapMutex);
            if (buffer->mapped) {
                const D3D12_RANGE writtenRange = {
                    static_cast<SIZE_T>(buffer->mappedOffset),
                    static_cast<SIZE_T>(buffer->mappedOffset +
                                        buffer->mappedSize)};
                buffer->resource->Unmap(0, &writtenRange);
                buffer->mapped = false;
            }
        }
        buffer->header.magic = 0;
        delete buffer;
    }

    ID3D12Resource *nativeBufferResource(Buffer *buffer) noexcept {
        clearError();
        buffer = requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer");
        return buffer ? buffer->resource.get() : nullptr;
    }

    HANDLE bufferSharedHandle(Buffer *buffer) noexcept {
        clearError();
        buffer = requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer");
        return buffer ? buffer->sharedHandle.get() : nullptr;
    }

    uint64_t bufferSize(Buffer *buffer) noexcept {
        clearError();
        buffer = requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer");
        return buffer ? buffer->size : 0;
    }

    BufferHeap bufferHeap(Buffer *buffer) noexcept {
        clearError();
        buffer = requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer");
        return buffer ? buffer->heap : BufferHeap::Default;
    }

    uint32_t bufferResourceFlags(Buffer *buffer) noexcept {
        clearError();
        buffer = requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer");
        return buffer ? buffer->resourceFlags : 0;
    }

    ResourceState bufferInitialState(Buffer *buffer) noexcept {
        clearError();
        buffer = requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer");
        return buffer ? buffer->initialState : ResourceState::Common;
    }

    HRESULT mapBuffer(Buffer *buffer, uint64_t offset, uint64_t size,
                      void **mappedData) noexcept {
        clearError();
        if (!mappedData) {
            return invalidArgument(
                "A D3D12 buffer map output pointer is required.");
        }
        *mappedData = nullptr;
        buffer = requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer");
        if (!buffer ||
            !rangeFits(offset, size, buffer ? buffer->size : 0)) {
            return invalidArgument("Invalid D3D12 buffer map range or output pointer.");
        }
        if (buffer->heap != BufferHeap::Upload) {
            return invalidArgument(
                "Only an UPLOAD D3D12 buffer can be mapped directly.");
        }

        std::lock_guard<std::mutex> lock(buffer->mapMutex);
        if (buffer->mapped) {
            setError("The D3D12 buffer is already mapped.");
            return E_FAIL;
        }

        void *base = nullptr;
        const D3D12_RANGE readRange = {0, 0};
        const HRESULT hr = buffer->resource->Map(0, &readRange, &base);
        if (FAILED(hr)) {
            setHresultError("ID3D12Resource::Map(upload buffer)", hr,
                            buffer->owner->device.get());
            return hr;
        }
        if (!base) {
            buffer->resource->Unmap(0, &readRange);
            setError("ID3D12Resource::Map returned a null upload-buffer pointer.");
            return E_FAIL;
        }

        buffer->mapped = true;
        buffer->mappedOffset = offset;
        buffer->mappedSize = size;
        *mappedData = static_cast<std::byte *>(base) + offset;
        return S_OK;
    }

    HRESULT unmapBuffer(Buffer *buffer, uint64_t offset,
                        uint64_t size) noexcept {
        clearError();
        buffer = requireObject(buffer, ObjectKind::Buffer, "D3D12 buffer");
        if (!buffer || !rangeFits(offset, size, buffer ? buffer->size : 0)) {
            return invalidArgument("Invalid D3D12 buffer unmap range.");
        }
        if (buffer->heap != BufferHeap::Upload) {
            return invalidArgument(
                "Only an UPLOAD D3D12 buffer can be unmapped directly.");
        }

        std::lock_guard<std::mutex> lock(buffer->mapMutex);
        if (!buffer->mapped || buffer->mappedOffset != offset ||
            buffer->mappedSize != size) {
            setError("The D3D12 buffer is not mapped with the requested range.");
            return E_FAIL;
        }

        const D3D12_RANGE writtenRange = {
            static_cast<SIZE_T>(offset),
            static_cast<SIZE_T>(offset + size)};
        buffer->resource->Unmap(0, &writtenRange);
        buffer->mapped = false;
        buffer->mappedOffset = 0;
        buffer->mappedSize = 0;
        return S_OK;
    }

    namespace {
        HRESULT validateOwner(const std::shared_ptr<DeviceState> &left,
                              const std::shared_ptr<DeviceState> &right,
                              const char *operation) noexcept {
            if (!left || !right || left.get() != right.get()) {
                setErrorParts(
                    "", operation ? operation : "D3D12 operation",
                    " received objects from different devices.");
                return E_INVALIDARG;
            }
            return S_OK;
        }

        HRESULT requireRecordingLocked(CommandList *commandList,
                                       const char *operation) noexcept {
            if (commandList->state != CommandState::Recording) {
                setErrorParts(
                    "", operation ? operation : "D3D12 command",
                    " requires a recording command list.");
                return E_FAIL;
            }
            return S_OK;
        }

        HRESULT commandTextureStateLocked(CommandList *commandList,
                                          Texture2D *texture,
                                          PendingTextureState **state) noexcept {
            auto existing = commandList->pendingTextureStates.find(
                texture->stateCell.get());
            if (existing != commandList->pendingTextureStates.end()) {
                *state = &existing->second;
                return S_OK;
            }

            PendingTextureState pending;
            pending.cell = texture->stateCell;
            {
                std::lock_guard<std::mutex> lock(texture->stateCell->mutex);
                pending.base = texture->stateCell->committed;
                pending.current = texture->stateCell->committed;
                pending.baseRevision = texture->stateCell->revision;
            }

            try {
                auto [inserted, created] =
                    commandList->pendingTextureStates.emplace(
                        texture->stateCell.get(), std::move(pending));
                (void) created;
                *state = &inserted->second;
                return S_OK;
            } catch (const std::bad_alloc &) {
                setError("Could not allocate command-local texture state tracking.");
                return E_OUTOFMEMORY;
            }
        }

        HRESULT requireCommandTextureStateLocked(CommandList *commandList,
                                                 Texture2D *texture,
                                                 ResourceState expected,
                                                 const char *operation) noexcept {
            PendingTextureState *pending = nullptr;
            HRESULT hr = commandTextureStateLocked(commandList, texture, &pending);
            if (FAILED(hr)) {
                return hr;
            }
            if (pending->current != expected) {
                setErrorParts(
                    "", operation ? operation : "D3D12 texture command",
                    " received a texture in the wrong command-local state.");
                return E_FAIL;
            }
            return S_OK;
        }

        HRESULT validatePendingTextureStatesLocked(
            CommandList *commandList) noexcept {
            for (const auto &entry : commandList->pendingTextureStates) {
                const PendingTextureState &pending = entry.second;
                std::lock_guard<std::mutex> lock(pending.cell->mutex);
                if (pending.cell->revision != pending.baseRevision ||
                    pending.cell->committed != pending.base) {
                    setError(
                        "A texture committed state changed after this command list was recorded.");
                    return E_FAIL;
                }
            }
            return S_OK;
        }

        void commitPendingTextureStatesLocked(CommandList *commandList) noexcept {
            for (auto &entry : commandList->pendingTextureStates) {
                PendingTextureState &pending = entry.second;
                std::lock_guard<std::mutex> lock(pending.cell->mutex);
                pending.cell->committed = pending.current;
                ++pending.cell->revision;
                pending.base = pending.current;
                pending.baseRevision = pending.cell->revision;
            }
        }

        HRESULT createUploadResource(const std::shared_ptr<DeviceState> &owner,
                                     uint64_t size,
                                     ComHandle<ID3D12Resource> &resource) noexcept {
            if (!owner || size == 0) {
                return invalidArgument("Invalid D3D12 upload resource size.");
            }

            D3D12_HEAP_PROPERTIES heap = {};
            heap.Type = D3D12_HEAP_TYPE_UPLOAD;
            heap.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
            heap.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
            heap.CreationNodeMask = 1;
            heap.VisibleNodeMask = 1;

            D3D12_RESOURCE_DESC description = {};
            description.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
            description.Width = size;
            description.Height = 1;
            description.DepthOrArraySize = 1;
            description.MipLevels = 1;
            description.Format = DXGI_FORMAT_UNKNOWN;
            description.SampleDesc.Count = 1;
            description.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;
            description.Flags = D3D12_RESOURCE_FLAG_NONE;

            return createCom<ID3D12Resource>(
                "ID3D12Device::CreateCommittedResource(upload)", resource,
                owner->device.get(), [&](ID3D12Resource **result) {
                    return owner->device->CreateCommittedResource(
                        &heap, D3D12_HEAP_FLAG_NONE, &description,
                        D3D12_RESOURCE_STATE_GENERIC_READ, nullptr,
                        IID_PPV_ARGS(result));
                });
        }

        HRESULT copyToMappedResource(ID3D12Resource *resource, const void *data,
                                     size_t size) noexcept {
            if (!resource || !data || size == 0) {
                return invalidArgument("Invalid D3D12 mapped write arguments.");
            }

            void *mapped = nullptr;
            const D3D12_RANGE readRange = {0, 0};
            HRESULT hr = resource->Map(0, &readRange, &mapped);
            if (FAILED(hr)) {
                ComHandle<ID3D12Device> device;
                resource->GetDevice(IID_PPV_ARGS(device.put()));
                setHresultError("ID3D12Resource::Map", hr, device.get());
                return hr;
            }

            std::memcpy(mapped, data, size);
            const D3D12_RANGE writtenRange = {0, size};
            resource->Unmap(0, &writtenRange);
            return S_OK;
        }

        ResourceView resourceFromOpaque(void *resource) noexcept {
            ResourceView view;
            if (!resource) {
                return view;
            }

            auto *header = static_cast<ObjectHeader *>(resource);
            if (header->magic != OBJECT_MAGIC) {
                setError("Invalid D3D12 resource handle for UAV barrier.");
                return view;
            }

            if (header->kind == ObjectKind::Texture2D) {
                auto *texture = static_cast<Texture2D *>(resource);
                view.owner = texture->owner;
                view.resource = texture->resource.get();
            } else if (header->kind == ObjectKind::Buffer) {
                auto *buffer = static_cast<Buffer *>(resource);
                view.owner = buffer->owner;
                view.resource = buffer->resource.get();
            } else {
                setError("The UAV barrier handle is not a texture or buffer.");
            }
            return view;
        }

        void finalizeSubmissionLocked(CommandList *commandList,
                                      uint64_t completionValue) noexcept {
            commandList->state = CommandState::Submitted;
            commandList->completionValue = completionValue;
            commandList->allocator->recordedOwner = nullptr;
            commandList->allocator->lastCompletionValue =
                std::max(commandList->allocator->lastCompletionValue,
                         completionValue);
        }

        void poisonSubmissionLocked(CommandList *commandList) noexcept {
            commandList->state = CommandState::Poisoned;
            commandList->allocator->poisoned = true;
            commandList->allocator->recordedOwner = nullptr;
        }
    } // namespace

    CommandAllocator *createCommandAllocator(Device *device) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        if (!device || !device->state) {
            return nullptr;
        }

        try {
            ComHandle<ID3D12CommandAllocator> nativeAllocator;
            const HRESULT hr = createCom<ID3D12CommandAllocator>(
                "ID3D12Device::CreateCommandAllocator", nativeAllocator,
                device->state->device.get(),
                [&](ID3D12CommandAllocator **result) {
                    return device->state->device->CreateCommandAllocator(
                        D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            auto state = std::make_shared<AllocatorState>();
            state->owner = device->state;
            state->allocator = std::move(nativeAllocator);
            state->allocator->SetName(L"SR D3D12 Command Allocator");

            auto *allocator = new CommandAllocator();
            allocator->state = std::move(state);
            return allocator;
        } catch (const std::bad_alloc &) {
            setError("Could not allocate the D3D12 command allocator wrapper.");
            return nullptr;
        } catch (...) {
            setError("Unexpected exception while creating a command allocator.");
            return nullptr;
        }
    }

    void destroyCommandAllocator(CommandAllocator *allocator) noexcept {
        if (!allocator) {
            return;
        }
        clearError();
        if (!requireObject(allocator, ObjectKind::CommandAllocator,
                           "D3D12 command allocator")) {
            return;
        }
        allocator->header.magic = 0;
        delete allocator;
    }

    ID3D12CommandAllocator *nativeCommandAllocator(
        CommandAllocator *allocator) noexcept {
        clearError();
        allocator = requireObject(allocator, ObjectKind::CommandAllocator,
                                  "D3D12 command allocator");
        return allocator && allocator->state
                   ? allocator->state->allocator.get()
                   : nullptr;
    }

    CommandList *createCommandList(Device *device,
                                   CommandAllocator *allocator) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        allocator = requireObject(allocator, ObjectKind::CommandAllocator,
                                  "D3D12 command allocator");
        if (!device || !allocator || !device->state || !allocator->state ||
            FAILED(validateOwner(device->state, allocator->state->owner,
                                 "createCommandList"))) {
            return nullptr;
        }

        try {
            ComHandle<ID3D12GraphicsCommandList> nativeList;
            HRESULT hr = createCom<ID3D12GraphicsCommandList>(
                "ID3D12Device::CreateCommandList", nativeList,
                device->state->device.get(),
                [&](ID3D12GraphicsCommandList **result) {
                    return device->state->device->CreateCommandList(
                        0, D3D12_COMMAND_LIST_TYPE_DIRECT,
                        allocator->state->allocator.get(), nullptr,
                        IID_PPV_ARGS(result));
                });
            if (FAILED(hr)) {
                return nullptr;
            }

            hr = nativeList->Close();
            if (FAILED(hr)) {
                setHresultError("ID3D12GraphicsCommandList::Close(initial)", hr,
                                device->state->device.get());
                return nullptr;
            }

            auto *commandList = new CommandList();
            commandList->owner = device->state;
            commandList->allocator = allocator->state;
            commandList->commandList = std::move(nativeList);
            commandList->commandList->SetName(L"SR D3D12 Command List");
            return commandList;
        } catch (const std::bad_alloc &) {
            setError("Could not allocate the D3D12 command list wrapper.");
            return nullptr;
        } catch (...) {
            setError("Unexpected exception while creating a command list.");
            return nullptr;
        }
    }

    HRESULT destroyCommandList(CommandList *commandList) noexcept {
        if (!commandList) {
            return S_OK;
        }
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        if (!commandList) {
            return E_INVALIDARG;
        }

        uint64_t completionValue = 0;
        {
            std::scoped_lock lock(commandList->mutex,
                                  commandList->allocator->mutex);
            if (commandList->state == CommandState::Recording) {
                commandList->commandList->Close();
            }
            if (commandList->allocator->recordedOwner == commandList) {
                commandList->allocator->recordedOwner = nullptr;
            }
            if (commandList->state == CommandState::Submitted) {
                completionValue = commandList->completionValue;
            }
        }
        if (completionValue != 0) {
            const HRESULT hr = waitForInternal(
                commandList->owner, completionValue, WAIT_INFINITE);
            if (FAILED(hr)) {
                return hr;
            }
        }

        commandList->header.magic = 0;
        delete commandList;
        return S_OK;
    }

    HRESULT beginCommandList(CommandList *commandList) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        if (!commandList) {
            return E_INVALIDARG;
        }

        uint64_t completionValue = 0;
        {
            std::scoped_lock lock(commandList->mutex,
                                  commandList->allocator->mutex);
            if (commandList->state == CommandState::Recording) {
                return invalidArgument("The D3D12 command list is already recording.");
            }
            if (commandList->state == CommandState::Executable) {
                return invalidArgument(
                    "Abort or submit the executable D3D12 command list before beginning it again.");
            }
            if (commandList->state == CommandState::Poisoned ||
                commandList->allocator->poisoned) {
                setError("The D3D12 command list allocator is poisoned after an untracked submission.");
                return E_FAIL;
            }
            if (commandList->allocator->recordedOwner &&
                commandList->allocator->recordedOwner != commandList) {
                setError("The D3D12 command allocator is owned by another command list.");
                return E_FAIL;
            }
            completionValue = std::max(
                commandList->completionValue,
                commandList->allocator->lastCompletionValue);
        }

        HRESULT hr = waitForInternal(commandList->owner, completionValue,
                                     WAIT_INFINITE);
        if (FAILED(hr)) {
            return hr;
        }

        std::scoped_lock lock(commandList->mutex,
                              commandList->allocator->mutex);
        if (commandList->allocator->recordedOwner &&
            commandList->allocator->recordedOwner != commandList) {
            setError("The D3D12 command allocator became busy while beginning a command list.");
            return E_FAIL;
        }
        if (commandList->state == CommandState::Recording ||
            commandList->state == CommandState::Executable ||
            commandList->state == CommandState::Poisoned) {
            setError("The D3D12 command list state changed while waiting for completion.");
            return E_FAIL;
        }

        commandList->retainedObjects.clear();
        commandList->pendingTextureStates.clear();
        hr = commandList->allocator->allocator->Reset();
        if (FAILED(hr)) {
            setHresultError("ID3D12CommandAllocator::Reset", hr,
                            commandList->owner->device.get());
            return hr;
        }
        hr = commandList->commandList->Reset(
            commandList->allocator->allocator.get(), nullptr);
        if (FAILED(hr)) {
            setHresultError("ID3D12GraphicsCommandList::Reset", hr,
                            commandList->owner->device.get());
            return hr;
        }

        commandList->allocator->recordedOwner = commandList;
        commandList->allocator->lastCompletionValue = 0;
        commandList->completionValue = 0;
        commandList->state = CommandState::Recording;
        return S_OK;
    }

    HRESULT endCommandList(CommandList *commandList) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        if (!commandList) {
            return E_INVALIDARG;
        }

        std::scoped_lock lock(commandList->mutex,
                              commandList->allocator->mutex);
        if (commandList->state != CommandState::Recording ||
            commandList->allocator->recordedOwner != commandList) {
            return invalidArgument("The D3D12 command list is not recording.");
        }

        const HRESULT hr = commandList->commandList->Close();
        if (FAILED(hr)) {
            setHresultError("ID3D12GraphicsCommandList::Close", hr,
                            commandList->owner->device.get());
            commandList->allocator->recordedOwner = nullptr;
            commandList->state = CommandState::Poisoned;
            commandList->allocator->poisoned = true;
            commandList->pendingTextureStates.clear();
            return hr;
        }
        commandList->state = CommandState::Executable;
        return S_OK;
    }

    HRESULT abortCommandList(CommandList *commandList) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        if (!commandList) {
            return E_INVALIDARG;
        }

        uint64_t completionValue = 0;
        {
            std::scoped_lock lock(commandList->mutex,
                                  commandList->allocator->mutex);
            if (commandList->state == CommandState::Poisoned) {
                setError("A poisoned D3D12 command list cannot be aborted safely.");
                return E_FAIL;
            }
            if (commandList->state == CommandState::Recording) {
                const HRESULT hr = commandList->commandList->Close();
                if (FAILED(hr)) {
                    setHresultError("ID3D12GraphicsCommandList::Close(abort)", hr,
                                    commandList->owner->device.get());
                    poisonSubmissionLocked(commandList);
                    return hr;
                }
            }
            if (commandList->state == CommandState::Submitted) {
                completionValue = commandList->completionValue;
            }
            if (commandList->allocator->recordedOwner == commandList) {
                commandList->allocator->recordedOwner = nullptr;
            }
        }

        if (completionValue != 0) {
            const HRESULT hr = waitForInternal(commandList->owner, completionValue,
                                               WAIT_INFINITE);
            if (FAILED(hr)) {
                return hr;
            }
        }

        std::scoped_lock lock(commandList->mutex,
                              commandList->allocator->mutex);
        commandList->retainedObjects.clear();
        commandList->pendingTextureStates.clear();
        commandList->completionValue = 0;
        commandList->state = CommandState::Closed;
        return S_OK;
    }

    ID3D12GraphicsCommandList *checkedNativeCommandList(
        CommandList *commandList) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        if (!commandList) {
            return nullptr;
        }
        std::lock_guard<std::mutex> lock(commandList->mutex);
        if (FAILED(requireRecordingLocked(commandList,
                                          "checkedNativeCommandList"))) {
            return nullptr;
        }
        return commandList->commandList.get();
    }

    ResourceState commandTextureState(CommandList *commandList,
                                      Texture2D *texture) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        if (!commandList || !texture ||
            FAILED(validateOwner(commandList->owner, texture->owner,
                                 "commandTextureState"))) {
            return ResourceState::Common;
        }

        std::lock_guard<std::mutex> lock(commandList->mutex);
        if (FAILED(requireRecordingLocked(commandList,
                                          "commandTextureState"))) {
            return ResourceState::Common;
        }
        PendingTextureState *pending = nullptr;
        if (FAILED(commandTextureStateLocked(commandList, texture, &pending))) {
            return ResourceState::Common;
        }
        return pending->current;
    }

    HRESULT setCommandTextureState(CommandList *commandList, Texture2D *texture,
                                   ResourceState state) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        D3D12_RESOURCE_STATES ignored = {};
        if (!commandList || !texture || !mapResourceState(state, ignored) ||
            FAILED(validateOwner(commandList->owner, texture->owner,
                                 "setCommandTextureState"))) {
            return invalidArgument("Invalid command-local texture state.");
        }

        std::lock_guard<std::mutex> lock(commandList->mutex);
        HRESULT hr = requireRecordingLocked(commandList,
                                            "setCommandTextureState");
        if (FAILED(hr)) {
            return hr;
        }
        PendingTextureState *pending = nullptr;
        hr = commandTextureStateLocked(commandList, texture, &pending);
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects, texture->resource.get());
        if (FAILED(hr)) {
            return hr;
        }
        pending->current = state;
        return S_OK;
    }

    HRESULT transitionTexture(CommandList *commandList, Texture2D *texture,
                              ResourceState before,
                              ResourceState after) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        if (!commandList || !texture ||
            FAILED(validateOwner(commandList->owner, texture->owner,
                                 "transitionTexture"))) {
            return E_INVALIDARG;
        }

        D3D12_RESOURCE_STATES beforeState = {};
        D3D12_RESOURCE_STATES afterState = {};
        if (!mapResourceState(before, beforeState) ||
            !mapResourceState(after, afterState)) {
            return invalidArgument("Invalid D3D12 texture transition state code.");
        }
        std::lock_guard<std::mutex> lock(commandList->mutex);
        HRESULT hr = requireRecordingLocked(commandList, "transitionTexture");
        if (FAILED(hr)) {
            return hr;
        }
        PendingTextureState *pending = nullptr;
        hr = commandTextureStateLocked(commandList, texture, &pending);
        if (FAILED(hr)) {
            return hr;
        }
        if (pending->current != before) {
            setError(
                "The texture transition before-state does not match command-local state.");
            return E_FAIL;
        }
        hr = retainObject(commandList->retainedObjects, texture->resource.get());
        if (FAILED(hr)) {
            return hr;
        }

        if (beforeState != afterState) {
            D3D12_RESOURCE_BARRIER barrier = {};
            barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
            barrier.Flags = D3D12_RESOURCE_BARRIER_FLAG_NONE;
            barrier.Transition.pResource = texture->resource.get();
            barrier.Transition.Subresource =
                D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
            barrier.Transition.StateBefore = beforeState;
            barrier.Transition.StateAfter = afterState;
            commandList->commandList->ResourceBarrier(1, &barrier);
        }
        pending->current = after;
        return S_OK;
    }

    HRESULT copyTexture(CommandList *commandList, Texture2D *source,
                        Texture2D *destination,
                        const TextureCopyRegion &region) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        source = requireObject(source, ObjectKind::Texture2D,
                               "D3D12 source Texture2D");
        destination = requireObject(destination, ObjectKind::Texture2D,
                                    "D3D12 destination Texture2D");
        if (!commandList || !source || !destination ||
            FAILED(validateOwner(commandList->owner, source->owner,
                                 "copyTexture")) ||
            FAILED(validateOwner(commandList->owner, destination->owner,
                                 "copyTexture"))) {
            return E_INVALIDARG;
        }
        if (source->surfaceFormat != destination->surfaceFormat ||
            region.sourceMip >= source->mipLevels ||
            region.destinationMip >= destination->mipLevels) {
            return invalidArgument(
                "Texture copy formats or mip levels are incompatible.");
        }

        const uint32_t sourceWidth = mipExtent(source->width, region.sourceMip);
        const uint32_t sourceHeight = mipExtent(source->height, region.sourceMip);
        const uint32_t destinationWidth =
            mipExtent(destination->width, region.destinationMip);
        const uint32_t destinationHeight =
            mipExtent(destination->height, region.destinationMip);
        if (!rectangleFits(region.sourceX, region.sourceY, region.width,
                           region.height, sourceWidth, sourceHeight) ||
            !rectangleFits(region.destinationX, region.destinationY,
                           region.width, region.height, destinationWidth,
                           destinationHeight)) {
            return invalidArgument("The D3D12 texture copy region is out of range.");
        }

        std::lock_guard<std::mutex> lock(commandList->mutex);
        HRESULT hr = requireRecordingLocked(commandList, "copyTexture");
        if (FAILED(hr)) {
            return hr;
        }
        hr = requireCommandTextureStateLocked(
            commandList, source, ResourceState::CopySource, "copyTexture(source)");
        if (FAILED(hr)) {
            return hr;
        }
        hr = requireCommandTextureStateLocked(
            commandList, destination, ResourceState::CopyDestination,
            "copyTexture(destination)");
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects, source->resource.get());
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects,
                          destination->resource.get());
        if (FAILED(hr)) {
            return hr;
        }

        D3D12_TEXTURE_COPY_LOCATION sourceLocation = {};
        sourceLocation.pResource = source->resource.get();
        sourceLocation.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
        sourceLocation.SubresourceIndex = region.sourceMip;

        D3D12_TEXTURE_COPY_LOCATION destinationLocation = {};
        destinationLocation.pResource = destination->resource.get();
        destinationLocation.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
        destinationLocation.SubresourceIndex = region.destinationMip;

        D3D12_BOX sourceBox = {};
        sourceBox.left = region.sourceX;
        sourceBox.top = region.sourceY;
        sourceBox.front = 0;
        sourceBox.right = region.sourceX + region.width;
        sourceBox.bottom = region.sourceY + region.height;
        sourceBox.back = 1;
        commandList->commandList->CopyTextureRegion(
            &destinationLocation, region.destinationX, region.destinationY, 0,
            &sourceLocation, &sourceBox);
        return S_OK;
    }

    HRESULT copyBuffer(CommandList *commandList, Buffer *source,
                       Buffer *destination, uint64_t sourceOffset,
                       uint64_t destinationOffset, uint64_t size) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        source = requireObject(source, ObjectKind::Buffer, "D3D12 source buffer");
        destination = requireObject(destination, ObjectKind::Buffer,
                                    "D3D12 destination buffer");
        if (!commandList || !source || !destination || size == 0 ||
            FAILED(validateOwner(commandList->owner, source->owner,
                                 "copyBuffer")) ||
            FAILED(validateOwner(commandList->owner, destination->owner,
                                 "copyBuffer")) ||
            !rangeFits(sourceOffset, size, source->size) ||
            !rangeFits(destinationOffset, size, destination->size)) {
            return invalidArgument("Invalid D3D12 buffer copy range or owner.");
        }
        if (source->heap != BufferHeap::Upload ||
            source->initialState != ResourceState::CopySource ||
            destination->heap != BufferHeap::Default ||
            destination->initialState != ResourceState::CopyDestination) {
            return invalidArgument(
                "Stage-1 D3D12 buffer copies require an UPLOAD/COPY_SOURCE "
                "source and a DEFAULT/COPY_DESTINATION destination.");
        }

        std::lock_guard<std::mutex> lock(commandList->mutex);
        HRESULT hr = requireRecordingLocked(commandList, "copyBuffer");
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects, source->resource.get());
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects,
                          destination->resource.get());
        if (FAILED(hr)) {
            return hr;
        }
        commandList->commandList->CopyBufferRegion(
            destination->resource.get(), destinationOffset, source->resource.get(),
            sourceOffset, size);
        return S_OK;
    }

    HRESULT writeBuffer(CommandList *commandList, Buffer *destination,
                        uint64_t destinationOffset, const void *data,
                        size_t size) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        destination = requireObject(destination, ObjectKind::Buffer,
                                    "D3D12 destination buffer");
        if (!commandList || !destination || !data || size == 0 ||
            FAILED(validateOwner(commandList->owner, destination->owner,
                                 "writeBuffer")) ||
            !rangeFits(destinationOffset, size, destination->size)) {
            return invalidArgument("Invalid D3D12 buffer write range or data.");
        }
        if (destination->heap == BufferHeap::Readback) {
            return invalidArgument("Cannot write a D3D12 readback buffer.");
        }
        if (destination->heap == BufferHeap::Default &&
            destination->initialState != ResourceState::CopyDestination) {
            return invalidArgument(
                "Writing a DEFAULT D3D12 buffer requires COPY_DESTINATION state.");
        }

        std::lock_guard<std::mutex> lock(commandList->mutex);
        HRESULT hr = requireRecordingLocked(commandList, "writeBuffer");
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects,
                          destination->resource.get());
        if (FAILED(hr)) {
            return hr;
        }

        if (destination->heap == BufferHeap::Upload) {
            std::lock_guard<std::mutex> mapLock(destination->mapMutex);
            if (destination->mapped) {
                setError(
                    "Cannot record an upload-buffer write while the buffer is mapped.");
                return E_FAIL;
            }
            void *mapped = nullptr;
            const D3D12_RANGE readRange = {0, 0};
            hr = destination->resource->Map(0, &readRange, &mapped);
            if (FAILED(hr)) {
                setHresultError("ID3D12Resource::Map(upload buffer)", hr,
                                destination->owner->device.get());
                return hr;
            }
            std::memcpy(static_cast<std::byte *>(mapped) + destinationOffset, data,
                        size);
            const D3D12_RANGE writtenRange = {
                static_cast<SIZE_T>(destinationOffset),
                static_cast<SIZE_T>(destinationOffset + size)};
            destination->resource->Unmap(0, &writtenRange);
            return S_OK;
        }

        ComHandle<ID3D12Resource> upload;
        hr = createUploadResource(commandList->owner, size, upload);
        if (FAILED(hr)) {
            return hr;
        }
        hr = copyToMappedResource(upload.get(), data, size);
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects, upload.get());
        if (FAILED(hr)) {
            return hr;
        }
        commandList->commandList->CopyBufferRegion(
            destination->resource.get(), destinationOffset, upload.get(), 0, size);
        return S_OK;
    }

    HRESULT writeTexture(CommandList *commandList, Texture2D *destination,
                         const TextureWriteRegion &region, const void *data,
                         size_t size) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        destination = requireObject(destination, ObjectKind::Texture2D,
                                    "D3D12 destination Texture2D");
        if (!commandList || !destination || !data || size == 0 ||
            FAILED(validateOwner(commandList->owner, destination->owner,
                                 "writeTexture")) ||
            region.mip >= destination->mipLevels) {
            return invalidArgument("Invalid D3D12 texture write arguments.");
        }

        const uint32_t destinationWidth =
            mipExtent(destination->width, region.mip);
        const uint32_t destinationHeight =
            mipExtent(destination->height, region.mip);
        if (!rectangleFits(region.x, region.y, region.width, region.height,
                           destinationWidth, destinationHeight)) {
            return invalidArgument("The D3D12 texture write region is out of range.");
        }

        const uint32_t pixelSize = bytesPerPixel(destination->surfaceFormat);
        if (pixelSize == 0 ||
            region.width > std::numeric_limits<uint32_t>::max() / pixelSize) {
            return invalidArgument("Unsupported or overflowing texture row size.");
        }
        const uint32_t rowBytes = region.width * pixelSize;
        const uint32_t sourceRowPitch =
            region.sourceRowPitch == 0 ? rowBytes : region.sourceRowPitch;
        if (sourceRowPitch < rowBytes) {
            return invalidArgument("The source texture row pitch is too small.");
        }
        const uint64_t requiredSourceSize =
            static_cast<uint64_t>(sourceRowPitch) * (region.height - 1) + rowBytes;
        if (requiredSourceSize > size) {
            return invalidArgument("The source texture data buffer is too small.");
        }

        const uint64_t uploadRowPitch =
            alignUp(rowBytes, D3D12_TEXTURE_DATA_PITCH_ALIGNMENT);
        if (uploadRowPitch == 0 ||
            uploadRowPitch > std::numeric_limits<uint32_t>::max() ||
            region.height >
                std::numeric_limits<uint64_t>::max() / uploadRowPitch) {
            return invalidArgument("The D3D12 texture upload size overflowed.");
        }
        const uint64_t uploadSize = uploadRowPitch * region.height;

        std::lock_guard<std::mutex> lock(commandList->mutex);
        HRESULT hr = requireRecordingLocked(commandList, "writeTexture");
        if (FAILED(hr)) {
            return hr;
        }
        hr = requireCommandTextureStateLocked(
            commandList, destination, ResourceState::CopyDestination,
            "writeTexture");
        if (FAILED(hr)) {
            return hr;
        }

        ComHandle<ID3D12Resource> upload;
        hr = createUploadResource(commandList->owner, uploadSize, upload);
        if (FAILED(hr)) {
            return hr;
        }

        void *mapped = nullptr;
        const D3D12_RANGE readRange = {0, 0};
        hr = upload->Map(0, &readRange, &mapped);
        if (FAILED(hr)) {
            setHresultError("ID3D12Resource::Map(texture upload)", hr,
                            commandList->owner->device.get());
            return hr;
        }
        const auto *sourceBytes = static_cast<const std::byte *>(data);
        auto *destinationBytes = static_cast<std::byte *>(mapped);
        for (uint32_t row = 0; row < region.height; ++row) {
            std::memcpy(destinationBytes + uploadRowPitch * row,
                        sourceBytes + static_cast<uint64_t>(sourceRowPitch) * row,
                        rowBytes);
        }
        const D3D12_RANGE writtenRange = {0, static_cast<SIZE_T>(uploadSize)};
        upload->Unmap(0, &writtenRange);

        hr = retainObject(commandList->retainedObjects,
                          destination->resource.get());
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects, upload.get());
        if (FAILED(hr)) {
            return hr;
        }

        D3D12_TEXTURE_COPY_LOCATION sourceLocation = {};
        sourceLocation.pResource = upload.get();
        sourceLocation.Type = D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT;
        sourceLocation.PlacedFootprint.Offset = 0;
        sourceLocation.PlacedFootprint.Footprint.Format =
            mapSurfaceFormat(destination->surfaceFormat);
        sourceLocation.PlacedFootprint.Footprint.Width = region.width;
        sourceLocation.PlacedFootprint.Footprint.Height = region.height;
        sourceLocation.PlacedFootprint.Footprint.Depth = 1;
        sourceLocation.PlacedFootprint.Footprint.RowPitch =
            static_cast<uint32_t>(uploadRowPitch);

        D3D12_TEXTURE_COPY_LOCATION destinationLocation = {};
        destinationLocation.pResource = destination->resource.get();
        destinationLocation.Type = D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX;
        destinationLocation.SubresourceIndex = region.mip;
        commandList->commandList->CopyTextureRegion(
            &destinationLocation, region.x, region.y, 0, &sourceLocation, nullptr);
        return S_OK;
    }

    HRESULT clearTextureRgba(CommandList *commandList, Texture2D *texture,
                             float red, float green, float blue,
                             float alpha) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        texture = requireObject(texture, ObjectKind::Texture2D,
                                "D3D12 Texture2D");
        if (!commandList || !texture ||
            FAILED(validateOwner(commandList->owner, texture->owner,
                                 "clearTextureRgba"))) {
            return E_INVALIDARG;
        }
        if (!texture->renderTargetHeap ||
            (texture->resourceFlags &
             D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET) == 0) {
            return invalidArgument(
                "RGBA clear requires a Texture2D created with ALLOW_RENDER_TARGET.");
        }

        std::lock_guard<std::mutex> lock(commandList->mutex);
        HRESULT hr = requireRecordingLocked(commandList, "clearTextureRgba");
        if (FAILED(hr)) {
            return hr;
        }
        hr = requireCommandTextureStateLocked(
            commandList, texture, ResourceState::RenderTarget,
            "clearTextureRgba");
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects, texture->resource.get());
        if (FAILED(hr)) {
            return hr;
        }
        hr = retainObject(commandList->retainedObjects,
                          texture->renderTargetHeap.get());
        if (FAILED(hr)) {
            return hr;
        }

        const float color[4] = {red, green, blue, alpha};
        commandList->commandList->ClearRenderTargetView(
            texture->renderTargetView, color, 0, nullptr);
        return S_OK;
    }

    HRESULT uavBarrier(CommandList *commandList,
                       void *resourceOrNull) noexcept {
        clearError();
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        if (!commandList) {
            return E_INVALIDARG;
        }

        ResourceView resource;
        if (resourceOrNull) {
            resource = resourceFromOpaque(resourceOrNull);
            if (!resource.resource ||
                FAILED(validateOwner(commandList->owner, resource.owner,
                                     "uavBarrier"))) {
                return E_INVALIDARG;
            }
        }

        std::lock_guard<std::mutex> lock(commandList->mutex);
        HRESULT hr = requireRecordingLocked(commandList, "uavBarrier");
        if (FAILED(hr)) {
            return hr;
        }
        if (resource.resource) {
            if (resourceOrNull &&
                static_cast<ObjectHeader *>(resourceOrNull)->kind ==
                    ObjectKind::Texture2D) {
                auto *texture = static_cast<Texture2D *>(resourceOrNull);
                hr = requireCommandTextureStateLocked(
                    commandList, texture, ResourceState::UnorderedAccess,
                    "uavBarrier");
                if (FAILED(hr)) {
                    return hr;
                }
            }
            hr = retainObject(commandList->retainedObjects, resource.resource);
            if (FAILED(hr)) {
                return hr;
            }
        }

        D3D12_RESOURCE_BARRIER barrier = {};
        barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_UAV;
        barrier.Flags = D3D12_RESOURCE_BARRIER_FLAG_NONE;
        barrier.UAV.pResource = resource.resource;
        commandList->commandList->ResourceBarrier(1, &barrier);
        return S_OK;
    }

    HRESULT submit(Device *device, CommandList *commandList,
                   SharedFence *sharedFence, uint64_t waitValue,
                   uint64_t signalValue,
                   SubmissionDisposition *outDisposition) noexcept {
        clearError();
        if (outDisposition) {
            *outDisposition = SubmissionDisposition::NotExecuted;
        }
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        commandList = requireObject(commandList, ObjectKind::CommandList,
                                    "D3D12 command list");
        if (!device || !commandList || !device->state ||
            FAILED(validateOwner(device->state, commandList->owner, "submit"))) {
            return E_INVALIDARG;
        }
        if (sharedFence) {
            sharedFence = requireObject(sharedFence, ObjectKind::SharedFence,
                                        "D3D12 shared fence");
            if (!sharedFence ||
                FAILED(validateOwner(commandList->owner, sharedFence->owner,
                                     "submit"))) {
                return E_INVALIDARG;
            }
        } else if (waitValue != 0 || signalValue != 0) {
            return invalidArgument(
                "A shared fence is required for nonzero wait or signal values.");
        }

        std::unique_lock<std::mutex> submitLock(commandList->owner->submitMutex);
        std::unique_lock<std::mutex> listLock(commandList->mutex);
        std::unique_lock<std::mutex> allocatorLock(commandList->allocator->mutex);
        std::unique_lock<std::mutex> fenceValueLock;
        if (sharedFence) {
            fenceValueLock = std::unique_lock<std::mutex>(sharedFence->valueMutex);
        }

        if (commandList->state != CommandState::Executable ||
            commandList->allocator->recordedOwner != commandList ||
            commandList->allocator->poisoned) {
            return invalidArgument(
                "Only an executable D3D12 command list can be submitted.");
        }
        if (sharedFence) {
            if (signalValue == 0 || signalValue <= waitValue ||
                waitValue > sharedFence->reservedValue ||
                signalValue > sharedFence->reservedValue ||
                signalValue <= sharedFence->lastNativeSignal) {
                return invalidArgument(
                    "D3D12 submit fence values must be reserved, increasing, and signal > wait.");
            }
        }
        HRESULT hr = validatePendingTextureStatesLocked(commandList);
        if (FAILED(hr)) {
            return hr;
        }
        if (commandList->owner->nextCompletionValue >=
            static_cast<uint64_t>(std::numeric_limits<int64_t>::max())) {
            setError("The internal D3D12 completion fence value space is exhausted.");
            return E_FAIL;
        }

        const size_t retainedCount = commandList->retainedObjects.size();
        if (sharedFence) {
            const HRESULT retainHr = retainObject(commandList->retainedObjects,
                                                  sharedFence->fence.get());
            if (FAILED(retainHr)) {
                return retainHr;
            }
        }

        try {
            commandList->owner->quarantinedSubmissions.emplace_back();
        } catch (const std::bad_alloc &) {
            commandList->retainedObjects.resize(retainedCount);
            setError(
                "Could not reserve a D3D12 untracked-submission quarantine slot.");
            return E_OUTOFMEMORY;
        }

        hr = S_OK;
        if (sharedFence && waitValue != 0) {
            hr = commandList->owner->queue->Wait(sharedFence->fence.get(),
                                                 waitValue);
            if (FAILED(hr)) {
                commandList->owner->quarantinedSubmissions.pop_back();
                commandList->retainedObjects.resize(retainedCount);
                setHresultError("ID3D12CommandQueue::Wait(shared fence)", hr,
                                commandList->owner->device.get());
                return hr;
            }
        }

        ID3D12CommandList *nativeLists[] = {commandList->commandList.get()};
        commandList->owner->queue->ExecuteCommandLists(1, nativeLists);
        if (outDisposition) {
            *outDisposition = SubmissionDisposition::ExecutedUntracked;
        }
        // From this point the recorded barriers are part of the serialized GPU
        // stream. Commit their final states even if a later fence signal needs
        // recovery, because rolling them back would disagree with GPU reality.
        commitPendingTextureStatesLocked(commandList);

        const uint64_t completionValue =
            ++commandList->owner->nextCompletionValue;
        hr = signalSubmissionCompletion(commandList->owner.get(),
                                        completionValue);
        if (FAILED(hr)) {
            const HRESULT completionSignalFailure = hr;
            setHresultError("ID3D12CommandQueue::Signal(internal completion)", hr,
                            commandList->owner->device.get());
            const std::array<char, LAST_ERROR_CAPACITY> completionSignalDiagnostic =
                g_lastError;

            // ExecuteCommandLists has no return value. If its internal completion
            // signal fails, queue a shared-fence signal and wait for it on the CPU.
            // A successful wait proves the commands completed and keeps the
            // allocator reusable without an untracked in-flight submission.
            if (sharedFence) {
                const HRESULT recoverySignal = signalSharedFenceOnQueue(
                    commandList->owner.get(), sharedFence->fence.get(),
                    signalValue);
                if (SUCCEEDED(recoverySignal)) {
                    sharedFence->lastNativeSignal = signalValue;
                    const HRESULT recoveryWait = waitForFence(
                        sharedFence->fence.get(), sharedFence->waitEvent.get(),
                        sharedFence->waitMutex, signalValue, WAIT_INFINITE,
                        commandList->owner->device.get());
                    if (SUCCEEDED(recoveryWait)) {
                        commandList->owner->quarantinedSubmissions.pop_back();
                        finalizeSubmissionLocked(commandList, 0);
                        if (outDisposition) {
                            *outDisposition = SubmissionDisposition::Submitted;
                        }
                        clearError();
                        return S_OK;
                    }
                }
            }

            QuarantinedSubmission &quarantine =
                commandList->owner->quarantinedSubmissions.back();
            quarantine.allocator =
                std::move(commandList->allocator->allocator);
            quarantine.commandList = std::move(commandList->commandList);
            quarantine.retainedObjects =
                std::move(commandList->retainedObjects);
            poisonSubmissionLocked(commandList);
            setError(completionSignalDiagnostic.data());
            return completionSignalFailure;
        }

        commandList->owner->quarantinedSubmissions.pop_back();

        commandList->owner->lastSubmitted.store(completionValue,
                                                std::memory_order_release);
        if (outDisposition) {
            *outDisposition = SubmissionDisposition::Submitted;
        }
        if (sharedFence) {
            hr = signalSharedFenceOnQueue(commandList->owner.get(),
                                          sharedFence->fence.get(), signalValue);
            if (FAILED(hr)) {
                setHresultError("ID3D12CommandQueue::Signal(shared fence)", hr,
                                commandList->owner->device.get());

                // Preserve output-ready ordering if the queue signal itself
                // fails: wait until the internal completion point, then signal
                // the shared fence from the CPU.
                const HRESULT completionWait = waitForInternal(
                    commandList->owner, completionValue, WAIT_INFINITE);
                if (SUCCEEDED(completionWait)) {
                    const HRESULT recoverySignal =
                        signalCpuSharedFence(sharedFence->fence.get(),
                                             signalValue);
                    if (SUCCEEDED(recoverySignal)) {
                        sharedFence->lastNativeSignal = signalValue;
                        finalizeSubmissionLocked(commandList, completionValue);
                        return S_OK;
                    }
                    setHresultError("ID3D12Fence::Signal(shared recovery)",
                                    recoverySignal,
                                    commandList->owner->device.get());
                    hr = recoverySignal;
                } else {
                    hr = completionWait;
                }
                finalizeSubmissionLocked(commandList, completionValue);
                return hr;
            }
            sharedFence->lastNativeSignal = signalValue;
        }

        finalizeSubmissionLocked(commandList, completionValue);
        return S_OK;
    }

    HRESULT recoverSharedFence(Device *device, SharedFence *sharedFence,
                               uint64_t waitValue,
                               uint64_t signalValue) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        sharedFence = requireObject(sharedFence, ObjectKind::SharedFence,
                                    "D3D12 shared fence");
        if (!device || !sharedFence || !device->state ||
            FAILED(validateOwner(device->state, sharedFence->owner,
                                 "recoverSharedFence"))) {
            return E_INVALIDARG;
        }

        std::unique_lock<std::mutex> submitLock(device->state->submitMutex);
        std::unique_lock<std::mutex> valueLock(sharedFence->valueMutex);
        if (signalValue == 0 || signalValue <= waitValue ||
            waitValue > sharedFence->reservedValue ||
            signalValue > sharedFence->reservedValue) {
            return invalidArgument(
                "Recovery fence values must be reserved, increasing, and signal > wait.");
        }

        // A previous attempt may already have queued or issued this signal and
        // then failed only while waiting for completion. Retrying must not queue
        // a duplicate signal; waiting for the already-issued value is idempotent.
        // Do not clear quarantine here because an older signal alone cannot prove
        // ownership of submissions quarantined after it was issued.
        if (sharedFence->lastNativeSignal >= signalValue) {
            return waitForFence(sharedFence->fence.get(),
                                sharedFence->waitEvent.get(),
                                sharedFence->waitMutex, signalValue,
                                WAIT_INFINITE, device->state->device.get());
        }

        HRESULT queueWait = S_OK;
        if (waitValue != 0) {
            queueWait = device->state->queue->Wait(sharedFence->fence.get(),
                                                   waitValue);
        }
        if (SUCCEEDED(queueWait)) {
            const HRESULT queueSignal = signalSharedFenceOnQueue(
                device->state.get(), sharedFence->fence.get(), signalValue);
            if (SUCCEEDED(queueSignal)) {
                sharedFence->lastNativeSignal = signalValue;
                const HRESULT queueCompletion = waitForFence(
                    sharedFence->fence.get(), sharedFence->waitEvent.get(),
                    sharedFence->waitMutex, signalValue, WAIT_INFINITE,
                    device->state->device.get());
                if (SUCCEEDED(queueCompletion)) {
                    device->state->quarantinedSubmissions.clear();
                }
                return queueCompletion;
            }
            setHresultError("ID3D12CommandQueue::Signal(empty recovery)",
                            queueSignal, device->state->device.get());
        } else {
            setHresultError("ID3D12CommandQueue::Wait(empty recovery)",
                            queueWait, device->state->device.get());
        }

        // This API is intended for an empty handoff, but prove all previously
        // queued work anyway before using the CPU fallback. That keeps a future
        // mistaken call from releasing a quarantined or tracked submission to
        // the consumer before its command list has finished.
        const HRESULT completionProof = proveQueuedWorkCompletedLocked(
            device->state,
            "ID3D12CommandQueue::Signal(shared recovery drain)");
        if (FAILED(completionProof)) {
            return completionProof;
        }
        const HRESULT producerWait = waitForFence(
            sharedFence->fence.get(), sharedFence->waitEvent.get(),
            sharedFence->waitMutex, waitValue, WAIT_INFINITE,
            device->state->device.get());
        if (FAILED(producerWait)) {
            return producerWait;
        }
        const HRESULT cpuSignal = signalCpuSharedFence(
            sharedFence->fence.get(), signalValue);
        if (FAILED(cpuSignal)) {
            setHresultError("ID3D12Fence::Signal(empty recovery)", cpuSignal,
                            device->state->device.get());
            return cpuSignal;
        }
        sharedFence->lastNativeSignal = signalValue;
        clearError();
        return S_OK;
    }

    HRESULT recoverExecutedSharedFence(Device *device,
                                       SharedFence *sharedFence,
                                       uint64_t waitValue,
                                       uint64_t signalValue) noexcept {
        clearError();
        device = requireObject(device, ObjectKind::Device, "D3D12 device");
        sharedFence = requireObject(sharedFence, ObjectKind::SharedFence,
                                    "D3D12 shared fence");
        if (!device || !sharedFence || !device->state ||
            FAILED(validateOwner(device->state, sharedFence->owner,
                                 "recoverExecutedSharedFence"))) {
            return E_INVALIDARG;
        }

        std::unique_lock<std::mutex> submitLock(device->state->submitMutex);
        std::unique_lock<std::mutex> valueLock(sharedFence->valueMutex);
        if (signalValue == 0 || signalValue <= waitValue ||
            waitValue > sharedFence->reservedValue ||
            signalValue > sharedFence->reservedValue) {
            return invalidArgument(
                "Executed recovery fence values must be reserved and signal > wait.");
        }
        // ExecuteCommandLists may already be in the queue. Only a new queue
        // completion proof can authorize a CPU handoff. This path never uses a
        // producer-only fallback.
        HRESULT hr = proveQueuedWorkCompletedLocked(
            device->state,
            "ID3D12CommandQueue::Signal(executed recovery drain)");
        if (FAILED(hr)) {
            return hr;
        }

        if (sharedFence->lastNativeSignal >= signalValue) {
            return waitForFence(sharedFence->fence.get(),
                                sharedFence->waitEvent.get(),
                                sharedFence->waitMutex, signalValue,
                                WAIT_INFINITE, device->state->device.get());
        }

        hr = signalCpuSharedFence(sharedFence->fence.get(), signalValue);
        if (FAILED(hr)) {
            setHresultError("ID3D12Fence::Signal(executed recovery)", hr,
                            device->state->device.get());
            return hr;
        }
        sharedFence->lastNativeSignal = signalValue;
        return S_OK;
    }
} // namespace sr::d3d12
