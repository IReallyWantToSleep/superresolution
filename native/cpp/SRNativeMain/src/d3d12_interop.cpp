#include <jni.h>

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
#include <cstdio>
#include <new>
#include <string>

using Microsoft::WRL::ComPtr;

namespace {
    constexpr uint32_t RESOURCE_COUNT = 5;
    constexpr uint32_t RESOURCE_INPUT_COLOR = 0;
    constexpr uint32_t RESOURCE_INPUT_DEPTH = 1;
    constexpr uint32_t RESOURCE_INPUT_MOTION_VECTORS = 2;
    constexpr uint32_t RESOURCE_INPUT_EXPOSURE = 3;
    constexpr uint32_t RESOURCE_OUTPUT_COLOR = 4;

    // Values mirror SRSurfaceFormat / FfxApiSurfaceFormat.
    constexpr int SR_FORMAT_R16G16B16A16_FLOAT = 4;
    constexpr int SR_FORMAT_R8G8B8A8_UNORM = 10;
    constexpr int SR_FORMAT_R11G11B10_FLOAT = 16;
    constexpr int SR_FORMAT_R16G16_FLOAT = 18;
    constexpr int SR_FORMAT_R16_FLOAT = 21;
    constexpr int SR_FORMAT_R32_FLOAT = 28;

    struct SharedTexture {
        ComPtr<ID3D12Resource> resource;
        HANDLE sharedHandle = nullptr;
        uint64_t allocationSize = 0;
    };

    struct D3D12InteropContext {
        ComPtr<IDXGIAdapter1> adapter;
        ComPtr<ID3D12Device> device;
        ComPtr<ID3D12CommandQueue> queue;
        ComPtr<ID3D12CommandAllocator> commandAllocator;
        ComPtr<ID3D12GraphicsCommandList> commandList;
        ComPtr<ID3D12Fence> fence;
        HANDLE fenceSharedHandle = nullptr;
        HANDLE fenceEvent = nullptr;
        uint64_t lastSubmittedFenceValue = 0;
        bool recording = false;
        std::array<SharedTexture, RESOURCE_COUNT> resources;
    };

    thread_local std::string g_lastError;

    void setError(const char *message) {
        g_lastError = message ? message : "Unknown D3D12 interop error.";
    }

    void setHresultError(const char *operation, HRESULT hr) {
        char buffer[256] = {};
        std::snprintf(
            buffer,
            sizeof(buffer),
            "%s failed with HRESULT 0x%08lX.",
            operation,
            static_cast<unsigned long>(hr));
        setError(buffer);
    }

    bool sameLuid(const LUID &left, uint64_t right) {
        const uint64_t leftValue =
            static_cast<uint64_t>(left.LowPart) |
            (static_cast<uint64_t>(static_cast<uint32_t>(left.HighPart)) << 32);
        return leftValue == right;
    }

    DXGI_FORMAT toDxgiFormat(int format) {
        switch (format) {
            case SR_FORMAT_R16G16B16A16_FLOAT:
                return DXGI_FORMAT_R16G16B16A16_FLOAT;
            case SR_FORMAT_R8G8B8A8_UNORM:
                return DXGI_FORMAT_R8G8B8A8_UNORM;
            case SR_FORMAT_R11G11B10_FLOAT:
                return DXGI_FORMAT_R11G11B10_FLOAT;
            case SR_FORMAT_R16G16_FLOAT:
                return DXGI_FORMAT_R16G16_FLOAT;
            case SR_FORMAT_R16_FLOAT:
                return DXGI_FORMAT_R16_FLOAT;
            case SR_FORMAT_R32_FLOAT:
                return DXGI_FORMAT_R32_FLOAT;
            default:
                return DXGI_FORMAT_UNKNOWN;
        }
    }

    bool createSharedTexture(
        D3D12InteropContext *context,
        uint32_t index,
        uint32_t width,
        uint32_t height,
        DXGI_FORMAT format) {
        if (!context || index >= RESOURCE_COUNT || width == 0 || height == 0 ||
            format == DXGI_FORMAT_UNKNOWN) {
            setError("Invalid shared D3D12 texture description.");
            return false;
        }

        D3D12_HEAP_PROPERTIES heapProperties = {};
        heapProperties.Type = D3D12_HEAP_TYPE_DEFAULT;
        heapProperties.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
        heapProperties.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
        heapProperties.CreationNodeMask = 1;
        heapProperties.VisibleNodeMask = 1;

        D3D12_RESOURCE_DESC resourceDesc = {};
        resourceDesc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
        resourceDesc.Width = width;
        resourceDesc.Height = height;
        resourceDesc.DepthOrArraySize = 1;
        resourceDesc.MipLevels = 1;
        resourceDesc.Format = format;
        resourceDesc.SampleDesc.Count = 1;
        resourceDesc.SampleDesc.Quality = 0;
        resourceDesc.Layout = D3D12_TEXTURE_LAYOUT_UNKNOWN;
        resourceDesc.Flags = D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS;

        SharedTexture &texture = context->resources[index];
        HRESULT hr = context->device->CreateCommittedResource(
            &heapProperties,
            D3D12_HEAP_FLAG_SHARED,
            &resourceDesc,
            D3D12_RESOURCE_STATE_COMMON,
            nullptr,
            IID_PPV_ARGS(&texture.resource));
        if (FAILED(hr)) {
            setHresultError("ID3D12Device::CreateCommittedResource", hr);
            return false;
        }

        const D3D12_RESOURCE_ALLOCATION_INFO allocationInfo =
            context->device->GetResourceAllocationInfo(0, 1, &resourceDesc);
        if (allocationInfo.SizeInBytes == 0 ||
            allocationInfo.SizeInBytes == UINT64_MAX) {
            setError("D3D12 returned an invalid shared resource allocation size.");
            return false;
        }
        texture.allocationSize = allocationInfo.SizeInBytes;

        hr = context->device->CreateSharedHandle(
            texture.resource.Get(),
            nullptr,
            GENERIC_ALL,
            nullptr,
            &texture.sharedHandle);
        if (FAILED(hr)) {
            setHresultError("ID3D12Device::CreateSharedHandle(resource)", hr);
            return false;
        }
        return true;
    }

    bool waitForFence(D3D12InteropContext *context, uint64_t value) {
        if (!context || value == 0 ||
            context->fence->GetCompletedValue() >= value) {
            return true;
        }
        const HRESULT hr = context->fence->SetEventOnCompletion(
            value,
            context->fenceEvent);
        if (FAILED(hr)) {
            setHresultError("ID3D12Fence::SetEventOnCompletion", hr);
            return false;
        }
        if (WaitForSingleObject(context->fenceEvent, INFINITE) != WAIT_OBJECT_0) {
            setError("Waiting for the D3D12 interop fence failed.");
            return false;
        }
        return true;
    }

    void closeSharedHandles(D3D12InteropContext *context) {
        if (!context) {
            return;
        }
        for (SharedTexture &texture : context->resources) {
            if (texture.sharedHandle) {
                CloseHandle(texture.sharedHandle);
                texture.sharedHandle = nullptr;
            }
        }
        if (context->fenceSharedHandle) {
            CloseHandle(context->fenceSharedHandle);
            context->fenceSharedHandle = nullptr;
        }
        if (context->fenceEvent) {
            CloseHandle(context->fenceEvent);
            context->fenceEvent = nullptr;
        }
    }

    D3D12InteropContext *fromHandle(jlong handle) {
        return reinterpret_cast<D3D12InteropContext *>(
            static_cast<intptr_t>(handle));
    }

    SharedTexture *getResource(D3D12InteropContext *context, jint index) {
        if (!context || index < 0 ||
            static_cast<uint32_t>(index) >= RESOURCE_COUNT) {
            setError("Invalid D3D12 interop resource index.");
            return nullptr;
        }
        return &context->resources[static_cast<uint32_t>(index)];
    }
}

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12CreateContext(
        JNIEnv *,
        jclass,
        jlong adapterLuid,
        jint renderWidth,
        jint renderHeight,
        jint outputWidth,
        jint outputHeight,
        jint colorFormat) {
        g_lastError.clear();
        if (adapterLuid == 0 || renderWidth <= 0 || renderHeight <= 0 ||
            outputWidth <= 0 || outputHeight <= 0) {
            setError("Invalid D3D12 interop context dimensions or adapter LUID.");
            return 0;
        }

        const DXGI_FORMAT dxgiColorFormat = toDxgiFormat(colorFormat);
        if (dxgiColorFormat == DXGI_FORMAT_UNKNOWN) {
            setError("The configured internal color format is not supported by D3D12 interop.");
            return 0;
        }

        auto *context = new (std::nothrow) D3D12InteropContext();
        if (!context) {
            setError("Could not allocate the D3D12 interop context.");
            return 0;
        }

        ComPtr<IDXGIFactory6> factory;
        HRESULT hr = CreateDXGIFactory1(IID_PPV_ARGS(&factory));
        if (FAILED(hr)) {
            setHresultError("CreateDXGIFactory1", hr);
            delete context;
            return 0;
        }

        for (UINT index = 0;; ++index) {
            ComPtr<IDXGIAdapter1> candidate;
            if (factory->EnumAdapters1(index, &candidate) == DXGI_ERROR_NOT_FOUND) {
                break;
            }
            DXGI_ADAPTER_DESC1 desc = {};
            if (SUCCEEDED(candidate->GetDesc1(&desc)) &&
                (desc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) == 0 &&
                sameLuid(desc.AdapterLuid, static_cast<uint64_t>(adapterLuid))) {
                context->adapter = candidate;
                break;
            }
        }
        if (!context->adapter) {
            setError("No D3D12 adapter matches the OpenGL device LUID.");
            delete context;
            return 0;
        }

        hr = D3D12CreateDevice(
            context->adapter.Get(),
            D3D_FEATURE_LEVEL_12_0,
            IID_PPV_ARGS(&context->device));
        if (FAILED(hr)) {
            setHresultError("D3D12CreateDevice", hr);
            delete context;
            return 0;
        }

        D3D12_COMMAND_QUEUE_DESC queueDesc = {};
        queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
        queueDesc.Priority = D3D12_COMMAND_QUEUE_PRIORITY_NORMAL;
        queueDesc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
        hr = context->device->CreateCommandQueue(
            &queueDesc,
            IID_PPV_ARGS(&context->queue));
        if (FAILED(hr)) {
            setHresultError("ID3D12Device::CreateCommandQueue", hr);
            delete context;
            return 0;
        }

        hr = context->device->CreateCommandAllocator(
            D3D12_COMMAND_LIST_TYPE_DIRECT,
            IID_PPV_ARGS(&context->commandAllocator));
        if (FAILED(hr)) {
            setHresultError("ID3D12Device::CreateCommandAllocator", hr);
            delete context;
            return 0;
        }

        hr = context->device->CreateCommandList(
            0,
            D3D12_COMMAND_LIST_TYPE_DIRECT,
            context->commandAllocator.Get(),
            nullptr,
            IID_PPV_ARGS(&context->commandList));
        if (FAILED(hr)) {
            setHresultError("ID3D12Device::CreateCommandList", hr);
            delete context;
            return 0;
        }
        hr = context->commandList->Close();
        if (FAILED(hr)) {
            setHresultError("ID3D12GraphicsCommandList::Close(initial)", hr);
            delete context;
            return 0;
        }

        hr = context->device->CreateFence(
            0,
            D3D12_FENCE_FLAG_SHARED,
            IID_PPV_ARGS(&context->fence));
        if (FAILED(hr)) {
            setHresultError("ID3D12Device::CreateFence", hr);
            delete context;
            return 0;
        }
        context->fenceEvent = CreateEventW(nullptr, FALSE, FALSE, nullptr);
        if (!context->fenceEvent) {
            setError("CreateEventW failed for the D3D12 interop fence.");
            delete context;
            return 0;
        }
        hr = context->device->CreateSharedHandle(
            context->fence.Get(),
            nullptr,
            GENERIC_ALL,
            nullptr,
            &context->fenceSharedHandle);
        if (FAILED(hr)) {
            setHresultError("ID3D12Device::CreateSharedHandle(fence)", hr);
            closeSharedHandles(context);
            delete context;
            return 0;
        }

        const bool resourcesCreated =
            createSharedTexture(
                context,
                RESOURCE_INPUT_COLOR,
                static_cast<uint32_t>(renderWidth),
                static_cast<uint32_t>(renderHeight),
                dxgiColorFormat) &&
            createSharedTexture(
                context,
                RESOURCE_INPUT_DEPTH,
                static_cast<uint32_t>(renderWidth),
                static_cast<uint32_t>(renderHeight),
                DXGI_FORMAT_R32_FLOAT) &&
            createSharedTexture(
                context,
                RESOURCE_INPUT_MOTION_VECTORS,
                static_cast<uint32_t>(renderWidth),
                static_cast<uint32_t>(renderHeight),
                DXGI_FORMAT_R16G16_FLOAT) &&
            createSharedTexture(
                context,
                RESOURCE_INPUT_EXPOSURE,
                1,
                1,
                DXGI_FORMAT_R16_FLOAT) &&
            createSharedTexture(
                context,
                RESOURCE_OUTPUT_COLOR,
                static_cast<uint32_t>(outputWidth),
                static_cast<uint32_t>(outputHeight),
                dxgiColorFormat);
        if (!resourcesCreated) {
            closeSharedHandles(context);
            delete context;
            return 0;
        }

        return static_cast<jlong>(
            reinterpret_cast<intptr_t>(context));
    }

    JNIEXPORT void JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12DestroyContext(
        JNIEnv *,
        jclass,
        jlong contextHandle) {
        D3D12InteropContext *context = fromHandle(contextHandle);
        if (!context) {
            return;
        }
        waitForFence(context, context->lastSubmittedFenceValue);
        closeSharedHandles(context);
        delete context;
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetDevice(
        JNIEnv *,
        jclass,
        jlong contextHandle) {
        D3D12InteropContext *context = fromHandle(contextHandle);
        return context
                   ? static_cast<jlong>(
                         reinterpret_cast<intptr_t>(context->device.Get()))
                   : 0;
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetCommandList(
        JNIEnv *,
        jclass,
        jlong contextHandle) {
        D3D12InteropContext *context = fromHandle(contextHandle);
        return context
                   ? static_cast<jlong>(
                         reinterpret_cast<intptr_t>(context->commandList.Get()))
                   : 0;
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResource(
        JNIEnv *,
        jclass,
        jlong contextHandle,
        jint index) {
        SharedTexture *texture = getResource(fromHandle(contextHandle), index);
        return texture
                   ? static_cast<jlong>(
                         reinterpret_cast<intptr_t>(texture->resource.Get()))
                   : 0;
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResourceSharedHandle(
        JNIEnv *,
        jclass,
        jlong contextHandle,
        jint index) {
        SharedTexture *texture = getResource(fromHandle(contextHandle), index);
        return texture
                   ? static_cast<jlong>(
                         reinterpret_cast<intptr_t>(texture->sharedHandle))
                   : 0;
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResourceAllocationSize(
        JNIEnv *,
        jclass,
        jlong contextHandle,
        jint index) {
        SharedTexture *texture = getResource(fromHandle(contextHandle), index);
        return texture
                   ? static_cast<jlong>(texture->allocationSize)
                   : 0;
    }

    JNIEXPORT jlong JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetFenceSharedHandle(
        JNIEnv *,
        jclass,
        jlong contextHandle) {
        D3D12InteropContext *context = fromHandle(contextHandle);
        return context
                   ? static_cast<jlong>(
                         reinterpret_cast<intptr_t>(context->fenceSharedHandle))
                   : 0;
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12BeginFrame(
        JNIEnv *,
        jclass,
        jlong contextHandle,
        jlong waitFenceValue) {
        D3D12InteropContext *context = fromHandle(contextHandle);
        if (!context || waitFenceValue <= 0) {
            setError("Invalid D3D12 begin-frame arguments.");
            return E_INVALIDARG;
        }
        if (context->recording) {
            setError("The D3D12 command list is already recording.");
            return E_FAIL;
        }
        if (!waitForFence(context, context->lastSubmittedFenceValue)) {
            return E_FAIL;
        }

        HRESULT hr = context->commandAllocator->Reset();
        if (FAILED(hr)) {
            setHresultError("ID3D12CommandAllocator::Reset", hr);
            return static_cast<jint>(hr);
        }
        hr = context->commandList->Reset(
            context->commandAllocator.Get(),
            nullptr);
        if (FAILED(hr)) {
            setHresultError("ID3D12GraphicsCommandList::Reset", hr);
            return static_cast<jint>(hr);
        }
        hr = context->queue->Wait(
            context->fence.Get(),
            static_cast<uint64_t>(waitFenceValue));
        if (FAILED(hr)) {
            setHresultError("ID3D12CommandQueue::Wait", hr);
            context->commandList->Close();
            return static_cast<jint>(hr);
        }
        context->recording = true;
        return S_OK;
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12ExecuteFrame(
        JNIEnv *,
        jclass,
        jlong contextHandle,
        jlong signalFenceValue) {
        D3D12InteropContext *context = fromHandle(contextHandle);
        if (!context || !context->recording || signalFenceValue <= 0) {
            setError("Invalid D3D12 execute-frame state or fence value.");
            return E_INVALIDARG;
        }

        HRESULT hr = context->commandList->Close();
        context->recording = false;
        if (FAILED(hr)) {
            setHresultError("ID3D12GraphicsCommandList::Close", hr);
            return static_cast<jint>(hr);
        }

        ID3D12CommandList *commandLists[] = {context->commandList.Get()};
        context->queue->ExecuteCommandLists(1, commandLists);
        hr = context->queue->Signal(
            context->fence.Get(),
            static_cast<uint64_t>(signalFenceValue));
        if (FAILED(hr)) {
            setHresultError("ID3D12CommandQueue::Signal", hr);
            return static_cast<jint>(hr);
        }
        context->lastSubmittedFenceValue =
            static_cast<uint64_t>(signalFenceValue);
        return S_OK;
    }

    JNIEXPORT jint JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12WaitIdle(
        JNIEnv *,
        jclass,
        jlong contextHandle) {
        D3D12InteropContext *context = fromHandle(contextHandle);
        if (!context) {
            setError("The D3D12 interop context is null.");
            return E_INVALIDARG;
        }
        return waitForFence(context, context->lastSubmittedFenceValue)
                   ? S_OK
                   : E_FAIL;
    }

    JNIEXPORT jstring JNICALL
    Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetLastError(
        JNIEnv *env,
        jclass) {
        return env->NewStringUTF(g_lastError.c_str());
    }
}

#endif
