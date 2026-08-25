#include <jni.h>

#include "sr/d3d12/d3d12_runtime.h"

#include <cstddef>
#include <cstdint>
#include <limits>
#include <string>

namespace {
    template<typename T>
    T *fromHandle(jlong handle) noexcept {
        return reinterpret_cast<T *>(static_cast<intptr_t>(handle));
    }

    template<typename T>
    jlong toHandle(T *pointer) noexcept {
        return static_cast<jlong>(reinterpret_cast<intptr_t>(pointer));
    }

    jlong toHandle(HANDLE handle) noexcept {
        return static_cast<jlong>(reinterpret_cast<intptr_t>(handle));
    }

    jint toResult(HRESULT result) noexcept { return static_cast<jint>(result); }

    jlong toSubmitResult(
        HRESULT result,
        sr::d3d12::SubmissionDisposition disposition) noexcept {
        const uint64_t packed =
            (static_cast<uint64_t>(static_cast<uint32_t>(disposition)) << 32) |
            static_cast<uint32_t>(result);
        return static_cast<jlong>(packed);
    }

    sr::d3d12::ResourceState state(jint value) noexcept {
        return static_cast<sr::d3d12::ResourceState>(
            static_cast<uint32_t>(value));
    }

    bool nonnegative(jlong value, const char *message) noexcept {
        if (value >= 0) {
            return true;
        }
        sr::d3d12::setLastError(message);
        return false;
    }

    const void *directBufferSlice(JNIEnv *env, jobject buffer, jint offset,
                                  jint size) noexcept {
        if (!buffer || offset < 0 || size <= 0) {
            sr::d3d12::setLastError(
                "The JNI data buffer, offset, or size is invalid.");
            return nullptr;
        }
        void *base = env->GetDirectBufferAddress(buffer);
        const jlong capacity = env->GetDirectBufferCapacity(buffer);
        if (!base || capacity < 0 ||
            static_cast<jlong>(offset) > capacity ||
            static_cast<jlong>(size) > capacity - offset) {
            sr::d3d12::setLastError(
                "A direct ByteBuffer with a valid data range is required.");
            return nullptr;
        }
        return static_cast<const std::byte *>(base) + offset;
    }
} // namespace

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCreateDevice(
        JNIEnv *, jclass, jlong adapterLuid, jint debugFlags) {
        if (adapterLuid == 0) {
            sr::d3d12::setLastError("The adapter LUID must be nonzero.");
            return 0;
        }
        return toHandle(sr::d3d12::createDevice(
            static_cast<uint64_t>(adapterLuid),
            static_cast<uint32_t>(debugFlags)));
    }

    JNIEXPORT void JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nDestroyDevice(
        JNIEnv *, jclass, jlong device) {
        sr::d3d12::destroyDevice(fromHandle<sr::d3d12::Device>(device));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetNativeDevice(
        JNIEnv *, jclass, jlong device) {
        return toHandle(
            sr::d3d12::nativeDevice(fromHandle<sr::d3d12::Device>(device)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetNativeQueue(
        JNIEnv *, jclass, jlong device) {
        return toHandle(
            sr::d3d12::nativeQueue(fromHandle<sr::d3d12::Device>(device)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetDeviceAdapterLuid(
        JNIEnv *, jclass, jlong device) {
        return static_cast<jlong>(sr::d3d12::deviceAdapterLuid(
            fromHandle<sr::d3d12::Device>(device)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetCompletedSubmissionValue(
        JNIEnv *, jclass, jlong device) {
        return static_cast<jlong>(sr::d3d12::completedSubmissionValue(
            fromHandle<sr::d3d12::Device>(device)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetLastSubmittedValue(
        JNIEnv *, jclass, jlong device) {
        return static_cast<jlong>(sr::d3d12::lastSubmittedValue(
            fromHandle<sr::d3d12::Device>(device)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nWaitIdle(
        JNIEnv *, jclass, jlong device, jint timeoutMilliseconds) {
        if (timeoutMilliseconds < -1) {
            sr::d3d12::setLastError("Invalid D3D12 wait timeout.");
            return toResult(E_INVALIDARG);
        }
        const uint32_t timeout = timeoutMilliseconds < 0
                                     ? sr::d3d12::WAIT_INFINITE
                                     : static_cast<uint32_t>(timeoutMilliseconds);
        return toResult(sr::d3d12::waitIdle(
            fromHandle<sr::d3d12::Device>(device), timeout));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nSetDebugName(
        JNIEnv *env, jclass, jlong object, jstring name) {
        if (object == 0 || !name) {
            sr::d3d12::setLastError("Invalid D3D12 debug-name arguments.");
            return toResult(E_INVALIDARG);
        }
        const jchar *characters = env->GetStringChars(name, nullptr);
        if (!characters) {
            sr::d3d12::setLastError("Could not access the Java debug name.");
            return toResult(E_OUTOFMEMORY);
        }
        const jsize length = env->GetStringLength(name);
        static_assert(sizeof(jchar) == sizeof(wchar_t));
        std::wstring nativeName;
        try {
            nativeName.assign(reinterpret_cast<const wchar_t *>(characters),
                              static_cast<size_t>(length));
        } catch (...) {
            env->ReleaseStringChars(name, characters);
            sr::d3d12::setLastError("Could not allocate the D3D12 debug name.");
            return toResult(E_OUTOFMEMORY);
        }
        env->ReleaseStringChars(name, characters);
        return toResult(sr::d3d12::setObjectName(
            reinterpret_cast<void *>(static_cast<intptr_t>(object)),
            nativeName.c_str()));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCreateSharedFence(
        JNIEnv *, jclass, jlong device, jlong initialValue) {
        if (!nonnegative(initialValue,
                         "The initial shared fence value must be nonnegative.")) {
            return 0;
        }
        return toHandle(sr::d3d12::createSharedFence(
            fromHandle<sr::d3d12::Device>(device),
            static_cast<uint64_t>(initialValue)));
    }

    JNIEXPORT void JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nDestroySharedFence(
        JNIEnv *, jclass, jlong fence) {
        sr::d3d12::destroySharedFence(
            fromHandle<sr::d3d12::SharedFence>(fence));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetNativeFence(
        JNIEnv *, jclass, jlong fence) {
        return toHandle(sr::d3d12::nativeFence(
            fromHandle<sr::d3d12::SharedFence>(fence)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetSharedFenceHandle(
        JNIEnv *, jclass, jlong fence) {
        return toHandle(sr::d3d12::sharedFenceHandle(
            fromHandle<sr::d3d12::SharedFence>(fence)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nReserveSharedFenceValue(
        JNIEnv *, jclass, jlong fence) {
        return static_cast<jlong>(sr::d3d12::reserveSharedFenceValue(
            fromHandle<sr::d3d12::SharedFence>(fence)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetCompletedSharedFenceValue(
        JNIEnv *, jclass, jlong fence) {
        return static_cast<jlong>(sr::d3d12::completedSharedFenceValue(
            fromHandle<sr::d3d12::SharedFence>(fence)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nSignalSharedFence(
        JNIEnv *, jclass, jlong fence, jlong value) {
        if (!nonnegative(value, "The shared fence signal value is negative.")) {
            return toResult(E_INVALIDARG);
        }
        return toResult(sr::d3d12::signalSharedFence(
            fromHandle<sr::d3d12::SharedFence>(fence),
            static_cast<uint64_t>(value)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nWaitSharedFence(
        JNIEnv *, jclass, jlong fence, jlong value, jint timeoutMilliseconds) {
        if (!nonnegative(value, "The shared fence wait value is negative.") ||
            timeoutMilliseconds < -1) {
            sr::d3d12::setLastError("Invalid shared fence wait arguments.");
            return toResult(E_INVALIDARG);
        }
        const uint32_t timeout = timeoutMilliseconds < 0
                                     ? sr::d3d12::WAIT_INFINITE
                                     : static_cast<uint32_t>(timeoutMilliseconds);
        return toResult(sr::d3d12::waitSharedFence(
            fromHandle<sr::d3d12::SharedFence>(fence),
            static_cast<uint64_t>(value), timeout));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nRecoverSharedFence(
        JNIEnv *, jclass, jlong device, jlong fence, jlong waitValue,
        jlong signalValue) {
        if (!nonnegative(waitValue, "The recovery wait value is negative.") ||
            !nonnegative(signalValue,
                         "The recovery signal value is negative.")) {
            return toResult(E_INVALIDARG);
        }
        return toResult(sr::d3d12::recoverSharedFence(
            fromHandle<sr::d3d12::Device>(device),
            fromHandle<sr::d3d12::SharedFence>(fence),
            static_cast<uint64_t>(waitValue),
            static_cast<uint64_t>(signalValue)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nRecoverExecutedSharedFence(
        JNIEnv *, jclass, jlong device, jlong fence, jlong waitValue,
        jlong signalValue) {
        if (!nonnegative(waitValue,
                         "The executed recovery wait value is negative.") ||
            !nonnegative(signalValue,
                         "The executed recovery signal value is negative.")) {
            return toResult(E_INVALIDARG);
        }
        return toResult(sr::d3d12::recoverExecutedSharedFence(
            fromHandle<sr::d3d12::Device>(device),
            fromHandle<sr::d3d12::SharedFence>(fence),
            static_cast<uint64_t>(waitValue),
            static_cast<uint64_t>(signalValue)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCreateTexture2D(
        JNIEnv *, jclass, jlong device, jint width, jint height, jint mipLevels,
        jint surfaceFormat, jint resourceFlags, jint initialState,
        jboolean shared) {
        if (width <= 0 || height <= 0 || mipLevels <= 0 ||
            mipLevels > std::numeric_limits<uint16_t>::max()) {
            sr::d3d12::setLastError("Invalid JNI Texture2D dimensions or mip count.");
            return 0;
        }
        return toHandle(sr::d3d12::createTexture2D(
            fromHandle<sr::d3d12::Device>(device),
            static_cast<uint32_t>(width), static_cast<uint32_t>(height),
            static_cast<uint16_t>(mipLevels), surfaceFormat,
            static_cast<uint32_t>(resourceFlags), state(initialState),
            shared == JNI_TRUE));
    }

    JNIEXPORT void JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nDestroyTexture2D(
        JNIEnv *, jclass, jlong texture) {
        sr::d3d12::destroyTexture2D(
            fromHandle<sr::d3d12::Texture2D>(texture));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetNativeTextureResource(
        JNIEnv *, jclass, jlong texture) {
        return toHandle(sr::d3d12::nativeTextureResource(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureSharedHandle(
        JNIEnv *, jclass, jlong texture) {
        return toHandle(sr::d3d12::textureSharedHandle(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureAllocationSize(
        JNIEnv *, jclass, jlong texture) {
        return static_cast<jlong>(sr::d3d12::textureAllocationSize(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureWidth(
        JNIEnv *, jclass, jlong texture) {
        return static_cast<jint>(sr::d3d12::textureWidth(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureHeight(
        JNIEnv *, jclass, jlong texture) {
        return static_cast<jint>(sr::d3d12::textureHeight(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureMipLevels(
        JNIEnv *, jclass, jlong texture) {
        return static_cast<jint>(sr::d3d12::textureMipLevels(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureSurfaceFormat(
        JNIEnv *, jclass, jlong texture) {
        return static_cast<jint>(sr::d3d12::textureSurfaceFormat(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureResourceFlags(
        JNIEnv *, jclass, jlong texture) {
        return static_cast<jint>(sr::d3d12::textureResourceFlags(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureInitialState(
        JNIEnv *, jclass, jlong texture) {
        return static_cast<jint>(sr::d3d12::textureInitialState(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetTextureCommittedState(
        JNIEnv *, jclass, jlong texture) {
        return static_cast<jint>(sr::d3d12::textureCommittedState(
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nSetTextureCommittedState(
        JNIEnv *, jclass, jlong texture, jint committedState) {
        return toResult(sr::d3d12::setTextureCommittedState(
            fromHandle<sr::d3d12::Texture2D>(texture), state(committedState)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCreateBuffer(
        JNIEnv *, jclass, jlong device, jlong size, jint heapType,
        jint resourceFlags, jint initialState, jboolean shared) {
        if (size <= 0) {
            sr::d3d12::setLastError("The D3D12 buffer size must be positive.");
            return 0;
        }
        return toHandle(sr::d3d12::createBuffer(
            fromHandle<sr::d3d12::Device>(device), static_cast<uint64_t>(size),
            static_cast<sr::d3d12::BufferHeap>(
                static_cast<uint32_t>(heapType)),
            static_cast<uint32_t>(resourceFlags), state(initialState),
            shared == JNI_TRUE));
    }

    JNIEXPORT void JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nDestroyBuffer(
        JNIEnv *, jclass, jlong buffer) {
        sr::d3d12::destroyBuffer(fromHandle<sr::d3d12::Buffer>(buffer));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetNativeBufferResource(
        JNIEnv *, jclass, jlong buffer) {
        return toHandle(sr::d3d12::nativeBufferResource(
            fromHandle<sr::d3d12::Buffer>(buffer)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetBufferSharedHandle(
        JNIEnv *, jclass, jlong buffer) {
        return toHandle(sr::d3d12::bufferSharedHandle(
            fromHandle<sr::d3d12::Buffer>(buffer)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetBufferSize(
        JNIEnv *, jclass, jlong buffer) {
        return static_cast<jlong>(sr::d3d12::bufferSize(
            fromHandle<sr::d3d12::Buffer>(buffer)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetBufferHeapType(
        JNIEnv *, jclass, jlong buffer) {
        return static_cast<jint>(sr::d3d12::bufferHeap(
            fromHandle<sr::d3d12::Buffer>(buffer)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetBufferResourceFlags(
        JNIEnv *, jclass, jlong buffer) {
        return static_cast<jint>(sr::d3d12::bufferResourceFlags(
            fromHandle<sr::d3d12::Buffer>(buffer)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetBufferInitialState(
        JNIEnv *, jclass, jlong buffer) {
        return static_cast<jint>(sr::d3d12::bufferInitialState(
            fromHandle<sr::d3d12::Buffer>(buffer)));
    }

    JNIEXPORT jobject JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nMapBuffer(
        JNIEnv *env, jclass, jlong buffer, jlong offset, jlong size) {
        if (offset < 0 || size < 0) {
            sr::d3d12::setLastError("Invalid JNI buffer map range.");
            return nullptr;
        }
        void *mappedData = nullptr;
        const HRESULT hr = sr::d3d12::mapBuffer(
            fromHandle<sr::d3d12::Buffer>(buffer),
            static_cast<uint64_t>(offset), static_cast<uint64_t>(size),
            &mappedData);
        if (FAILED(hr)) {
            return nullptr;
        }

        jobject directBuffer = env->NewDirectByteBuffer(mappedData, size);
        if (!directBuffer) {
            sr::d3d12::unmapBuffer(
                fromHandle<sr::d3d12::Buffer>(buffer),
                static_cast<uint64_t>(offset), static_cast<uint64_t>(size));
            if (!env->ExceptionCheck()) {
                sr::d3d12::setLastError(
                    "Could not create a direct ByteBuffer for the mapped D3D12 buffer.");
            }
            return nullptr;
        }
        return directBuffer;
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nUnmapBuffer(
        JNIEnv *, jclass, jlong buffer, jlong offset, jlong size) {
        if (offset < 0 || size < 0) {
            sr::d3d12::setLastError("Invalid JNI buffer unmap range.");
            return toResult(E_INVALIDARG);
        }
        return toResult(sr::d3d12::unmapBuffer(
            fromHandle<sr::d3d12::Buffer>(buffer),
            static_cast<uint64_t>(offset), static_cast<uint64_t>(size)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCreateCommandAllocator(
        JNIEnv *, jclass, jlong device) {
        return toHandle(sr::d3d12::createCommandAllocator(
            fromHandle<sr::d3d12::Device>(device)));
    }

    JNIEXPORT void JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nDestroyCommandAllocator(
        JNIEnv *, jclass, jlong allocator) {
        sr::d3d12::destroyCommandAllocator(
            fromHandle<sr::d3d12::CommandAllocator>(allocator));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetNativeCommandAllocator(
        JNIEnv *, jclass, jlong allocator) {
        return toHandle(sr::d3d12::nativeCommandAllocator(
            fromHandle<sr::d3d12::CommandAllocator>(allocator)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCreateCommandList(
        JNIEnv *, jclass, jlong device, jlong allocator) {
        return toHandle(sr::d3d12::createCommandList(
            fromHandle<sr::d3d12::Device>(device),
            fromHandle<sr::d3d12::CommandAllocator>(allocator)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nDestroyCommandList(
        JNIEnv *, jclass, jlong commandList) {
        return toResult(sr::d3d12::destroyCommandList(
            fromHandle<sr::d3d12::CommandList>(commandList)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nBeginCommandList(
        JNIEnv *, jclass, jlong commandList) {
        return toResult(sr::d3d12::beginCommandList(
            fromHandle<sr::d3d12::CommandList>(commandList)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nEndCommandList(
        JNIEnv *, jclass, jlong commandList) {
        return toResult(sr::d3d12::endCommandList(
            fromHandle<sr::d3d12::CommandList>(commandList)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nAbortCommandList(
        JNIEnv *, jclass, jlong commandList) {
        return toResult(sr::d3d12::abortCommandList(
            fromHandle<sr::d3d12::CommandList>(commandList)));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetNativeCommandList(
        JNIEnv *, jclass, jlong commandList) {
        return toHandle(sr::d3d12::checkedNativeCommandList(
            fromHandle<sr::d3d12::CommandList>(commandList)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetCommandTextureState(
        JNIEnv *, jclass, jlong commandList, jlong texture) {
        return static_cast<jint>(sr::d3d12::commandTextureState(
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::Texture2D>(texture)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nSetCommandTextureState(
        JNIEnv *, jclass, jlong commandList, jlong texture, jint value) {
        return toResult(sr::d3d12::setCommandTextureState(
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::Texture2D>(texture), state(value)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCmdTransitionTexture(
        JNIEnv *, jclass, jlong commandList, jlong texture, jint before,
        jint after) {
        return toResult(sr::d3d12::transitionTexture(
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::Texture2D>(texture), state(before),
            state(after)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCmdCopyTexture(
        JNIEnv *, jclass, jlong commandList, jlong source, jlong destination,
        jint sourceX, jint sourceY, jint width, jint height, jint sourceMip,
        jint destinationX, jint destinationY, jint destinationMip) {
        if (sourceX < 0 || sourceY < 0 || width <= 0 || height <= 0 ||
            sourceMip < 0 || destinationX < 0 || destinationY < 0 ||
            destinationMip < 0) {
            sr::d3d12::setLastError("Invalid JNI texture copy region.");
            return toResult(E_INVALIDARG);
        }
        const sr::d3d12::TextureCopyRegion region = {
            static_cast<uint32_t>(sourceX),
            static_cast<uint32_t>(sourceY),
            static_cast<uint32_t>(width),
            static_cast<uint32_t>(height),
            static_cast<uint32_t>(sourceMip),
            static_cast<uint32_t>(destinationX),
            static_cast<uint32_t>(destinationY),
            static_cast<uint32_t>(destinationMip),
        };
        return toResult(sr::d3d12::copyTexture(
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::Texture2D>(source),
            fromHandle<sr::d3d12::Texture2D>(destination), region));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCmdCopyBuffer(
        JNIEnv *, jclass, jlong commandList, jlong source, jlong destination,
        jlong sourceOffset, jlong destinationOffset, jlong size) {
        if (sourceOffset < 0 || destinationOffset < 0 || size <= 0) {
            sr::d3d12::setLastError("Invalid JNI buffer copy range.");
            return toResult(E_INVALIDARG);
        }
        return toResult(sr::d3d12::copyBuffer(
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::Buffer>(source),
            fromHandle<sr::d3d12::Buffer>(destination),
            static_cast<uint64_t>(sourceOffset),
            static_cast<uint64_t>(destinationOffset),
            static_cast<uint64_t>(size)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCmdWriteBuffer(
        JNIEnv *env, jclass, jlong commandList, jlong destination,
        jlong destinationOffset, jobject data, jint dataOffset, jint size) {
        if (destinationOffset < 0) {
            sr::d3d12::setLastError("The destination buffer offset is negative.");
            return toResult(E_INVALIDARG);
        }
        const void *source = directBufferSlice(env, data, dataOffset, size);
        if (!source) {
            return toResult(E_INVALIDARG);
        }
        return toResult(sr::d3d12::writeBuffer(
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::Buffer>(destination),
            static_cast<uint64_t>(destinationOffset), source,
            static_cast<size_t>(size)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCmdWriteTexture2D(
        JNIEnv *env, jclass, jlong commandList, jlong destination, jint x, jint y,
        jint width, jint height, jint mip, jint sourceRowPitch, jobject data,
        jint dataOffset, jint dataSize) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0 || mip < 0 ||
            sourceRowPitch < 0) {
            sr::d3d12::setLastError("Invalid JNI texture write region.");
            return toResult(E_INVALIDARG);
        }
        const void *source = directBufferSlice(env, data, dataOffset, dataSize);
        if (!source) {
            return toResult(E_INVALIDARG);
        }
        const sr::d3d12::TextureWriteRegion region = {
            static_cast<uint32_t>(x),
            static_cast<uint32_t>(y),
            static_cast<uint32_t>(width),
            static_cast<uint32_t>(height),
            static_cast<uint32_t>(mip),
            static_cast<uint32_t>(sourceRowPitch),
        };
        return toResult(sr::d3d12::writeTexture(
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::Texture2D>(destination), region, source,
            static_cast<size_t>(dataSize)));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCmdClearTextureRgba(
        JNIEnv *, jclass, jlong commandList, jlong texture, jfloat red,
        jfloat green, jfloat blue, jfloat alpha) {
        return toResult(sr::d3d12::clearTextureRgba(
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::Texture2D>(texture), red, green, blue,
            alpha));
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nCmdUavBarrier(
        JNIEnv *, jclass, jlong commandList, jlong resourceOrZero) {
        return toResult(sr::d3d12::uavBarrier(
            fromHandle<sr::d3d12::CommandList>(commandList),
            reinterpret_cast<void *>(static_cast<intptr_t>(resourceOrZero))));
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nSubmit(
        JNIEnv *, jclass, jlong device, jlong commandList, jlong sharedFence,
        jlong waitValue, jlong signalValue) {
        sr::d3d12::SubmissionDisposition disposition =
            sr::d3d12::SubmissionDisposition::NotExecuted;
        if (!nonnegative(waitValue, "The submit wait value is negative.") ||
            !nonnegative(signalValue, "The submit signal value is negative.")) {
            return toSubmitResult(E_INVALIDARG, disposition);
        }
        const HRESULT result = sr::d3d12::submit(
            fromHandle<sr::d3d12::Device>(device),
            fromHandle<sr::d3d12::CommandList>(commandList),
            fromHandle<sr::d3d12::SharedFence>(sharedFence),
            static_cast<uint64_t>(waitValue),
            static_cast<uint64_t>(signalValue),
            &disposition);
        return toSubmitResult(result, disposition);
    }

    JNIEXPORT jstring JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12Native_nGetLastError(
        JNIEnv *env, jclass) {
        return env->NewStringUTF(sr::d3d12::lastError());
    }
} // extern "C"
