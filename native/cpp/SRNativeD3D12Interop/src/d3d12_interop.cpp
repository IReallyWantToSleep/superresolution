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
#include <memory>
#include <mutex>
#include <new>
#include <optional>
#include <string>
#include <utility>

using Microsoft::WRL::ComPtr;

namespace {

constexpr uint32_t RESOURCE_COUNT = 5;
// Slot order of D3D12InteropContext::resources; mirrors the index constants
// on the Java side, which passes them to Nd3d12GetResource*.
[[maybe_unused]] constexpr uint32_t RESOURCE_INPUT_COLOR = 0;
[[maybe_unused]] constexpr uint32_t RESOURCE_INPUT_DEPTH = 1;
[[maybe_unused]] constexpr uint32_t RESOURCE_INPUT_MOTION_VECTORS = 2;
[[maybe_unused]] constexpr uint32_t RESOURCE_INPUT_EXPOSURE = 3;
[[maybe_unused]] constexpr uint32_t RESOURCE_OUTPUT_COLOR = 4;

// Values mirror SRSurfaceFormat / FfxApiSurfaceFormat.
constexpr int SR_FORMAT_R16G16B16A16_FLOAT = 4;
constexpr int SR_FORMAT_R8G8B8A8_UNORM = 10;
constexpr int SR_FORMAT_R11G11B10_FLOAT = 16;
constexpr int SR_FORMAT_R16G16_FLOAT = 18;
constexpr int SR_FORMAT_R16_FLOAT = 21;
constexpr int SR_FORMAT_R32_FLOAT = 28;

/**
 * Minimal RAII wrapper for a Win32 HANDLE, in the spirit of
 * wil::unique_handle (which this project does not depend on): the handle is
 * closed automatically on destruction, and the wrapper is move-only so a
 * raw void* can no longer be copied around or leaked by accident.
 */
class UniqueHandle {
public:
  UniqueHandle() = default;
  explicit UniqueHandle(HANDLE handle) : handle_(handle) {}
  ~UniqueHandle() { reset(); }

  UniqueHandle(const UniqueHandle &) = delete;
  UniqueHandle &operator=(const UniqueHandle &) = delete;

  UniqueHandle(UniqueHandle &&other) noexcept : handle_(other.release()) {}
  UniqueHandle &operator=(UniqueHandle &&other) noexcept {
    reset(other.release());
    return *this;
  }

  HANDLE get() const { return handle_; }
  explicit operator bool() const { return handle_ != nullptr; }

  // Frees the current handle and returns storage for an out-parameter
  // (e.g. ID3D12Device::CreateSharedHandle).
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

// Lifecycle of the command list across a frame: Idle outside of a frame,
// Recording between a successful Nd3d12BeginFrame and Nd3d12ExecuteFrame.
enum class FrameState { Idle, Recording };

/**
 * A COM interface pointer that is non-null by construction.
 * convention: the only way to obtain one is the checked from() factory, so
 * holders such as D3D12InteropContext can dereference their members without
 * any null check. Move-only, like the ownership it wraps.
 */
template <typename T> class ComObj {
public:
  ComObj(const ComObj &) = delete;
  ComObj &operator=(const ComObj &) = delete;
  ComObj(ComObj &&) noexcept = default;
  ComObj &operator=(ComObj &&) noexcept = default;

  /**
   * Checked conversion from a nullable ComPtr: returns an engaged NonNullCom
   * when ptr holds an interface, std::nullopt otherwise.
   */
  static std::optional<ComObj> from(ComPtr<T> ptr) {
    if (!ptr) {
      return std::nullopt;
    }
    return ComObj(std::move(ptr));
  }

  T *get() const { return ptr_.Get(); }
  T *operator->() const { return ptr_.Get(); }

  // Returns an additional owning reference (AddRef), mirroring Rc::clone.
  ComPtr<T> share() const { return ptr_; }

private:
  explicit ComObj(ComPtr<T> ptr) : ptr_(std::move(ptr)) {}
  ComPtr<T> ptr_;
};

thread_local std::string g_lastError;

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
 * Runs a COM creation call (one taking a T** out-parameter and returning an
 * HRESULT) and returns the created interface as a ComObj.
 */
template <typename T, typename Create>
std::optional<ComObj<T>> createCom(const char *operation, Create &&create) {
  ComPtr<T> ptr;
  const HRESULT hr = create(ptr.ReleaseAndGetAddressOf());
  if (FAILED(hr)) {
    setHresultError(operation, hr);
    return std::nullopt;
  }
  return ComObj<T>::from(std::move(ptr));
}

/**
 * A fully-initialized shared interop texture: the factory createSharedTexture
 * only produces one once the resource, its NT shared handle, and the
 * allocation size all exist, so the half-initialized state is not
 * representable.
 */
struct SharedTexture {
  ComObj<ID3D12Resource> resource;
  UniqueHandle sharedHandle;
  uint64_t allocationSize = 0;
};

struct D3D12InteropContext {
  /**
   * All COM members are ComObj: a context only exists once every object
   * was created successfully, so the frame and wait functions can dereference
   * them unconditionally — validity is enforced by the type, not by
   * convention.
   */
  D3D12InteropContext(ComObj<IDXGIAdapter1> adapter,
                      ComObj<ID3D12Device> device,
                      ComObj<ID3D12CommandQueue> queue,
                      ComObj<ID3D12CommandAllocator> commandAllocator,
                      ComObj<ID3D12GraphicsCommandList> commandList,
                      ComObj<ID3D12Fence> fence,
                      UniqueHandle fenceSharedHandle, UniqueHandle fenceEvent,
                      std::array<SharedTexture, RESOURCE_COUNT> resources)
      : adapter(std::move(adapter)), device(std::move(device)),
        queue(std::move(queue)), commandAllocator(std::move(commandAllocator)),
        commandList(std::move(commandList)), fence(std::move(fence)),
        fenceSharedHandle(std::move(fenceSharedHandle)),
        fenceEvent(std::move(fenceEvent)), resources(std::move(resources)) {}

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

/**
 * Process-wide D3D12 device cache with its mutex bundled in, in the spirit
 * of Rust's Mutex<T>: the data and the lock protecting it live in one object,
 * so the cached device can only be touched through locked access.
 */
class SharedDevice {
public:
  /**
   * Returns the cached device when it matches adapterLuid and has not been
   * removed; otherwise creates a new device on the given adapter and caches
   * it. On creation failure the thread-local error string is set and an
   * empty optional is returned.
   */
  std::optional<ComObj<ID3D12Device>>
  getOrCreate(const ComObj<IDXGIAdapter1> &adapter, uint64_t adapterLuid) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (device_ && luid_ == adapterLuid &&
        SUCCEEDED(device_->GetDeviceRemovedReason())) {
      return ComObj<ID3D12Device>::from(device_);
    }
    device_.Reset();
    luid_ = 0;
    auto device =
        createCom<ID3D12Device>("D3D12CreateDevice", [&](ID3D12Device **pp) {
          return D3D12CreateDevice(adapter.get(), D3D_FEATURE_LEVEL_12_0,
                                   IID_PPV_ARGS(pp));
        });
    if (!device) {
      return std::nullopt;
    }
    device_ = device->share();
    luid_ = adapterLuid;
    return device;
  }

private:
  std::mutex mutex_;
  ComPtr<ID3D12Device> device_;
  uint64_t luid_ = 0;
};

SharedDevice g_sharedDevice;

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
 * Creates one shared interop texture as a committed UAV-capable Texture2D on
 * the DEFAULT heap with D3D12_HEAP_FLAG_SHARED, records its driver allocation
 * size, and exports an NT shared handle so the resource can be opened from
 * OpenGL (e.g. via GL_EXT_memory_object). Returns the complete SharedTexture,
 * or an empty optional on failure with the thread-local error string set —
 * the half-initialized state is not representable.
 */
std::optional<SharedTexture>
createSharedTexture(const ComObj<ID3D12Device> &device, uint32_t width,
                    uint32_t height, DXGI_FORMAT format) {
  if (width == 0 || height == 0 || format == DXGI_FORMAT_UNKNOWN) {
    setError("Invalid shared D3D12 texture description.");
    return std::nullopt;
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

  auto resource = createCom<ID3D12Resource>(
      "ID3D12Device::CreateCommittedResource", [&](ID3D12Resource **pp) {
        return device->CreateCommittedResource(
            &heapProperties, D3D12_HEAP_FLAG_SHARED, &resourceDesc,
            D3D12_RESOURCE_STATE_COMMON, nullptr, IID_PPV_ARGS(pp));
      });
  if (!resource) {
    return std::nullopt;
  }

  const D3D12_RESOURCE_ALLOCATION_INFO allocationInfo =
      device->GetResourceAllocationInfo(0, 1, &resourceDesc);
  if (allocationInfo.SizeInBytes == 0 ||
      allocationInfo.SizeInBytes == UINT64_MAX) {
    setError("D3D12 returned an invalid shared resource allocation size.");
    return std::nullopt;
  }

  UniqueHandle sharedHandle;
  const HRESULT hr =
      device->CreateSharedHandle(resource->get(), nullptr, GENERIC_ALL,
                                 nullptr, sharedHandle.put());
  if (FAILED(hr)) {
    setHresultError("ID3D12Device::CreateSharedHandle(resource)", hr);
    return std::nullopt;
  }
  return SharedTexture{std::move(*resource), std::move(sharedHandle),
                       allocationInfo.SizeInBytes};
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
      context->fence->SetEventOnCompletion(value, context->fenceEvent.get());
  if (FAILED(hr)) {
    setHresultError("ID3D12Fence::SetEventOnCompletion", hr);
    return false;
  }
  if (WaitForSingleObject(context->fenceEvent.get(), INFINITE) !=
      WAIT_OBJECT_0) {
    setError("Waiting for the D3D12 interop fence failed.");
    return false;
  }
  return true;
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
 * Each step yields a checked object (ComObj / SharedTexture); the context
 * itself is only constructed once every step has succeeded, and intermediate
 * objects clean up after themselves via RAII on any failure.
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

  auto factory = createCom<IDXGIFactory6>(
      "CreateDXGIFactory1",
      [](IDXGIFactory6 **pp) { return CreateDXGIFactory1(IID_PPV_ARGS(pp)); });
  if (!factory) {
    return 0;
  }

  std::optional<ComObj<IDXGIAdapter1>> adapter;
  for (UINT index = 0;; ++index) {
    ComPtr<IDXGIAdapter1> candidate;
    const HRESULT hr = (*factory)->EnumAdapters1(index, &candidate);
    if (hr == DXGI_ERROR_NOT_FOUND) {
      break;
    }
    if (FAILED(hr)) {
      setHresultError("IDXGIFactory1::EnumAdapters1", hr);
      return 0;
    }
    DXGI_ADAPTER_DESC1 desc = {};
    if (SUCCEEDED(candidate->GetDesc1(&desc)) &&
        (desc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) == 0 &&
        sameLuid(desc.AdapterLuid, static_cast<uint64_t>(adapterLuid))) {
      adapter = ComObj<IDXGIAdapter1>::from(std::move(candidate));
      break;
    }
  }
  if (!adapter) {
    setError("No D3D12 adapter matches the OpenGL device LUID.");
    return 0;
  }

  auto device =
      g_sharedDevice.getOrCreate(*adapter, static_cast<uint64_t>(adapterLuid));
  if (!device) {
    return 0;
  }

  D3D12_COMMAND_QUEUE_DESC queueDesc = {};
  queueDesc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
  queueDesc.Priority = D3D12_COMMAND_QUEUE_PRIORITY_NORMAL;
  queueDesc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;
  auto queue = createCom<ID3D12CommandQueue>(
      "ID3D12Device::CreateCommandQueue", [&](ID3D12CommandQueue **pp) {
        return (*device)->CreateCommandQueue(&queueDesc, IID_PPV_ARGS(pp));
      });
  if (!queue) {
    return 0;
  }

  auto commandAllocator = createCom<ID3D12CommandAllocator>(
      "ID3D12Device::CreateCommandAllocator", [&](ID3D12CommandAllocator **pp) {
        return (*device)->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
                                                 IID_PPV_ARGS(pp));
      });
  if (!commandAllocator) {
    return 0;
  }

  auto commandList = createCom<ID3D12GraphicsCommandList>(
      "ID3D12Device::CreateCommandList", [&](ID3D12GraphicsCommandList **pp) {
        return (*device)->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
                                            commandAllocator->get(), nullptr,
                                            IID_PPV_ARGS(pp));
      });
  if (!commandList) {
    return 0;
  }
  HRESULT hr = (*commandList)->Close();
  if (FAILED(hr)) {
    setHresultError("ID3D12GraphicsCommandList::Close(initial)", hr);
    return 0;
  }

  auto fence = createCom<ID3D12Fence>(
      "ID3D12Device::CreateFence", [&](ID3D12Fence **pp) {
        return (*device)->CreateFence(0, D3D12_FENCE_FLAG_SHARED,
                                      IID_PPV_ARGS(pp));
      });
  if (!fence) {
    return 0;
  }

  UniqueHandle fenceEvent(CreateEventW(nullptr, FALSE, FALSE, nullptr));
  if (!fenceEvent) {
    setError("CreateEventW failed for the D3D12 interop fence.");
    return 0;
  }

  UniqueHandle fenceSharedHandle;
  hr = (*device)->CreateSharedHandle(fence->get(), nullptr, GENERIC_ALL,
                                     nullptr, fenceSharedHandle.put());
  if (FAILED(hr)) {
    setHresultError("ID3D12Device::CreateSharedHandle(fence)", hr);
    return 0;
  }

  auto inputColor =
      createSharedTexture(*device, static_cast<uint32_t>(renderWidth),
                          static_cast<uint32_t>(renderHeight), dxgiColorFormat);
  if (!inputColor) {
    return 0;
  }
  auto inputDepth =
      createSharedTexture(*device, static_cast<uint32_t>(renderWidth),
                          static_cast<uint32_t>(renderHeight),
                          DXGI_FORMAT_R32_FLOAT);
  if (!inputDepth) {
    return 0;
  }
  auto motionVectors = createSharedTexture(
      *device, static_cast<uint32_t>(renderWidth),
      static_cast<uint32_t>(renderHeight), DXGI_FORMAT_R16G16_FLOAT);
  if (!motionVectors) {
    return 0;
  }
  auto exposure = createSharedTexture(*device, 1, 1, DXGI_FORMAT_R16_FLOAT);
  if (!exposure) {
    return 0;
  }
  auto outputColor =
      createSharedTexture(*device, static_cast<uint32_t>(outputWidth),
                          static_cast<uint32_t>(outputHeight), dxgiColorFormat);
  if (!outputColor) {
    return 0;
  }

  // Every object was created successfully; only now does the context exist,
  // with the validity of all its members enforced by ComObj.
  auto context = std::unique_ptr<D3D12InteropContext>(
      new (std::nothrow) D3D12InteropContext(
          std::move(*adapter), std::move(*device), std::move(*queue),
          std::move(*commandAllocator), std::move(*commandList),
          std::move(*fence), std::move(fenceSharedHandle),
          std::move(fenceEvent),
          {std::move(*inputColor), std::move(*inputDepth),
           std::move(*motionVectors), std::move(*exposure),
           std::move(*outputColor)}));
  if (!context) {
    setError("Could not allocate the D3D12 interop context.");
    return 0;
  }

  return static_cast<jlong>(reinterpret_cast<intptr_t>(context.release()));
}

/**
 * Destroys a context created by Nd3d12CreateContext: waits for the last
 * submitted fence value so no in-flight GPU work references the resources,
 * then frees the context, whose members (COM resources and Win32 handles)
 * are released automatically by RAII. A null handle is a no-op.
 */
JNIEXPORT void JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12DestroyContext(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  if (!context) {
    return;
  }
  waitForFence(context, context->lastSubmittedFenceValue);
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
                       reinterpret_cast<intptr_t>(context->device.get()))
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
                       reinterpret_cast<intptr_t>(context->commandList.get()))
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
                       reinterpret_cast<intptr_t>(texture->resource.get()))
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
                       reinterpret_cast<intptr_t>(texture->sharedHandle.get()))
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
                       reinterpret_cast<intptr_t>(context->fenceSharedHandle.get()))
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
  if (context->frameState == FrameState::Recording) {
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
  hr = context->commandList->Reset(context->commandAllocator.get(), nullptr);
  if (FAILED(hr)) {
    setHresultError("ID3D12GraphicsCommandList::Reset", hr);
    return static_cast<jint>(hr);
  }
  hr = context->queue->Wait(context->fence.get(),
                            static_cast<uint64_t>(waitFenceValue));
  if (FAILED(hr)) {
    setHresultError("ID3D12CommandQueue::Wait", hr);
    context->commandList->Close();
    return static_cast<jint>(hr);
  }
  context->frameState = FrameState::Recording;
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
  if (!context || context->frameState != FrameState::Recording ||
      signalFenceValue <= 0) {
    setError("Invalid D3D12 execute-frame state or fence value.");
    return E_INVALIDARG;
  }

  HRESULT hr = context->commandList->Close();
  context->frameState = FrameState::Idle;
  if (FAILED(hr)) {
    setHresultError("ID3D12GraphicsCommandList::Close", hr);
    return static_cast<jint>(hr);
  }

  ID3D12CommandList *commandLists[] = {context->commandList.get()};
  context->queue->ExecuteCommandLists(1, commandLists);
  hr = context->queue->Signal(context->fence.get(),
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
