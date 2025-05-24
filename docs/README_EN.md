<div align="center"><img src="common/src/main/resources/assets/super_resolution/logo.png" width="256"/></div>
<div align="center"><img src="https://img.shields.io/github/forks/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/stars/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/license/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/issues/187J3X1-114514/superresolution"/></div>

<div align="center">
<h1>Super Resolution</h1>
<span>English</span> <a href="../README.md">简体中文</a>
</div>

----

Built-in super resolution algorithms in Minecraft to enhance performance/graphics quality.

# Supported Algorithms

* FSR1
* FSR2 (Ported from C++ to Java, may have slight differences from the original)
* SGSR2
* SGSR1
* NIS (Under development)

# Compatibility

* Sodium - Works correctly
* Iris - Works correctly
* Distant Horizons - Works correctly
* Embeddium - Works correctly
* OptiFine - Not tested

# Requirements

## System Requirements

* Windows 10/11 x64
* Linux x64
* Currently unsupported on mobile devices (Android native libraries are provided, but features like compute shaders,
  DSA, and SpirV shader binaries may not function properly)
* **Will never support** macOS

## GPU Requirements

* OpenGL version >= 4.3
* OpenGL extensions: `GL_ARB_direct_state_access`, `GL_ARB_gl_spirv`
* Vulkan version >= 1.2 (Optional)

# Found an Issue?

Report bugs, crashes, or request support for other game versions (only 1.18+; loaders limited to Forge, Fabric,
NeoForge. Porting depends on difficulty) by opening an
issue [here](https://github.com/187J3X1-114514/superresolution/issues).

# Building

First, compile the C++ native libraries by running the `native:buildNative` task.
> Note: Windows requires MinGW and CMake. See full requirements [here](native/README.md).

Open your terminal and run the following. The built JARs will be in `build_jars`:

```shell
git clone https://github.com/187J3X1-114514/superresolution
cd superresolution
python script/buildAll.py
```

# Potential Future Features

* Implementing Vulkan-OpenGL texture sharing (if achieved, will enable direct support for FSR3/DLSS)

---

## Stargazers Over Time

[![Stargazers over time](https://starchart.cc/187J3X1-114514/superresolution.svg?variant=adaptive)](https://starchart.cc/187J3X1-114514/superresolution)