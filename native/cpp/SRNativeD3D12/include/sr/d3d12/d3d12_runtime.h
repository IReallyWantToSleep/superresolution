#pragma once

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>

#include <d3d12.h>

#include <cstddef>
#include <cstdint>

namespace sr::d3d12 {
    constexpr uint32_t WAIT_INFINITE = 0xFFFFFFFFu;

    enum DebugFlag : uint32_t {
        DEBUG_NONE = 0,
        DEBUG_LAYER = 1u << 0,
        DEBUG_GPU_VALIDATION = 1u << 1,
        DEBUG_DRED = 1u << 2,
    };

    // Stable Java/native state codes. Do not replace these with D3D12 values.
    enum class ResourceState : uint32_t {
        Common = 0,
        ComputeRead = 1,
        UnorderedAccess = 2,
        CopySource = 3,
        CopyDestination = 4,
        RenderTarget = 5,
        DepthWrite = 6,
        Present = 7,
    };

    enum class SubmissionDisposition : uint32_t {
        NotExecuted = 0,
        Submitted = 1,
        ExecutedUntracked = 2,
    };

    enum class BufferHeap : uint32_t {
        Default = 0,
        Upload = 1,
        Readback = 2,
    };

    struct Device;
    struct SharedFence;
    struct Texture2D;
    struct Buffer;
    struct CommandAllocator;
    struct CommandList;

    struct TextureCopyRegion {
        uint32_t sourceX;
        uint32_t sourceY;
        uint32_t width;
        uint32_t height;
        uint32_t sourceMip;
        uint32_t destinationX;
        uint32_t destinationY;
        uint32_t destinationMip;
    };

    struct TextureWriteRegion {
        uint32_t x;
        uint32_t y;
        uint32_t width;
        uint32_t height;
        uint32_t mip;
        uint32_t sourceRowPitch;
    };

    const char *lastError() noexcept;
    void setLastError(const char *message) noexcept;
    HRESULT setObjectName(void *object, const wchar_t *name) noexcept;

    Device *createDevice(uint64_t adapterLuid, uint32_t debugFlags) noexcept;
    void destroyDevice(Device *device) noexcept;
    ID3D12Device *nativeDevice(Device *device) noexcept;
    ID3D12CommandQueue *nativeQueue(Device *device) noexcept;
    uint64_t deviceAdapterLuid(Device *device) noexcept;
    uint64_t completedSubmissionValue(Device *device) noexcept;
    uint64_t lastSubmittedValue(Device *device) noexcept;
    HRESULT waitIdle(Device *device, uint32_t timeoutMilliseconds) noexcept;

    SharedFence *createSharedFence(Device *device, uint64_t initialValue) noexcept;
    void destroySharedFence(SharedFence *fence) noexcept;
    ID3D12Fence *nativeFence(SharedFence *fence) noexcept;
    HANDLE sharedFenceHandle(SharedFence *fence) noexcept;
    uint64_t reserveSharedFenceValue(SharedFence *fence) noexcept;
    uint64_t completedSharedFenceValue(SharedFence *fence) noexcept;
    HRESULT signalSharedFence(SharedFence *fence, uint64_t value) noexcept;
    HRESULT waitSharedFence(SharedFence *fence, uint64_t value,
                            uint32_t timeoutMilliseconds) noexcept;

    Texture2D *createTexture2D(Device *device, uint32_t width, uint32_t height,
                               uint16_t mipLevels, int32_t srSurfaceFormat,
                               uint32_t resourceFlags, ResourceState initialState,
                               bool shared) noexcept;
    void destroyTexture2D(Texture2D *texture) noexcept;
    ID3D12Resource *nativeTextureResource(Texture2D *texture) noexcept;
    HANDLE textureSharedHandle(Texture2D *texture) noexcept;
    uint64_t textureAllocationSize(Texture2D *texture) noexcept;
    uint32_t textureWidth(Texture2D *texture) noexcept;
    uint32_t textureHeight(Texture2D *texture) noexcept;
    uint16_t textureMipLevels(Texture2D *texture) noexcept;
    int32_t textureSurfaceFormat(Texture2D *texture) noexcept;
    uint32_t textureResourceFlags(Texture2D *texture) noexcept;
    ResourceState textureInitialState(Texture2D *texture) noexcept;
    ResourceState textureCommittedState(Texture2D *texture) noexcept;
    HRESULT setTextureCommittedState(Texture2D *texture,
                                     ResourceState state) noexcept;

    Buffer *createBuffer(Device *device, uint64_t size, BufferHeap heap,
                         uint32_t resourceFlags, ResourceState initialState,
                         bool shared) noexcept;
    void destroyBuffer(Buffer *buffer) noexcept;
    ID3D12Resource *nativeBufferResource(Buffer *buffer) noexcept;
    HANDLE bufferSharedHandle(Buffer *buffer) noexcept;
    uint64_t bufferSize(Buffer *buffer) noexcept;
    BufferHeap bufferHeap(Buffer *buffer) noexcept;
    uint32_t bufferResourceFlags(Buffer *buffer) noexcept;
    ResourceState bufferInitialState(Buffer *buffer) noexcept;
    HRESULT mapBuffer(Buffer *buffer, uint64_t offset, uint64_t size,
                      void **mappedData) noexcept;
    HRESULT unmapBuffer(Buffer *buffer, uint64_t offset,
                        uint64_t size) noexcept;

    CommandAllocator *createCommandAllocator(Device *device) noexcept;
    void destroyCommandAllocator(CommandAllocator *allocator) noexcept;
    ID3D12CommandAllocator *nativeCommandAllocator(CommandAllocator *allocator) noexcept;

    CommandList *createCommandList(Device *device,
                                   CommandAllocator *allocator) noexcept;
    HRESULT destroyCommandList(CommandList *commandList) noexcept;
    HRESULT beginCommandList(CommandList *commandList) noexcept;
    HRESULT endCommandList(CommandList *commandList) noexcept;
    HRESULT abortCommandList(CommandList *commandList) noexcept;
    ID3D12GraphicsCommandList *checkedNativeCommandList(
        CommandList *commandList) noexcept;
    ResourceState commandTextureState(CommandList *commandList,
                                      Texture2D *texture) noexcept;
    HRESULT setCommandTextureState(CommandList *commandList, Texture2D *texture,
                                   ResourceState state) noexcept;

    HRESULT transitionTexture(CommandList *commandList, Texture2D *texture,
                              ResourceState before, ResourceState after) noexcept;
    HRESULT copyTexture(CommandList *commandList, Texture2D *source,
                        Texture2D *destination,
                        const TextureCopyRegion &region) noexcept;
    HRESULT copyBuffer(CommandList *commandList, Buffer *source,
                       Buffer *destination, uint64_t sourceOffset,
                       uint64_t destinationOffset, uint64_t size) noexcept;
    HRESULT writeBuffer(CommandList *commandList, Buffer *destination,
                        uint64_t destinationOffset, const void *data,
                        size_t size) noexcept;
    HRESULT writeTexture(CommandList *commandList, Texture2D *destination,
                         const TextureWriteRegion &region, const void *data,
                         size_t size) noexcept;
    HRESULT clearTextureRgba(CommandList *commandList, Texture2D *texture,
                             float red, float green, float blue,
                             float alpha) noexcept;
    HRESULT uavBarrier(CommandList *commandList, void *resourceOrNull) noexcept;

    // The queue operations are serialized as one transaction:
    // shared-fence wait, execute, internal completion signal, shared-fence signal.
    // A null shared fence is valid only when both values are zero.
    HRESULT submit(Device *device, CommandList *commandList,
                   SharedFence *sharedFence, uint64_t waitValue,
                   uint64_t signalValue,
                   SubmissionDisposition *outDisposition = nullptr) noexcept;
    HRESULT recoverSharedFence(Device *device, SharedFence *sharedFence,
                               uint64_t waitValue,
                               uint64_t signalValue) noexcept;
    HRESULT recoverExecutedSharedFence(Device *device,
                                       SharedFence *sharedFence,
                                       uint64_t waitValue,
                                       uint64_t signalValue) noexcept;
} // namespace sr::d3d12
