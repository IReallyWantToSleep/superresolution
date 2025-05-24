<div align="center"><img src="https://raw.githubusercontent.com/187J3X1-114514/superresolution/refs/heads/multi-version/common/src/main/resources/assets/super_resolution/logo.png" width="256"/></div>
<div align="center"><img src="https://img.shields.io/github/forks/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/stars/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/license/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/issues/187J3X1-114514/superresolution"/></div>

<div align="center">
<h1>Super Resolution</h1>
<a href="docs/README_EN.md">English</a> <span>简体中文</span>
</div>

----

在Minecraft中内置超分辨率算法，以提升性能/画质

# 支持的算法

* FSR1
* FSR2 (从C++移植到Java，可能与原版有略微不同)
* SGSR2
* SGSR1
* NIS (正在开发)

# 兼容性

* 钠 正常工作
* Iris 正常工作
* 遥远的地平线(Distant Horizons) 正常工作
* Embeddium 正常工作
* OptiFine 没有测试

# 要求

## 系统要求

* Windows 10/11 x64
* Linux x64
* 目前不支持在移动设备上运行，但提供安卓的本机库，安卓端的计算着色器，DSA，SpirV着色器二进制等功能工作不正常
* 永远不会支持MacOS系统

## 显卡要求

* 支持OpenGL版本 >= 4.3
* 支持OpenGL扩展 `GL_ARB_direct_state_access` `GL_ARB_gl_spirv`
* 支持Vulkan版本 >= 1.2 (非必须)

# 有问题？

发现Bug，游戏崩溃，想要支持其他游戏版本（仅限1.18及以上，加载器仅限Forge,Fabric,NeoForge，视移植难度进行移植）

在[这里](https://github.com/187J3X1-114514/superresolution/issues)打开一个issues

# 构建

首先编译C++依赖库，运行`native:buildNative`任务即可
> 注：Windows平台需要MinGW和Cmake，其它要求看[这里](native/README.md)
>
打开你的终端，运行，然后build_jars就是模组文件

```shell
git clone https://github.com/187J3X1-114514/superresolution
cd superresolution
python script/buildAll.py
```

# 可能会有的功能

* 实现Vulkan与OpenGL共享纹理 (真实现了的话，直接支持FSR3,DLSS)

---

## 星星历史图

[![Stargazers over time](https://starchart.cc/187J3X1-114514/superresolution.svg?variant=adaptive)](https://starchart.cc/187J3X1-114514/superresolution)


