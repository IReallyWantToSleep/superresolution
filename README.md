<div align="center"><img src="common/src/main/resources/assets/super_resolution/logo.png" width="256"/></div>
<div align="center"><img src="https://img.shields.io/github/forks/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/stars/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/license/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/issues/187J3X1-114514/superresolution"/></div>

# Super Resolution

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

* Windows
* Linux ~~(即将去除)~~
* 不支持MacOS系统
* 不支持在移动设备上运行

## 显卡要求

* 显卡支持OpenGL版本 >= 4.3
* 显卡支持Vulkan版本 >= 1.2 (非必须)

# 有问题？

发现Bug，游戏崩溃，想要支持其他游戏版本（仅限1.16及以上，加载器仅限Forge,Fabric,NeoForge）

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

* 可能 _(注意是可能)_ 用Java重写FSR2(FSR3)的部分代码，目前FSR2部分完成，基本可用，但部分功能由于不需要所以没有移植
* 实现Vulkan与OpenGL共享纹理 (真实现了的话，直接支持FSR3,DLSS)

---

## 星星历史图

[![Stargazers over time](https://starchart.cc/187J3X1-114514/superresolution.svg?variant=adaptive)](https://starchart.cc/187J3X1-114514/superresolution)


