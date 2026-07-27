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

## Renderer interop

`D3D12InteropAlgorithm` implements the renderer-facing half as a sibling to
`VulkanInteropAlgorithm`. The initial implementation deliberately uses a
single serial resource set:

1. query OpenGL's `GL_DEVICE_LUID_EXT` and create D3D12 on the matching DXGI
   adapter;
2. create five D3D12-owned shared committed textures for color, depth, motion
   vectors, exposure, and output;
3. import the resource handles into OpenGL with
   `GL_EXT_memory_object_win32`;
4. import a shared D3D12 timeline fence with `GL_EXT_semaphore_win32`;
5. preprocess the Minecraft inputs in OpenGL, signal ownership to D3D12,
   dispatch FFX API, signal ownership back to OpenGL, and flip the output into
   the normal renderer texture.

`FfxFSR4D3D12` supplies the FSR-specific context and dispatch descriptions.
The algorithm is registered as `fsr4_d3d12` on Windows when the required
OpenGL extensions are available. The signed DLL remains an explicit external
resource and must be selected by the user.

## Prototype boundary

The resource import, fence round trip, and FFX command recording/execution have
been validated in standalone smoke tests on an AMD Radeon RX 7900 XT. The
remaining work is integration testing inside Minecraft, including validation
of motion-vector/depth conventions and rendered image quality. A future
high-performance mode can add multiple in-flight resource sets after the
serial path is proven in game.

## Provider lifecycle

All FFX API creation descriptors are stored in the provider's private context
for the full FFX context lifetime, as required by AMD's API contract. The AMD
DLL remains loaded until `srDestroyUpscaleContext` destroys the FFX context.
