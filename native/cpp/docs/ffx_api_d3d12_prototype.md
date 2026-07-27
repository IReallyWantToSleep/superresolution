# SRAPI Direct3D 12 / AMD FFX API prototype

This branch adds the native API foundation needed to run AMD's current
signed-DLL upscaler through Direct3D 12.

## Why this uses FFX API

FSR 4.1 is exposed by AMD FSR SDK 2.x through the FFX API and the signed
`amd_fidelityfx_upscaler_dx12.dll`. The older FidelityFX SDK 1.x backend
compiled shaders directly into the application. Reviving that deleted DX12
backend would provide an FSR 2/3 implementation, but it would not be the
correct integration path for the current FSR 4.1 provider.

The adapter in `SRNativeFSR/src/ffx_api_upscale.cpp` therefore:

1. loads AMD's signed upscaler DLL dynamically;
2. resolves `ffxCreateContext`, `ffxDestroyContext`, `ffxQuery`, and
   `ffxDispatch`;
3. translates SRAPI context and dispatch descriptions to the FFX API ABI; and
4. exposes the adapter as provider `SR_MODULES_FFX_API_UPSCALE_ID`.

The signed AMD DLL is not copied into this repository. Obtain it from an
official AMD FSR SDK release and pass its absolute path through the
`ffxApiDllPath` context string parameter. If the parameter is omitted, the
provider looks for `amd_fidelityfx_upscaler_dx12.dll` in the process' secure
DLL search directories.

## D3D12 SRAPI handles

The cross-platform SRAPI ABI does not include `d3d12.h`. It carries:

- `SRD3D12DeviceInfo.device` as an opaque `ID3D12Device*`;
- `SRCommandBufferD3D12.commandList` as an opaque
  `ID3D12GraphicsCommandList*`; and
- `SRTextureResource.handle` as an opaque `ID3D12Resource*`.

`SRTextureResource.state` describes the current resource state. It uses the
same bit values as the FFX API resource-state enum.

The Java/JNI layer mirrors those values with `long` native addresses. Raw
D3D12 resources can be created with the `SRTextureResource(long, description,
states)` constructor.

## Prototype boundary

This change makes SRAPI and its FSR provider D3D12-capable, but Minecraft still
renders through OpenGL or the project's Vulkan path. A complete in-game FSR
4.1 implementation additionally needs a Windows graphics interop layer that:

- creates a D3D12 device on the same physical adapter;
- shares the color, depth, motion-vector, exposure, and output resources with
  the renderer;
- translates resource layouts/states correctly; and
- synchronizes OpenGL/Vulkan work with the D3D12 command queue and fences.

That interop work belongs above SRAPI and should be implemented as a sibling to
the existing `VulkanInteropAlgorithm`; it is intentionally not hidden inside
the FFX provider.

## Provider lifecycle

All FFX API creation descriptors are stored in the provider's private context
for the full FFX context lifetime, as required by AMD's API contract. The AMD
DLL remains loaded until `srDestroyUpscaleContext` destroys the FFX context.
