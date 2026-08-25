#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>

#include <d3d12.h>
#include <dxgi1_6.h>
#include <wrl/client.h>

#include "sr/d3d12/d3d12_runtime.h"
#include "sr/sr_api_types.h"

#include <array>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <limits>
#include <memory>
#include <string>
#include <string_view>

namespace {
    using Microsoft::WRL::ComPtr;

    uint64_t packLuid(const LUID &luid) {
        return static_cast<uint64_t>(luid.LowPart) |
               (static_cast<uint64_t>(static_cast<uint32_t>(luid.HighPart))
                << 32);
    }

    uint64_t findHardwareAdapterLuid() {
        ComPtr<IDXGIFactory6> factory;
        if (FAILED(CreateDXGIFactory1(IID_PPV_ARGS(&factory)))) {
            return 0;
        }

        for (UINT index = 0;; ++index) {
            ComPtr<IDXGIAdapter1> adapter;
            const HRESULT enumerate = factory->EnumAdapters1(index, &adapter);
            if (enumerate == DXGI_ERROR_NOT_FOUND) {
                break;
            }
            if (FAILED(enumerate)) {
                return 0;
            }

            DXGI_ADAPTER_DESC1 description = {};
            if (FAILED(adapter->GetDesc1(&description)) ||
                (description.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) != 0) {
                continue;
            }

            ComPtr<ID3D12Device> probe;
            if (SUCCEEDED(D3D12CreateDevice(
                    adapter.Get(), D3D_FEATURE_LEVEL_12_0,
                    IID_PPV_ARGS(&probe)))) {
                return packLuid(description.AdapterLuid);
            }
        }
        return 0;
    }

    bool check(HRESULT result, const char *operation) {
        if (SUCCEEDED(result)) {
            return true;
        }
        std::fprintf(stderr, "%s failed: %s\n", operation,
                     sr::d3d12::lastError());
        return false;
    }

    bool checkRejected(HRESULT result, const char *operation) {
        if (FAILED(result)) {
            return true;
        }
        std::fprintf(stderr, "%s unexpectedly succeeded.\n", operation);
        return false;
    }

    template<typename T, auto Destroy>
    struct Destroyer {
        void operator()(T *object) const noexcept {
            static_cast<void>(Destroy(object));
        }
    };

    template<typename T, auto Destroy>
    using Owned = std::unique_ptr<T, Destroyer<T, Destroy>>;
} // namespace

int main() {
    const uint64_t adapterLuid = findHardwareAdapterLuid();
    if (adapterLuid == 0) {
        std::fprintf(stderr, "No hardware adapter supports D3D feature level 12_0.\n");
        return EXIT_FAILURE;
    }

    uint32_t debugFlags = sr::d3d12::DEBUG_NONE;
    if (GetEnvironmentVariableW(L"SR_D3D12_SMOKE_DEBUG", nullptr, 0) != 0) {
        debugFlags = sr::d3d12::DEBUG_LAYER | sr::d3d12::DEBUG_GPU_VALIDATION |
                     sr::d3d12::DEBUG_DRED;
    }

    Owned<sr::d3d12::Device, sr::d3d12::destroyDevice> device(
        sr::d3d12::createDevice(adapterLuid, debugFlags));
    if (!device) {
        std::fprintf(stderr, "createDevice failed: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }
    if (!sr::d3d12::nativeDevice(device.get()) ||
        !sr::d3d12::nativeQueue(device.get()) ||
        sr::d3d12::deviceAdapterLuid(device.get()) != adapterLuid) {
        std::fprintf(stderr, "Device queries failed: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::SharedFence, sr::d3d12::destroySharedFence> fence(
        sr::d3d12::createSharedFence(device.get(), 0));
    if (!fence || !sr::d3d12::nativeFence(fence.get()) ||
        !sr::d3d12::sharedFenceHandle(fence.get())) {
        std::fprintf(stderr, "Shared fence creation failed: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::SharedFence, sr::d3d12::destroySharedFence> overflowFence(
        sr::d3d12::createSharedFence(
            device.get(),
            static_cast<uint64_t>(std::numeric_limits<int64_t>::max()) - 1));
    if (!overflowFence ||
        sr::d3d12::reserveSharedFenceValue(overflowFence.get()) !=
            static_cast<uint64_t>(std::numeric_limits<int64_t>::max()) ||
        sr::d3d12::reserveSharedFenceValue(overflowFence.get()) != 0) {
        std::fprintf(stderr, "Signed shared-fence exhaustion check failed: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::Texture2D, sr::d3d12::destroyTexture2D> texture(
        sr::d3d12::createTexture2D(
            device.get(), 4, 4, 1, SR_TEXTURE_FORMAT_R8G8B8A8_UNORM,
            D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET |
                D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS,
            sr::d3d12::ResourceState::Common, true),
        Destroyer<sr::d3d12::Texture2D, sr::d3d12::destroyTexture2D>{});
    if (!texture || !sr::d3d12::nativeTextureResource(texture.get()) ||
        !sr::d3d12::textureSharedHandle(texture.get()) ||
        sr::d3d12::textureAllocationSize(texture.get()) == 0) {
        std::fprintf(stderr, "Texture creation failed: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::CommandAllocator,
          sr::d3d12::destroyCommandAllocator>
        allocator(sr::d3d12::createCommandAllocator(device.get()),
                  Destroyer<sr::d3d12::CommandAllocator,
                            sr::d3d12::destroyCommandAllocator>{});
    Owned<sr::d3d12::CommandList, sr::d3d12::destroyCommandList> commandList(
        allocator
            ? sr::d3d12::createCommandList(device.get(), allocator.get())
            : nullptr,
        Destroyer<sr::d3d12::CommandList,
                  sr::d3d12::destroyCommandList>{});
    if (!allocator || !commandList) {
        std::fprintf(stderr, "Command object creation failed: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    if (!check(sr::d3d12::beginCommandList(commandList.get()), "begin") ||
        !sr::d3d12::checkedNativeCommandList(commandList.get()) ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), texture.get(),
                   sr::d3d12::ResourceState::Common,
                   sr::d3d12::ResourceState::RenderTarget),
               "transition to render target") ||
        !check(sr::d3d12::clearTextureRgba(commandList.get(), texture.get(),
                                           0.125f, 0.25f, 0.5f, 1.0f),
               "clear") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), texture.get(),
                   sr::d3d12::ResourceState::RenderTarget,
                   sr::d3d12::ResourceState::Common),
               "transition to common") ||
        !check(sr::d3d12::endCommandList(commandList.get()), "end")) {
        return EXIT_FAILURE;
    }

    const uint64_t waitValue = sr::d3d12::reserveSharedFenceValue(fence.get());
    const uint64_t signalValue = sr::d3d12::reserveSharedFenceValue(fence.get());
    if (waitValue == 0 || signalValue == 0 ||
        !check(sr::d3d12::signalSharedFence(fence.get(), waitValue),
               "producer signal") ||
        !check(sr::d3d12::submit(device.get(), commandList.get(), fence.get(),
                                 waitValue, signalValue),
               "submit") ||
        !check(sr::d3d12::waitSharedFence(fence.get(), signalValue,
                                          sr::d3d12::WAIT_INFINITE),
               "shared fence wait") ||
        !check(sr::d3d12::waitIdle(device.get(), sr::d3d12::WAIT_INFINITE),
               "device wait")) {
        return EXIT_FAILURE;
    }

    if (sr::d3d12::textureCommittedState(texture.get()) !=
        sr::d3d12::ResourceState::Common) {
        std::fprintf(stderr, "Committed texture state was not updated.\n");
        return EXIT_FAILURE;
    }

    if (!check(sr::d3d12::beginCommandList(commandList.get()), "begin abort") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), texture.get(),
                   sr::d3d12::ResourceState::Common,
                   sr::d3d12::ResourceState::CopyDestination),
               "record abort transition") ||
        !check(sr::d3d12::abortCommandList(commandList.get()), "abort") ||
        sr::d3d12::textureCommittedState(texture.get()) !=
            sr::d3d12::ResourceState::Common) {
        std::fprintf(stderr, "Abort did not preserve committed texture state.\n");
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::Texture2D, sr::d3d12::destroyTexture2D> retainedTexture(
        sr::d3d12::createTexture2D(
            device.get(), 4, 4, 1, SR_TEXTURE_FORMAT_R8G8B8A8_UNORM,
            D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET,
            sr::d3d12::ResourceState::Common, false),
        Destroyer<sr::d3d12::Texture2D, sr::d3d12::destroyTexture2D>{});
    if (!retainedTexture ||
        !check(sr::d3d12::beginCommandList(commandList.get()),
               "begin retained-resource submission") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), retainedTexture.get(),
                   sr::d3d12::ResourceState::Common,
                   sr::d3d12::ResourceState::RenderTarget),
               "transition retained resource") ||
        !check(sr::d3d12::clearTextureRgba(
                   commandList.get(), retainedTexture.get(), 0.0f, 0.0f, 0.0f,
                   1.0f),
               "clear retained resource") ||
        !check(sr::d3d12::endCommandList(commandList.get()),
               "end retained-resource submission")) {
        return EXIT_FAILURE;
    }

    // The wrapper may be released once recording is complete. The command list
    // must keep the resource and its RTV heap alive through GPU completion.
    retainedTexture.reset();
    if (!check(sr::d3d12::submit(device.get(), commandList.get(), nullptr, 0, 0),
               "submit retained resource") ||
        !check(sr::d3d12::waitIdle(device.get(), sr::d3d12::WAIT_INFINITE),
               "wait retained resource")) {
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::Texture2D, sr::d3d12::destroyTexture2D> copyDestination(
        sr::d3d12::createTexture2D(
            device.get(), 4, 4, 1, SR_TEXTURE_FORMAT_R8G8B8A8_UNORM,
            D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET,
            sr::d3d12::ResourceState::CopyDestination, false));
    Owned<sr::d3d12::Buffer, sr::d3d12::destroyBuffer> uploadBuffer(
        sr::d3d12::createBuffer(device.get(), 16,
                                sr::d3d12::BufferHeap::Upload, 0,
                                sr::d3d12::ResourceState::CopySource, false));
    Owned<sr::d3d12::Buffer, sr::d3d12::destroyBuffer> defaultBuffer(
        sr::d3d12::createBuffer(device.get(), 16,
                                sr::d3d12::BufferHeap::Default, 0,
                                sr::d3d12::ResourceState::CopyDestination,
                                false));
    Owned<sr::d3d12::Buffer, sr::d3d12::destroyBuffer> wrongSourceHeap(
        sr::d3d12::createBuffer(device.get(), 16,
                                sr::d3d12::BufferHeap::Default, 0,
                                sr::d3d12::ResourceState::CopySource, false));
    Owned<sr::d3d12::Buffer, sr::d3d12::destroyBuffer> wrongSourceState(
        sr::d3d12::createBuffer(device.get(), 16,
                                sr::d3d12::BufferHeap::Upload, 0,
                                sr::d3d12::ResourceState::Common, false));
    Owned<sr::d3d12::Buffer, sr::d3d12::destroyBuffer> wrongDestinationHeap(
        sr::d3d12::createBuffer(device.get(), 16,
                                sr::d3d12::BufferHeap::Readback, 0,
                                sr::d3d12::ResourceState::CopyDestination,
                                false));
    Owned<sr::d3d12::Buffer, sr::d3d12::destroyBuffer> wrongDestinationState(
        sr::d3d12::createBuffer(device.get(), 16,
                                sr::d3d12::BufferHeap::Default, 0,
                                sr::d3d12::ResourceState::Common, false));
    if (!copyDestination || !uploadBuffer || !defaultBuffer ||
        !wrongSourceHeap || !wrongSourceState || !wrongDestinationHeap ||
        !wrongDestinationState) {
        std::fprintf(stderr, "Copy/write resource creation failed: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    std::array<std::byte, 64> textureBytes = {};
    for (size_t index = 0; index < textureBytes.size(); ++index) {
        textureBytes[index] = static_cast<std::byte>(index);
    }
    const std::array<uint32_t, 4> bufferWords = {
        0x11223344u, 0x55667788u, 0x99AABBCCu, 0xDDEEFF00u};
    const sr::d3d12::TextureWriteRegion writeRegion = {0, 0, 4, 4, 0, 0};
    const sr::d3d12::TextureCopyRegion copyRegion = {0, 0, 4, 4, 0, 0, 0, 0};
    void *mappedUpload = nullptr;
    void *duplicateMap = nullptr;
    if (!check(sr::d3d12::mapBuffer(
                   uploadBuffer.get(), 0, sizeof(bufferWords), &mappedUpload),
               "map upload buffer") ||
        !mappedUpload) {
        std::fprintf(stderr, "UPLOAD map returned no data: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }
    std::memcpy(mappedUpload, bufferWords.data(), sizeof(bufferWords));
    if (!checkRejected(sr::d3d12::mapBuffer(
                           uploadBuffer.get(), 0, sizeof(bufferWords),
                           &duplicateMap),
                       "map an already mapped upload buffer") ||
        !checkRejected(sr::d3d12::mapBuffer(
                           defaultBuffer.get(), 0, sizeof(bufferWords),
                           &duplicateMap),
                       "map a DEFAULT buffer directly") ||
        !check(sr::d3d12::unmapBuffer(
                   uploadBuffer.get(), 0, sizeof(bufferWords)),
               "unmap upload buffer") ||
        !checkRejected(sr::d3d12::unmapBuffer(
                           uploadBuffer.get(), 0, sizeof(bufferWords)),
                       "unmap an unmapped upload buffer") ||
        !check(sr::d3d12::beginCommandList(commandList.get()), "begin copy/write") ||
        !checkRejected(sr::d3d12::copyBuffer(
                           commandList.get(), wrongSourceHeap.get(),
                           defaultBuffer.get(), 0, 0, sizeof(bufferWords)),
                       "copy buffer with DEFAULT source heap") ||
        !checkRejected(sr::d3d12::copyBuffer(
                           commandList.get(), wrongSourceState.get(),
                           defaultBuffer.get(), 0, 0, sizeof(bufferWords)),
                       "copy buffer with non-COPY_SOURCE source state") ||
        !checkRejected(sr::d3d12::copyBuffer(
                           commandList.get(), uploadBuffer.get(),
                           wrongDestinationHeap.get(), 0, 0,
                           sizeof(bufferWords)),
                       "copy buffer with non-DEFAULT destination heap") ||
        !checkRejected(sr::d3d12::copyBuffer(
                           commandList.get(), uploadBuffer.get(),
                           wrongDestinationState.get(), 0, 0,
                           sizeof(bufferWords)),
                       "copy buffer with non-COPY_DESTINATION destination state") ||
        !checkRejected(sr::d3d12::writeBuffer(
                           commandList.get(), wrongDestinationState.get(), 0,
                           bufferWords.data(), sizeof(bufferWords)),
                       "write DEFAULT buffer with non-COPY_DESTINATION state") ||
        !check(sr::d3d12::writeBuffer(
                   commandList.get(), uploadBuffer.get(), 0,
                   bufferWords.data(), sizeof(bufferWords)),
               "write upload buffer") ||
        !check(sr::d3d12::copyBuffer(
                   commandList.get(), uploadBuffer.get(), defaultBuffer.get(),
                   0, 0, sizeof(bufferWords)),
               "copy buffer") ||
        !check(sr::d3d12::writeBuffer(
                   commandList.get(), defaultBuffer.get(), 8,
                   bufferWords.data(), sizeof(uint32_t) * 2),
               "write default buffer") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), texture.get(),
                   sr::d3d12::ResourceState::Common,
                   sr::d3d12::ResourceState::CopyDestination),
               "transition upload texture") ||
        !check(sr::d3d12::writeTexture(
                   commandList.get(), texture.get(), writeRegion,
                   textureBytes.data(), textureBytes.size()),
               "write texture") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), texture.get(),
                   sr::d3d12::ResourceState::CopyDestination,
                   sr::d3d12::ResourceState::CopySource),
               "transition copy source") ||
        !check(sr::d3d12::copyTexture(commandList.get(), texture.get(),
                                      copyDestination.get(), copyRegion),
               "copy texture") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), texture.get(),
                   sr::d3d12::ResourceState::CopySource,
                   sr::d3d12::ResourceState::UnorderedAccess),
               "transition UAV") ||
        !check(sr::d3d12::uavBarrier(commandList.get(), texture.get()),
               "UAV barrier") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), texture.get(),
                   sr::d3d12::ResourceState::UnorderedAccess,
                   sr::d3d12::ResourceState::Common),
               "transition source common") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), copyDestination.get(),
                   sr::d3d12::ResourceState::CopyDestination,
                   sr::d3d12::ResourceState::RenderTarget),
               "transition destination render target") ||
        !check(sr::d3d12::clearTextureRgba(
                   commandList.get(), copyDestination.get(), 0.0f, 0.0f, 0.0f,
                   1.0f),
               "clear copy destination") ||
        !check(sr::d3d12::transitionTexture(
                   commandList.get(), copyDestination.get(),
                   sr::d3d12::ResourceState::RenderTarget,
                   sr::d3d12::ResourceState::Common),
               "transition destination common") ||
        !check(sr::d3d12::endCommandList(commandList.get()), "end copy/write") ||
        !check(sr::d3d12::submit(device.get(), commandList.get(), nullptr, 0, 0),
               "submit copy/write") ||
        !check(sr::d3d12::waitIdle(device.get(), sr::d3d12::WAIT_INFINITE),
               "wait copy/write")) {
        return EXIT_FAILURE;
    }

    if (sr::d3d12::textureCommittedState(texture.get()) !=
            sr::d3d12::ResourceState::Common ||
        sr::d3d12::textureCommittedState(copyDestination.get()) !=
            sr::d3d12::ResourceState::Common) {
        std::fprintf(stderr, "Copy/write texture states were not committed.\n");
        return EXIT_FAILURE;
    }

    const uint64_t recoveryWait =
        sr::d3d12::reserveSharedFenceValue(fence.get());
    const uint64_t recoverySignal =
        sr::d3d12::reserveSharedFenceValue(fence.get());
    if (!check(sr::d3d12::signalSharedFence(fence.get(), recoveryWait),
               "recovery producer signal") ||
        !check(sr::d3d12::recoverSharedFence(
                   device.get(), fence.get(), recoveryWait, recoverySignal),
               "empty recovery") ||
        sr::d3d12::completedSharedFenceValue(fence.get()) < recoverySignal) {
        std::fprintf(stderr, "Empty wait-signal recovery failed: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

#if defined(SR_D3D12_TEST_HOOKS)
    const uint64_t retryRecoveryWait =
        sr::d3d12::reserveSharedFenceValue(fence.get());
    const uint64_t retryRecoverySignal =
        sr::d3d12::reserveSharedFenceValue(fence.get());
    if (retryRecoveryWait == 0 || retryRecoverySignal == 0 ||
        !check(sr::d3d12::signalSharedFence(fence.get(), retryRecoveryWait),
               "retry recovery producer signal")) {
        return EXIT_FAILURE;
    }
    sr::d3d12::testing::failNextFenceWait(E_FAIL);
    const HRESULT interruptedRecovery = sr::d3d12::recoverSharedFence(
        device.get(), fence.get(), retryRecoveryWait, retryRecoverySignal);
    const std::string interruptedRecoveryDiagnostic = sr::d3d12::lastError();
    if (SUCCEEDED(interruptedRecovery) ||
        std::string_view(interruptedRecoveryDiagnostic).find(
            "Injected D3D12 fence wait") == std::string_view::npos ||
        !check(sr::d3d12::recoverSharedFence(
                   device.get(), fence.get(), retryRecoveryWait,
                   retryRecoverySignal),
               "retry empty recovery after wait failure") ||
        sr::d3d12::completedSharedFenceValue(fence.get()) <
            retryRecoverySignal) {
        std::fprintf(stderr,
                     "Empty recovery was not wait-only retryable: %s\n",
                     interruptedRecoveryDiagnostic.c_str());
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::Buffer, sr::d3d12::destroyBuffer> destroyRetryBuffer(
        sr::d3d12::createBuffer(device.get(), 16,
                                sr::d3d12::BufferHeap::Default, 0,
                                sr::d3d12::ResourceState::CopyDestination,
                                false));
    Owned<sr::d3d12::CommandAllocator,
          sr::d3d12::destroyCommandAllocator>
        destroyRetryAllocator(
            sr::d3d12::createCommandAllocator(device.get()));
    sr::d3d12::CommandList *destroyRetryCommandList =
        destroyRetryAllocator
            ? sr::d3d12::createCommandList(
                  device.get(), destroyRetryAllocator.get())
            : nullptr;
    const std::array<std::byte, 16> destroyRetryPayload{};
    sr::d3d12::SubmissionDisposition destroyRetryDisposition =
        sr::d3d12::SubmissionDisposition::NotExecuted;
    if (!destroyRetryBuffer || !destroyRetryAllocator ||
        !destroyRetryCommandList ||
        !check(sr::d3d12::beginCommandList(destroyRetryCommandList),
               "begin destroy-retry submission") ||
        !check(sr::d3d12::writeBuffer(
                   destroyRetryCommandList, destroyRetryBuffer.get(), 0,
                   destroyRetryPayload.data(), destroyRetryPayload.size()),
               "record retained upload for destroy retry") ||
        !check(sr::d3d12::endCommandList(destroyRetryCommandList),
               "end destroy-retry submission") ||
        !check(sr::d3d12::submit(
                   device.get(), destroyRetryCommandList, nullptr, 0, 0,
                   &destroyRetryDisposition),
               "submit destroy-retry command list") ||
        destroyRetryDisposition !=
            sr::d3d12::SubmissionDisposition::Submitted) {
        return EXIT_FAILURE;
    }
    sr::d3d12::testing::failNextFenceWait(E_FAIL);
    const HRESULT interruptedDestroy =
        sr::d3d12::destroyCommandList(destroyRetryCommandList);
    const std::string interruptedDestroyDiagnostic = sr::d3d12::lastError();
    if (SUCCEEDED(interruptedDestroy) ||
        std::string_view(interruptedDestroyDiagnostic).find(
            "Injected D3D12 fence wait") == std::string_view::npos ||
        !check(sr::d3d12::destroyCommandList(destroyRetryCommandList),
               "retry command-list destroy after wait failure")) {
        std::fprintf(stderr,
                     "Submitted command-list destroy was not retryable: %s\n",
                     interruptedDestroyDiagnostic.c_str());
        return EXIT_FAILURE;
    }
    destroyRetryCommandList = nullptr;

    Owned<sr::d3d12::CommandAllocator,
          sr::d3d12::destroyCommandAllocator>
        failedAllocator(sr::d3d12::createCommandAllocator(device.get()));
    Owned<sr::d3d12::CommandList, sr::d3d12::destroyCommandList>
        failedCommandList(
            failedAllocator
                ? sr::d3d12::createCommandList(device.get(),
                                               failedAllocator.get())
                : nullptr);
    Owned<sr::d3d12::Texture2D, sr::d3d12::destroyTexture2D> failedTexture(
        sr::d3d12::createTexture2D(
            device.get(), 4, 4, 1, SR_TEXTURE_FORMAT_R8G8B8A8_UNORM,
            D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET,
            sr::d3d12::ResourceState::Common, false));
    if (!failedAllocator || !failedCommandList || !failedTexture ||
        !check(sr::d3d12::beginCommandList(failedCommandList.get()),
               "begin injected-signal-failure submission") ||
        !check(sr::d3d12::transitionTexture(
                   failedCommandList.get(), failedTexture.get(),
                   sr::d3d12::ResourceState::Common,
                   sr::d3d12::ResourceState::RenderTarget),
               "transition injected-signal-failure resource") ||
        !check(sr::d3d12::clearTextureRgba(
                   failedCommandList.get(), failedTexture.get(), 0.0f, 0.0f,
                   0.0f, 1.0f),
               "clear injected-signal-failure resource") ||
        !check(sr::d3d12::transitionTexture(
                   failedCommandList.get(), failedTexture.get(),
                   sr::d3d12::ResourceState::RenderTarget,
                   sr::d3d12::ResourceState::Common),
               "restore injected-signal-failure resource") ||
        !check(sr::d3d12::endCommandList(failedCommandList.get()),
               "end injected-signal-failure submission")) {
        return EXIT_FAILURE;
    }

    sr::d3d12::testing::failNextInternalCompletionSignal(E_FAIL);
    sr::d3d12::SubmissionDisposition failedDisposition =
        sr::d3d12::SubmissionDisposition::NotExecuted;
    const HRESULT injectedFailure = sr::d3d12::submit(
        device.get(), failedCommandList.get(), nullptr, 0, 0,
        &failedDisposition);
    if (SUCCEEDED(injectedFailure) ||
        failedDisposition !=
            sr::d3d12::SubmissionDisposition::ExecutedUntracked ||
        std::string_view(sr::d3d12::lastError()).find(
            "Signal(internal completion)") == std::string_view::npos ||
        sr::d3d12::testing::quarantinedSubmissionCount(device.get()) != 1) {
        std::fprintf(stderr,
                     "Injected completion-signal failure was not quarantined: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    // Model Java releasing every wrapper immediately after submit reports the
    // error. The queued native objects must remain quarantined until a later
    // fence can prove completion.
    failedTexture.reset();
    failedCommandList.reset();
    failedAllocator.reset();
    if (sr::d3d12::testing::quarantinedSubmissionCount(device.get()) != 1 ||
        !check(sr::d3d12::waitIdle(device.get(), sr::d3d12::WAIT_INFINITE),
               "drain injected-signal-failure quarantine") ||
        sr::d3d12::testing::quarantinedSubmissionCount(device.get()) != 0) {
        std::fprintf(stderr,
                     "Injected completion-signal quarantine did not drain: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::CommandAllocator,
          sr::d3d12::destroyCommandAllocator>
        recoveredAllocator(sr::d3d12::createCommandAllocator(device.get()));
    Owned<sr::d3d12::CommandList, sr::d3d12::destroyCommandList>
        recoveredCommandList(
            recoveredAllocator
                ? sr::d3d12::createCommandList(device.get(),
                                               recoveredAllocator.get())
                : nullptr);
    const uint64_t recoveredSignal =
        sr::d3d12::reserveSharedFenceValue(fence.get());
    if (!recoveredAllocator || !recoveredCommandList || recoveredSignal == 0 ||
        !check(sr::d3d12::beginCommandList(recoveredCommandList.get()),
               "begin shared recovery submission") ||
        !check(sr::d3d12::endCommandList(recoveredCommandList.get()),
               "end shared recovery submission")) {
        return EXIT_FAILURE;
    }

    sr::d3d12::testing::failNextInternalCompletionSignal(E_FAIL);
    sr::d3d12::SubmissionDisposition recoveredDisposition =
        sr::d3d12::SubmissionDisposition::NotExecuted;
    if (!check(sr::d3d12::submit(device.get(), recoveredCommandList.get(),
                                 fence.get(), 0, recoveredSignal,
                                 &recoveredDisposition),
               "submit with shared completion recovery") ||
        recoveredDisposition != sr::d3d12::SubmissionDisposition::Submitted ||
        sr::d3d12::lastError()[0] != '\0' ||
        sr::d3d12::testing::quarantinedSubmissionCount(device.get()) != 0 ||
        !check(sr::d3d12::beginCommandList(recoveredCommandList.get()),
               "reuse command list after shared recovery") ||
        !check(sr::d3d12::abortCommandList(recoveredCommandList.get()),
               "abort reused command list after shared recovery")) {
        std::fprintf(stderr, "Shared completion recovery left quarantine: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::CommandAllocator,
          sr::d3d12::destroyCommandAllocator>
        untrackedAllocator(sr::d3d12::createCommandAllocator(device.get()));
    Owned<sr::d3d12::CommandList, sr::d3d12::destroyCommandList>
        untrackedCommandList(
            untrackedAllocator
                ? sr::d3d12::createCommandList(device.get(),
                                               untrackedAllocator.get())
                : nullptr);
    const uint64_t untrackedSignal =
        sr::d3d12::reserveSharedFenceValue(fence.get());
    if (!untrackedAllocator || !untrackedCommandList || untrackedSignal == 0 ||
        !check(sr::d3d12::beginCommandList(untrackedCommandList.get()),
               "begin untracked shared recovery submission") ||
        !check(sr::d3d12::endCommandList(untrackedCommandList.get()),
               "end untracked shared recovery submission")) {
        return EXIT_FAILURE;
    }

    sr::d3d12::testing::failNextInternalCompletionSignal(E_FAIL);
    sr::d3d12::testing::failNextQueueSharedFenceSignal(E_FAIL);
    sr::d3d12::SubmissionDisposition untrackedDisposition =
        sr::d3d12::SubmissionDisposition::NotExecuted;
    const HRESULT untrackedFailure = sr::d3d12::submit(
        device.get(), untrackedCommandList.get(), fence.get(), 0,
        untrackedSignal, &untrackedDisposition);
    if (SUCCEEDED(untrackedFailure) ||
        untrackedDisposition !=
            sr::d3d12::SubmissionDisposition::ExecutedUntracked ||
        sr::d3d12::testing::quarantinedSubmissionCount(device.get()) != 1 ||
        sr::d3d12::completedSharedFenceValue(fence.get()) >= untrackedSignal) {
        std::fprintf(stderr,
                     "Could not create an untracked shared submission: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    // Executed recovery must not use the empty-path producer-only CPU
    // fallback. If the queue drain cannot be established, ownership remains
    // quarantined and the consumer fence value must stay incomplete.
    sr::d3d12::testing::failNextInternalCompletionSignal(E_FAIL);
    sr::d3d12::testing::failNextQueueSharedFenceSignal(E_FAIL);
    const HRESULT blockedRecovery = sr::d3d12::recoverSharedFence(
        device.get(), fence.get(), 0, untrackedSignal);
    const std::string blockedRecoveryDiagnostic = sr::d3d12::lastError();
    if (SUCCEEDED(blockedRecovery) ||
        sr::d3d12::completedSharedFenceValue(fence.get()) >= untrackedSignal ||
        sr::d3d12::testing::quarantinedSubmissionCount(device.get()) != 1 ||
        std::string_view(blockedRecoveryDiagnostic).find(
            "Signal(shared recovery drain)") == std::string_view::npos) {
        std::fprintf(stderr,
                     "Executed recovery used an unsafe fallback: %s\n",
                     blockedRecoveryDiagnostic.c_str());
        return EXIT_FAILURE;
    }
    if (!check(sr::d3d12::recoverExecutedSharedFence(
                   device.get(), fence.get(), 0, untrackedSignal),
               "retry untracked shared recovery") ||
        sr::d3d12::completedSharedFenceValue(fence.get()) < untrackedSignal ||
        sr::d3d12::testing::quarantinedSubmissionCount(device.get()) != 0) {
        std::fprintf(stderr,
                     "Untracked shared recovery did not drain safely: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }

    Owned<sr::d3d12::CommandAllocator,
          sr::d3d12::destroyCommandAllocator>
        submittedAllocator(sr::d3d12::createCommandAllocator(device.get()));
    Owned<sr::d3d12::CommandList, sr::d3d12::destroyCommandList>
        submittedCommandList(
            submittedAllocator
                ? sr::d3d12::createCommandList(device.get(),
                                               submittedAllocator.get())
                : nullptr);
    const uint64_t submittedSignal =
        sr::d3d12::reserveSharedFenceValue(fence.get());
    if (!submittedAllocator || !submittedCommandList || submittedSignal == 0 ||
        !check(sr::d3d12::beginCommandList(submittedCommandList.get()),
               "begin tracked shared recovery submission") ||
        !check(sr::d3d12::endCommandList(submittedCommandList.get()),
               "end tracked shared recovery submission")) {
        return EXIT_FAILURE;
    }

    sr::d3d12::testing::failNextQueueSharedFenceSignal(E_FAIL);
    sr::d3d12::testing::failNextCpuSharedFenceSignal(E_FAIL);
    sr::d3d12::SubmissionDisposition submittedDisposition =
        sr::d3d12::SubmissionDisposition::NotExecuted;
    const HRESULT submittedFailure = sr::d3d12::submit(
        device.get(), submittedCommandList.get(), fence.get(), 0,
        submittedSignal, &submittedDisposition);
    if (SUCCEEDED(submittedFailure) ||
        submittedDisposition != sr::d3d12::SubmissionDisposition::Submitted ||
        sr::d3d12::completedSharedFenceValue(fence.get()) >= submittedSignal) {
        std::fprintf(stderr,
                     "Could not create a tracked shared-signal failure: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }
    if (!check(sr::d3d12::recoverExecutedSharedFence(
                   device.get(), fence.get(), 0, submittedSignal),
               "recover tracked shared submission") ||
        sr::d3d12::completedSharedFenceValue(fence.get()) < submittedSignal ||
        !check(sr::d3d12::beginCommandList(submittedCommandList.get()),
               "reuse tracked shared recovery command list") ||
        !check(sr::d3d12::abortCommandList(submittedCommandList.get()),
               "abort tracked shared recovery command list")) {
        std::fprintf(stderr,
                     "Tracked shared recovery did not complete safely: %s\n",
                     sr::d3d12::lastError());
        return EXIT_FAILURE;
    }
#endif

    std::printf("SRNativeD3D12 smoke passed (completion=%llu, shared=%llu).\n",
                static_cast<unsigned long long>(
                    sr::d3d12::completedSubmissionValue(device.get())),
                static_cast<unsigned long long>(
                    sr::d3d12::completedSharedFenceValue(fence.get())));
    return EXIT_SUCCESS;
}
