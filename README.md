<div align="center"><img src="common/src/main/resources/assets/super_resolution/logo.png" width="256"/></div>
<div align="center"><img src="https://img.shields.io/github/forks/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/stars/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/license/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/issues/187J3X1-114514/superresolution"/></div>

# Super Resolution

在Minecraft中内置超分辨率算法，以提升性能/画质

# 支持的算法

* FSR1
* FSR2 (正在开发，缺少运动矢量输入)

# 兼容性

* 钠 正常工作
* Iris 正常工作
* 遥远的地平线(Distant Horizons) 正常工作
* Embeddium 正常工作
* OptiFine 没有测试

# 要求

## 系统要求

* Windows
* Linux
* 不支持MacOS系统

## 显卡要求

### 通用

* 显卡支持OpenGL版本 >= 4.3
* 显卡支持Vulkan版本 >= 1.2 (非必须，因为功能没做完)

### FSR1

* 无特殊要求

### FSR2

* 显卡支持OpenGL版本 >= 4.5
* 显卡支持支持OpenGL扩展GL_KHR_shader_subgroup

*注：以上的要求NVIDIA GTX 750全部满足，只要你不是用老掉牙的显卡 (或是老版本驱动) 应该都能支持*

# 已知问题

* 使用FSR2时游戏有可能会没有画面

# 有问题？

发现Bug，游戏崩溃，想要支持其他游戏版本（仅限1.16及以上，加载器仅限Forge,Fabric,NeoForge）

在[这里](https://github.com/187J3X1-114514/superresolution/issues)打开一个issues

# 构建

首先编译[c++依赖库](https://github.com/187J3X1-114514/fsr2_opengl_java)，然后把生成的文件复制到common/src/main/resources/lib
_(非必要)_

打开你的终端，运行，然后build_jars就是模组文件

```shell
git clone https://github.com/187J3X1-114514/superresolution
cd superresolution
python script/buildAll.py
```

# 可能会有的功能

* 实现Vulkan与OpenGL共享纹理 (真实现了的话，直接支持FSR3,DLSS)
* 生成运动矢量

---

## 星星历史图
[![Stargazers over time](https://starchart.cc/187J3X1-114514/superresolution.svg?variant=adaptive)](https://starchart.cc/187J3X1-114514/superresolution)


