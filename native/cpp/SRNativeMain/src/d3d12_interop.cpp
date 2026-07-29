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
#include <mutex>
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
std::mutex g_deviceMutex;
ComPtr<ID3D12Device> g_sharedDevice;
uint64_t g_sharedDeviceLuid = 0;

/**
 * Records an error message in the thread-local error string, which can later
 * be retrieved from Java via Nd3d12GetLastError. A null message is replaced
 * with a generic fallback.
 */
void setError(const char *message) {
  g_lastError = message ? message : "Unknown D3D12 interop error.";
}

/**
 * Formats "<operation> failed with HRESULT 0x........" and stores it as the
 * thread-local last-error string (see setError).
 */
void setHresultError(const char *operation, HRESULT hr) {
  char buffer[256] = {};
  std::snprintf(buffer, sizeof(buffer), "%s failed with HRESULT 0x%08lX.",
                operation, static_cast<unsigned long>(hr));
  setError(buffer);
}

/**
 * Compares a DXGI LUID against its packed 64-bit representation
 * (LowPart in the low 32 bits, HighPart in the high 32 bits), which is how
 * the adapter LUID is passed across the JNI boundary.
 */
bool sameLuid(const LUID &left, uint64_t right) {
  const uint64_t leftValue =
      static_cast<uint64_t>(left.LowPart) |
      (static_cast<uint64_t>(static_cast<uint32_t>(left.HighPart)) << 32);
  return leftValue == right;
}

/**
 * Maps an SRSurfaceFormat/FfxApiSurfaceFormat value (as passed from Java) to
 * the matching DXGI_FORMAT. Returns DXGI_FORMAT_UNKNOWN for unsupported
 * formats so callers can reject them.
 */
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

/**
 * Creates one of the shared interop textures (index RESOURCE_INPUT_* /
 * RESOURCE_OUTPUT_COLOR) as a committed UAV-capable Texture2D on the DEFAULT
 * heap with D3D12_HEAP_FLAG_SHARED, records its driver allocation size, and
 * exports an NT shared handle so the resource can be opened from OpenGL
 * (e.g. via GL_EXT_memory_object). On failure the thread-local error string
 * is set and false is returned.
 */
bool createSharedTexture(D3D12InteropContext *context, uint32_t index,
                         uint32_t width, uint32_t height, DXGI_FORMAT format) {
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
      &heapProperties, D3D12_HEAP_FLAG_SHARED, &resourceDesc,
      D3D12_RESOURCE_STATE_COMMON, nullptr, IID_PPV_ARGS(&texture.resource));
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

  hr = context->device->CreateSharedHandle(texture.resource.Get(), nullptr,
                                           GENERIC_ALL, nullptr,
                                           &texture.sharedHandle);
  if (FAILED(hr)) {
    setHresultError("ID3D12Device::CreateSharedHandle(resource)", hr);
    return false;
  }
  return true;
}

/**
 * Blocks the calling thread until the interop fence reaches the given value.
 * Returns immediately (success) when the context is null, the value is 0, or
 * the fence has already completed it; otherwise arms the fence event and
 * waits on it. Returns false and sets the error string on failure.
 */
bool waitForFence(D3D12InteropContext *context, uint64_t value) {
  if (!context || value == 0 || context->fence->GetCompletedValue() >= value) {
    return true;
  }
  const HRESULT hr =
      context->fence->SetEventOnCompletion(value, context->fenceEvent);
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

/**
 * Closes every Win32 handle owned by the context: the shared handle of each
 * interop texture, the shared fence handle, and the fence wait event.
 * COM resources are released separately via ComPtr when the context is
 * destroyed.
 */
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

/**
 * Reinterprets the opaque jlong handle returned by Nd3d12CreateContext back
 * into the native context pointer. Returns null for a 0 handle.
 */
D3D12InteropContext *fromHandle(jlong handle) {
  return reinterpret_cast<D3D12InteropContext *>(static_cast<intptr_t>(handle));
}

/**
 * Looks up one of the shared interop textures by index
 * (RESOURCE_INPUT_COLOR .. RESOURCE_OUTPUT_COLOR). Returns null and sets the
 * error string when the context is null or the index is out of range.
 */
SharedTexture *getResource(D3D12InteropContext *context, jint index) {
  if (!context || index < 0 || static_cast<uint32_t>(index) >= RESOURCE_COUNT) {
    setError("Invalid D3D12 interop resource index.");
    return nullptr;
  }
  return &context->resources[static_cast<uint32_t>(index)];
}
} // namespace

extern "C" {
/**
 * Creates a D3D12 interop context for the GPU identified by adapterLuid
 * (matching the OpenGL device so both APIs share one adapter).
 *
 * Initializes (or reuses, when the LUID matches and the device is not
 * removed) a process-wide shared ID3D12Device, then creates a direct command
 * queue, allocator, and command list, a shared fence with its wait event and
 * NT handle, and the five shared interop textures: input color (render size,
 * colorFormat), input depth (R32_FLOAT), input motion vectors (R16G16_FLOAT),
 * input exposure (1x1 R16_FLOAT), and output color (output size, colorFormat).
 *
 * Returns an opaque context handle (pointer as jlong), or 0 on failure with
 * the reason available via Nd3d12GetLastError.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12CreateContext(
    JNIEnv *, jclass, jlong adapterLuid, jint renderWidth, jint renderHeight,
    jint outputWidth, jint outputHeight, jint colorFormat) {
  g_lastError.clear();
  if (adapterLuid == 0 || renderWidth <= 0 || renderHeight <= 0 ||
      outputWidth <= 0 || outputHeight <= 0) {
    setError("Invalid D3D12 interop context dimensions or adapter LUID.");
    return 0;
  }

  const DXGI_FORMAT dxgiColorFormat = toDxgiFormat(colorFormat);
  if (dxgiColorFormat == DXGI_FORMAT_UNKNOWN) {
    setError("The configured internal color format is not supported by D3D12 "
             "interop.");
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

  {
    std::lock_guard<std::mutex> lock(g_deviceMutex);
    if (g_sharedDevice &&
        g_sharedDeviceLuid == static_cast<uint64_t>(adapterLuid) &&
        SUCCEEDED(g_sharedDevice->GetDeviceRemovedReason())) {
      context->device = g_sharedDevice;
    } else {
      g_sharedDevice.Reset();
      g_sharedDeviceLuid = 0;
      hr = D3D12CreateDevice(context->adapter.Get(), D3D_FEATURE_LEVEL_12_0,
                             IID_PPV_ARGS(&context->device));
      if (FAILED(hr)) {
        setHresultError("D3D12CreateDevice", hr);
        delete context;
        return 0;
      }
      g_sharedDevice = context->device;
      g_sharedDeviceLuid = static_cast<uint64_t>(adapterLuid);
    }
  }

  D3D12_COMMAND_QUEUE_DESC queueDesc = {};
  queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
  queueDesc.Priority = D3D12_COMMAND_QUEUE_PRIORITY_NORMAL;
  queueDesc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
  hr = context->device->CreateCommandQueue(&queueDesc,
                                           IID_PPV_ARGS(&context->queue));
  if (FAILED(hr)) {
    setHresultError("ID3D12Device::CreateCommandQueue", hr);
    delete context;
    return 0;
  }

  hr = context->device->CreateCommandAllocator(
      D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&context->commandAllocator));
  if (FAILED(hr)) {
    setHresultError("ID3D12Device::CreateCommandAllocator", hr);
    delete context;
    return 0;
  }

  hr = context->device->CreateCommandList(
      0, D3D12_COMMAND_LIST_TYPE_DIRECT, context->commandAllocator.Get(),
      nullptr, IID_PPV_ARGS(&context->commandList));
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

  hr = context->device->CreateFence(0, D3D12_FENCE_FLAG_SHARED,
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
  hr = context->device->CreateSharedHandle(context->fence.Get(), nullptr,
                                           GENERIC_ALL, nullptr,
                                           &context->fenceSharedHandle);
  if (FAILED(hr)) {
    setHresultError("ID3D12Device::CreateSharedHandle(fence)", hr);
    closeSharedHandles(context);
    delete context;
    return 0;
  }

  const bool resourcesCreated =
      createSharedTexture(
          context, RESOURCE_INPUT_COLOR, static_cast<uint32_t>(renderWidth),
          static_cast<uint32_t>(renderHeight), dxgiColorFormat) &&
      createSharedTexture(
          context, RESOURCE_INPUT_DEPTH, static_cast<uint32_t>(renderWidth),
          static_cast<uint32_t>(renderHeight), DXGI_FORMAT_R32_FLOAT) &&
      createSharedTexture(context, RESOURCE_INPUT_MOTION_VECTORS,
                          static_cast<uint32_t>(renderWidth),
                          static_cast<uint32_t>(renderHeight),
                          DXGI_FORMAT_R16G16_FLOAT) &&
      createSharedTexture(context, RESOURCE_INPUT_EXPOSURE, 1, 1,
                          DXGI_FORMAT_R16_FLOAT) &&
      createSharedTexture(context, RESOURCE_OUTPUT_COLOR,
                          static_cast<uint32_t>(outputWidth),
                          static_cast<uint32_t>(outputHeight), dxgiColorFormat);
  if (!resourcesCreated) {
    closeSharedHandles(context);
    delete context;
    return 0;
  }

  return static_cast<jlong>(reinterpret_cast<intptr_t>(context));
}

/**
 * Destroys a context created by Nd3d12CreateContext: waits for the last
 * submitted fence value so no in-flight GPU work references the resources,
 * closes all shared Win32 handles, and frees the context. A null handle is
 * a no-op.
 */
JNIEXPORT void JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12DestroyContext(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  if (!context) {
    return;
  }
  waitForFence(context, context->lastSubmittedFenceValue);
  closeSharedHandles(context);
  delete context;
}

/**
 * Returns the raw ID3D12Device pointer of the context as a jlong, so Java can
 * hand it to the FidelityFX FSR backend. Returns 0 for an invalid handle.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetDevice(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  return context ? static_cast<jlong>(
                       reinterpret_cast<intptr_t>(context->device.Get()))
                 : 0;
}

/**
 * Returns the raw ID3D12GraphicsCommandList pointer of the context as a
 * jlong, so Java can record upscaling commands into it between
 * Nd3d12BeginFrame and Nd3d12ExecuteFrame. Returns 0 for an invalid handle.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetCommandList(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  return context ? static_cast<jlong>(
                       reinterpret_cast<intptr_t>(context->commandList.Get()))
                 : 0;
}

/**
 * Returns the raw ID3D12Resource pointer of the interop texture at the given
 * index as a jlong, so Java can bind it as an FSR input/output. Returns 0 for
 * an invalid handle or index (with the error string set in the latter case).
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResource(
    JNIEnv *, jclass, jlong contextHandle, jint index) {
  SharedTexture *texture = getResource(fromHandle(contextHandle), index);
  return texture ? static_cast<jlong>(
                       reinterpret_cast<intptr_t>(texture->resource.Get()))
                 : 0;
}

/**
 * Returns the NT shared handle (as a jlong) of the interop texture at the
 * given index, used to import the texture into OpenGL. Returns 0 for an
 * invalid handle or index.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResourceSharedHandle(
    JNIEnv *, jclass, jlong contextHandle, jint index) {
  SharedTexture *texture = getResource(fromHandle(contextHandle), index);
  return texture ? static_cast<jlong>(
                       reinterpret_cast<intptr_t>(texture->sharedHandle))
                 : 0;
}

/**
 * Returns the driver-reported allocation size in bytes of the interop texture
 * at the given index. OpenGL needs this when importing the shared memory
 * object. Returns 0 for an invalid handle or index.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResourceAllocationSize(
    JNIEnv *, jclass, jlong contextHandle, jint index) {
  SharedTexture *texture = getResource(fromHandle(contextHandle), index);
  return texture ? static_cast<jlong>(texture->allocationSize) : 0;
}

/**
 * Returns the NT shared handle (as a jlong) of the context's fence, used to
 * import the fence into OpenGL as a semaphore for cross-API synchronization.
 * Returns 0 for an invalid handle.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetFenceSharedHandle(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  return context ? static_cast<jlong>(
                       reinterpret_cast<intptr_t>(context->fenceSharedHandle))
                 : 0;
}

/**
 * Starts a D3D12 frame: CPU-waits for the previous frame's fence value,
 * resets the command allocator and list into recording state, then makes the
 * queue GPU-wait on waitFenceValue — the fence value signaled by OpenGL once
 * the input textures are ready. Java may then record commands into the list
 * obtained from Nd3d12GetCommandList.
 *
 * Returns S_OK on success, E_INVALIDARG/E_FAIL for invalid state, or the
 * failing HRESULT (as jint) otherwise; the error string holds details.
 */
JNIEXPORT jint JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12BeginFrame(
    JNIEnv *, jclass, jlong contextHandle, jlong waitFenceValue) {
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
  hr = context->commandList->Reset(context->commandAllocator.Get(), nullptr);
  if (FAILED(hr)) {
    setHresultError("ID3D12GraphicsCommandList::Reset", hr);
    return static_cast<jint>(hr);
  }
  hr = context->queue->Wait(context->fence.Get(),
                            static_cast<uint64_t>(waitFenceValue));
  if (FAILED(hr)) {
    setHresultError("ID3D12CommandQueue::Wait", hr);
    context->commandList->Close();
    return static_cast<jint>(hr);
  }
  context->recording = true;
  return S_OK;
}

/**
 * Ends a D3D12 frame started by Nd3d12BeginFrame: closes the command list,
 * submits it to the queue, and signals signalFenceValue on the shared fence
 * so OpenGL can wait for the output texture to be ready. The value is
 * remembered as lastSubmittedFenceValue for later idle waits.
 *
 * Returns S_OK on success, E_INVALIDARG when not recording or the fence value
 * is invalid, or the failing HRESULT (as jint) otherwise.
 */
JNIEXPORT jint JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12ExecuteFrame(
    JNIEnv *, jclass, jlong contextHandle, jlong signalFenceValue) {
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
  hr = context->queue->Signal(context->fence.Get(),
                              static_cast<uint64_t>(signalFenceValue));
  if (FAILED(hr)) {
    setHresultError("ID3D12CommandQueue::Signal", hr);
    return static_cast<jint>(hr);
  }
  context->lastSubmittedFenceValue = static_cast<uint64_t>(signalFenceValue);
  return S_OK;
}

/**
 * Blocks until all GPU work submitted so far (up to the last value signaled
 * by Nd3d12ExecuteFrame) has completed. Returns S_OK, E_INVALIDARG for a
 * null context, or E_FAIL if the fence wait itself failed.
 */
JNIEXPORT jint JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12WaitIdle(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  if (!context) {
    setError("The D3D12 interop context is null.");
    return E_INVALIDARG;
  }
  return waitForFence(context, context->lastSubmittedFenceValue) ? S_OK
                                                                 : E_FAIL;
}

/**
 * Returns the thread-local error message recorded by the last failed interop
 * call on this thread, or an empty string if none occurred.
 */
JNIEXPORT jstring JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetLastError(
    JNIEnv *env, jclass) {
  return env->NewStringUTF(g_lastError.c_str());
}
}

#endif
