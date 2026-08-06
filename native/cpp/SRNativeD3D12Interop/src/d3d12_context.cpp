#include "d3d12_interop_internal.h"

#if defined(ON_WIN64)

#include <cstdio>
#include <mutex>
#include <new>

namespace sr::d3d12 {
namespace {

// Values mirror SRSurfaceFormat / FfxApiSurfaceFormat on the Java and
// FidelityFX sides of the boundary.
constexpr int SR_FORMAT_R16G16B16A16_FLOAT = 4;
constexpr int SR_FORMAT_R8G8B8A8_UNORM = 10;
constexpr int SR_FORMAT_R11G11B10_FLOAT = 16;
constexpr int SR_FORMAT_R16G16_FLOAT = 18;
constexpr int SR_FORMAT_R16_FLOAT = 21;
constexpr int SR_FORMAT_R32_FLOAT = 28;

thread_local std::string g_lastError;

/**
 * Process-wide D3D12 device cache with its synchronization bundled in.
 *
 * A device is reused only while it belongs to the requested adapter and has
 * not been removed. Keeping the mutex private prevents unsynchronized access
 * to the cached COM pointer and its associated LUID.
 */
class SharedDevice {
public:
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
 * Compares a DXGI LUID with the packed 64-bit representation passed through
 * JNI: LowPart occupies the low 32 bits and HighPart the high 32 bits.
 */
bool sameLuid(const LUID &left, uint64_t right) {
  const uint64_t leftValue =
      static_cast<uint64_t>(left.LowPart) |
      (static_cast<uint64_t>(static_cast<uint32_t>(left.HighPart)) << 32);
  return leftValue == right;
}

/**
 * Maps an SRSurfaceFormat/FfxApiSurfaceFormat integer to DXGI_FORMAT.
 * Unsupported values deliberately map to DXGI_FORMAT_UNKNOWN so context
 * construction rejects them before allocating GPU resources.
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
 * Creates one committed, UAV-capable Texture2D on a shared DEFAULT heap.
 *
 * The returned value contains the resource, its exported NT handle, and the
 * driver-reported allocation size needed by OpenGL memory import. Any failure
 * releases intermediate objects automatically and records an error.
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
  const HRESULT hr = device->CreateSharedHandle(
      resource->get(), nullptr, GENERIC_ALL, nullptr, sharedHandle.put());
  if (FAILED(hr)) {
    setHresultError("ID3D12Device::CreateSharedHandle(resource)", hr);
    return std::nullopt;
  }

  return SharedTexture{std::move(*resource), std::move(sharedHandle),
                       allocationInfo.SizeInBytes};
}

/**
 * Finds the non-software DXGI adapter whose LUID matches the OpenGL device.
 * Matching adapters is required for D3D12/OpenGL resource sharing.
 */
std::optional<ComObj<IDXGIAdapter1>> findAdapter(uint64_t adapterLuid) {
  auto factory =
      createCom<IDXGIFactory6>("CreateDXGIFactory1", [](IDXGIFactory6 **pp) {
        return CreateDXGIFactory1(IID_PPV_ARGS(pp));
      });
  if (!factory) {
    return std::nullopt;
  }

  for (UINT index = 0;; ++index) {
    ComPtr<IDXGIAdapter1> candidate;
    const HRESULT hr = (*factory)->EnumAdapters1(index, &candidate);
    if (hr == DXGI_ERROR_NOT_FOUND) {
      break;
    }
    if (FAILED(hr)) {
      setHresultError("IDXGIFactory1::EnumAdapters1", hr);
      return std::nullopt;
    }

    DXGI_ADAPTER_DESC1 desc = {};
    if (SUCCEEDED(candidate->GetDesc1(&desc)) &&
        (desc.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) == 0 &&
        sameLuid(desc.AdapterLuid, adapterLuid)) {
      return ComObj<IDXGIAdapter1>::from(std::move(candidate));
    }
  }

  setError("No D3D12 adapter matches the OpenGL device LUID.");
  return std::nullopt;
}

} // namespace

// Error messages are thread-local because Java queries the error on the same
// thread immediately after a failed native operation.
void clearError() { g_lastError.clear(); }

void setError(const char *message) {
  g_lastError = message ? message : "Unknown D3D12 interop error.";
}

void setHresultError(const char *operation, HRESULT hr) {
  char buffer[256] = {};
  std::snprintf(buffer, sizeof(buffer), "%s failed with HRESULT 0x%08lX.",
                operation, static_cast<unsigned long>(hr));
  setError(buffer);
}

const std::string &lastError() { return g_lastError; }

/**
 * Builds a fully usable interop context transactionally.
 *
 * The adapter and cached device are followed by the direct command queue,
 * allocator/list pair, shared fence and event, then the five textures in Java
 * resource-index order:
 *
 *   input color, input depth, motion vectors, exposure, output color.
 *
 * The context itself is allocated only after every prerequisite succeeds, so
 * cleanup on any earlier return is handled entirely by RAII.
 */
std::unique_ptr<D3D12InteropContext>
createContext(uint64_t adapterLuid, uint32_t renderWidth, uint32_t renderHeight,
              uint32_t outputWidth, uint32_t outputHeight, int colorFormat) {
  clearError();
  if (adapterLuid == 0 || renderWidth == 0 || renderHeight == 0 ||
      outputWidth == 0 || outputHeight == 0) {
    setError("Invalid D3D12 interop context dimensions or adapter LUID.");
    return nullptr;
  }

  const DXGI_FORMAT dxgiColorFormat = toDxgiFormat(colorFormat);
  if (dxgiColorFormat == DXGI_FORMAT_UNKNOWN) {
    setError("The configured internal color format is not supported by D3D12 "
             "interop.");
    return nullptr;
  }

  auto adapter = findAdapter(adapterLuid);
  if (!adapter) {
    return nullptr;
  }

  auto device = g_sharedDevice.getOrCreate(*adapter, adapterLuid);
  if (!device) {
    return nullptr;
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
    return nullptr;
  }

  auto commandAllocator = createCom<ID3D12CommandAllocator>(
      "ID3D12Device::CreateCommandAllocator", [&](ID3D12CommandAllocator **pp) {
        return (*device)->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT,
                                                 IID_PPV_ARGS(pp));
      });
  if (!commandAllocator) {
    return nullptr;
  }

  auto commandList = createCom<ID3D12GraphicsCommandList>(
      "ID3D12Device::CreateCommandList", [&](ID3D12GraphicsCommandList **pp) {
        return (*device)->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
                                            commandAllocator->get(), nullptr,
                                            IID_PPV_ARGS(pp));
      });
  if (!commandList) {
    return nullptr;
  }
  HRESULT hr = (*commandList)->Close();
  if (FAILED(hr)) {
    setHresultError("ID3D12GraphicsCommandList::Close(initial)", hr);
    return nullptr;
  }

  auto fence = createCom<ID3D12Fence>(
      "ID3D12Device::CreateFence", [&](ID3D12Fence **pp) {
        return (*device)->CreateFence(0, D3D12_FENCE_FLAG_SHARED,
                                      IID_PPV_ARGS(pp));
      });
  if (!fence) {
    return nullptr;
  }

  UniqueHandle fenceEvent(CreateEventW(nullptr, FALSE, FALSE, nullptr));
  if (!fenceEvent) {
    setError("CreateEventW failed for the D3D12 interop fence.");
    return nullptr;
  }

  UniqueHandle fenceSharedHandle;
  hr = (*device)->CreateSharedHandle(fence->get(), nullptr, GENERIC_ALL,
                                     nullptr, fenceSharedHandle.put());
  if (FAILED(hr)) {
    setHresultError("ID3D12Device::CreateSharedHandle(fence)", hr);
    return nullptr;
  }

  auto inputColor =
      createSharedTexture(*device, renderWidth, renderHeight, dxgiColorFormat);
  if (!inputColor) {
    return nullptr;
  }
  auto inputDepth = createSharedTexture(*device, renderWidth, renderHeight,
                                        DXGI_FORMAT_R32_FLOAT);
  if (!inputDepth) {
    return nullptr;
  }
  auto motionVectors = createSharedTexture(*device, renderWidth, renderHeight,
                                           DXGI_FORMAT_R16G16_FLOAT);
  if (!motionVectors) {
    return nullptr;
  }
  auto exposure = createSharedTexture(*device, 1, 1, DXGI_FORMAT_R32_FLOAT);
  if (!exposure) {
    return nullptr;
  }
  auto outputColor =
      createSharedTexture(*device, outputWidth, outputHeight, dxgiColorFormat);
  if (!outputColor) {
    return nullptr;
  }

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
  }
  return context;
}

} // namespace sr::d3d12

#endif
