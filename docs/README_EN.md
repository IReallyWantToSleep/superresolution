<div align="center"><img src="https://raw.githubusercontent.com/187J3X1-114514/superresolution/refs/heads/multi-version/common/src/main/resources/assets/super_resolution/logo.png" width="256"/></div>
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

Built-in super-resolution algorithms for **Minecraft**, improving performance and visual quality.

# Supported Algorithms

* FSR1
* FSR2
* SGSR2
* SGSR1
* DLSS
* XeSS
* FSR3

# Other Features

* Super-resolution support inside shader packs, [documentation](https://github.com/187J3X1-114514/superresolution/wiki/Shaderpack-Interface-documentation)

# Compatibility

* Sodium - Works fine
* Iris - Works fine
* Distant Horizons - Works fine
* Embeddium - Works fine
* Voxy - Works fine
* OptiFine - Not tested

# Requirements

## System Requirements

* Windows 10/11 x64
* Linux x64
* macOS Arm64 (planned)

### About Android Devices

Currently, running on Android is **not supported**. Native Android libraries are provided (but cannot be loaded correctly).

In addition, compute shaders, DSA, and SpirV shader binaries often do not work correctly across various Android OpenGL translation layers. However, **SuperResolution can function without these features where possible**.

## GPU Requirements

### Recommended

* OpenGL 4.3 or later
* OpenGL extensions: `GL_ARB_direct_state_access`, `GL_ARB_gl_spirv`, `GL_ARB_clear_texture`
* Vulkan 1.2 or later

### Minimum

* OpenGL 4.1 or later

# Found an Issue?

If you:

* Discovered a bug
* Encountered a game crash
* Want support for other Minecraft versions

*Note: Only 1.18 and above, loader support limited to Forge, Fabric, NeoForge. Ports depend on feasibility.*

Please open an [issue here](https://github.com/187J3X1-114514/superresolution/issues).

# Building

First, build the native C++ dependencies by running the `native:buildNative` task.

> **Note:** Windows requires MinGW and CMake. For other requirements, see [here](../native/README.md).
>
Then open your terminal and run, the generated `build_jars` will contain your mod files.

```shell
git clone https://github.com/187J3X1-114514/superresolution
cd superresolution
./gradlew buildAllVersions
```

---

# License

* The mod itself uses GPL-3.0
* Native libraries use MIT
* This software contains source code provided by NVIDIA Corporation

## Stargazer History

[![Stargazers over time](https://starchart.cc/187J3X1-114514/superresolution.svg?variant=adaptive)](https://starchart.cc/187J3X1-114514/superresolution)
