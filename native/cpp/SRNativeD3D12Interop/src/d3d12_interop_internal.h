#pragma once

#if defined(ON_WIN64)

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>

#include <d3d12.h>
#include <dxgi1_6.h>
#include <wrl/client.h>

#include <array>
#include <cstdint>
#include <memory>
#include <optional>
#include <string>
#include <utility>

namespace sr::d3d12 {
    using Microsoft::WRL::ComPtr;

    // Slot count and order of D3D12InteropContext::resources. The order mirrors
    // the RESOURCE_* constants on the Java side.
    constexpr uint32_t RESOURCE_COUNT = 5;

    /**
     * Minimal move-only RAII wrapper for a Win32 HANDLE.
     *
     * This provides the ownership behavior needed here without adding a
     * dependency on wil::unique_handle. A non-null handle is always closed when
     * the wrapper is destroyed or reset.
     */
    class UniqueHandle {
    public:
        UniqueHandle() = default;

        explicit UniqueHandle(HANDLE handle) : handle_(handle) {
        }

        ~UniqueHandle() { reset(); }

        UniqueHandle(const UniqueHandle &) = delete;

        UniqueHandle &operator=(const UniqueHandle &) = delete;

        UniqueHandle(UniqueHandle &&other) noexcept : handle_(other.release()) {
        }

        UniqueHandle &operator=(UniqueHandle &&other) noexcept {
            reset(other.release());
            return *this;
        }

        HANDLE get() const { return handle_; }
        explicit operator bool() const { return handle_ != nullptr; }

        // Releases the current handle and returns storage suitable for Win32
        // out-parameters such as ID3D12Device::CreateSharedHandle.
        HANDLE *put() {
            reset();
            return &handle_;
        }

        HANDLE release() {
            HANDLE handle = handle_;
            handle_ = nullptr;
            return handle;
        }

        void reset(HANDLE handle = nullptr) {
            if (handle_) {
                CloseHandle(handle_);
            }
            handle_ = handle;
        }

    private:
        HANDLE handle_ = nullptr;
    };

    /**
     * Owning COM pointer that is non-null by construction.
     *
     * Instances can only be obtained through from(), which rejects an empty
     * ComPtr. Consequently, a fully constructed interop context can dereference
     * all of its COM members without repeating null checks.
     */
    template<typename T>
    class ComObj {
    public:
        ComObj(const ComObj &) = delete;

        ComObj &operator=(const ComObj &) = delete;

        ComObj(ComObj &&) noexcept = default;

        ComObj &operator=(ComObj &&) noexcept = default;

        static std::optional<ComObj> from(ComPtr<T> ptr) {
            if (!ptr) {
                return std::nullopt;
            }
            return ComObj(std::move(ptr));
        }

        T *get() const { return ptr_.Get(); }
        T *operator->() const { return ptr_.Get(); }

        // Returns another owning COM reference, including the corresponding AddRef.
        ComPtr<T> share() const { return ptr_; }

    private:
        explicit ComObj(ComPtr<T> ptr) : ptr_(std::move(ptr)) {
        }

        ComPtr<T> ptr_;
    };

    // Command-list lifecycle across a frame. Recording begins only after a
    // successful beginFrame() and ends when executeFrame() closes the list.
    enum class FrameState { Idle, Recording };

    /**
     * A completely initialized shared texture.
     *
     * Keeping the resource, exported NT handle, and allocation size together
     * prevents callers from observing a partially created interop texture.
     */
    struct SharedTexture {
        ComObj<ID3D12Resource> resource;
        UniqueHandle sharedHandle;
        uint64_t allocationSize = 0;
    };

    /**
     * All D3D12 objects and synchronization state owned by one Java context.
     *
     * Every owning member is RAII-managed. Construction occurs only after every
     * required D3D12 object and shared handle has been created successfully.
     */
    struct D3D12InteropContext {
        D3D12InteropContext(ComObj<IDXGIAdapter1> adapter,
                            ComObj<ID3D12Device> device,
                            ComObj<ID3D12CommandQueue> queue,
                            ComObj<ID3D12CommandAllocator> commandAllocator,
                            ComObj<ID3D12GraphicsCommandList> commandList,
                            ComObj<ID3D12Fence> fence, UniqueHandle fenceSharedHandle,
                            UniqueHandle fenceEvent,
                            std::array<SharedTexture, RESOURCE_COUNT> resources)
            : adapter(std::move(adapter)), device(std::move(device)),
              queue(std::move(queue)), commandAllocator(std::move(commandAllocator)),
              commandList(std::move(commandList)), fence(std::move(fence)),
              fenceSharedHandle(std::move(fenceSharedHandle)),
              fenceEvent(std::move(fenceEvent)), resources(std::move(resources)) {
        }

        ComObj<IDXGIAdapter1> adapter;
        ComObj<ID3D12Device> device;
        ComObj<ID3D12CommandQueue> queue;
        ComObj<ID3D12CommandAllocator> commandAllocator;
        ComObj<ID3D12GraphicsCommandList> commandList;
        ComObj<ID3D12Fence> fence;
        UniqueHandle fenceSharedHandle;
        UniqueHandle fenceEvent;
        uint64_t lastSubmittedFenceValue = 0;
        FrameState frameState = FrameState::Idle;
        std::array<SharedTexture, RESOURCE_COUNT> resources;
    };

    // Thread-local diagnostics exposed to Java by Nd3d12GetLastError.
    void clearError();

    void setError(const char *message);

    void setHresultError(const char *operation, HRESULT hr);

    const std::string &lastError();

    /**
     * Runs a COM creation operation and converts its nullable out-parameter into
     * a checked ComObj. On failure, the operation and HRESULT are recorded in the
     * thread-local error string.
     */
    template<typename T, typename Create>
    std::optional<ComObj<T> > createCom(const char *operation, Create &&create) {
        ComPtr<T> ptr;
        const HRESULT hr = create(ptr.ReleaseAndGetAddressOf());
        if (FAILED(hr)) {
            setHresultError(operation, hr);
            return std::nullopt;
        }
        return ComObj<T>::from(std::move(ptr));
    }

    /**
     * Creates the adapter-bound device, command objects, shared fence, and all
     * five shared textures. Returns null on failure after recording an error.
     */
    std::unique_ptr<D3D12InteropContext>
    createContext(uint64_t adapterLuid, uint32_t renderWidth, uint32_t renderHeight,
                  uint32_t outputWidth, uint32_t outputHeight, int colorFormat);

    // Native frame operations used by the thin JNI boundary.
    bool waitForFence(D3D12InteropContext *context, uint64_t value);

    HRESULT beginFrame(D3D12InteropContext *context, uint64_t waitFenceValue);

    HRESULT executeFrame(D3D12InteropContext *context, uint64_t signalFenceValue);

    HRESULT waitIdle(D3D12InteropContext *context);
} // namespace sr::d3d12

#endif