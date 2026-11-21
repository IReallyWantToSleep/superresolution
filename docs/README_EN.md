<div align="center"><img src="https://raw.githubusercontent.com/IReallyWantToSleep/superresolution/refs/heads/dev/modules/common/src/main/resources/assets/super_resolution/textures/gui/logo.png" width="130"/></div>
<div align="center">
<img src="https://img.shields.io/github/forks/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/stars/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/license/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/issues/187J3X1-114514/superresolution"/>
</div>

<div align="center">
<h1>Super Resolution</h1>
<a href="README_EN.md">English</a> / <a href="README_JP.md">日本語</a> / <a href="../README.md">简体中文</a>
</div>

---

Built-in super-resolution algorithms for Minecraft to improve performance and visual quality.

# Supported Algorithms

* FSR1
* FSR2
* FSR3
* SGSR2
* SGSR1
* DLSS
* XeSS

# Other Features

* Super-resolution support inside shader packs, [documentation](https://sr.187j3x1-114514.org/docs/Shaderpack-Interface-documentation/)

# Compatibility

* Sodium - Works fine
* Iris - Works fine
* Distant Horizons - Works fine
* Embeddium - Works fine
* Voxy - Works fine
* OptiFine - Not supported

## Minecraft 26.3: Iris on native Vulkan (Fabric)

Use the **Vulkan** build of Super Resolution, a RenderPearl-compatible Iris snapshot, matching Sodium, and Minecraft's Vulkan graphics backend. Enable a shader pack with an active `shaders/superresolution.json` or `superresolution.vN.json` profile. Select the upscaler in Super Resolution's settings. FSR 1 only needs color; AMD FSR and NVIDIA DLSS also need the pack's depth and motion vectors. DLSS requires a supported RTX GPU and its NGX feature library. The existing `dev-vulkan` safety fallback uses the FSR 2 provider when FSR 3 is selected on Minecraft's device.

To build against the bundled Iris, run `./gradlew :fabric:build -Pminecraft_version_config=26.3.vk`. To use local snapshots without replacing the bundled JAR:

```sh
./gradlew :fabric:build -Pminecraft_version_config=26.3.vk \
  -PirisVulkanJar=/absolute/path/to/iris.jar -PsodiumJar=/absolute/path/to/sodium.jar
```

Use `:fabric:runClient` with the same properties to start an isolated dev client in `runs/fabric-iris-vulkan`. For a smoke test, copy `modules/common/src/test/resources/iris-vulkan-smoke` into its `shaderpacks` directory, select the pack, and enter a world at 640×400. It upscales a 376×235 gradient into the same color target before a compute-only pass. A passing run fills the frame with the gradient; magenta outside it means the crop was not upscaled, and cyan indicates inconsistent scaling or jitter macros.

# Requirements

## System Requirements

* Windows 10/11 x64
* Linux x64

### About Android Devices

We will never support Android mobile devices.

## GPU Requirements

### Recommended

* OpenGL 4.3 or later
* OpenGL extensions: `GL_ARB_direct_state_access`, `GL_ARB_gl_spirv`, `GL_ARB_clear_texture`, `GL_EXT_memory_object`, `GL_EXT_semaphore`
* Vulkan 1.2 or later

### Minimum

* OpenGL 4.1 or later

# Found an Issue?

Open an [issue here](https://github.com/187J3X1-114514/superresolution/issues).

# Support Us

If SR has been helpful to you and you'd like to support our development, you're welcome to support us on [Afdian](https://ifdian.net/a/X187J3X1).

---

# License

* The mod itself uses GPL-3.0
* Native libraries use LGPL-3.0
* This software contains source code provided by NVIDIA Corporation
