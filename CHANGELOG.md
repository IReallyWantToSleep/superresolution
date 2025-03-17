# 0.0.5a1

警告：该版本国际化尚不完善 Warning: The internationalization of this version is not yet complete

* 优化了空值处理
* 实验性的SGSR
* 实验性的移动设备支持
* 美化信息界面 ~~_(指打断动画)_~~
* 在信息界面添加了更多内容
* 改进Vulkan兼容性检测
* 修复issues/22
* 修复issues/24

# 0.0.4a3

* 修复配置文件不存在时游戏崩溃的bug

# 0.0.4a2

* 支持1.20.4Fabric
* 支持1.21.4Fabric/NeoForge
* 重写帧缓冲区部分代码
* 更改配置界面滚动条样式
* 优化配置界面应用按钮逻辑
* 修复与沉浸工程的渲染bug
* 修复与野蛮渲染剔除的mixin冲突
* 修复issues/19

# 0.0.4a1

* 重写游戏画面捕获代码
* 重写配置系统
* 重写配置界面
* 修复issues/14
* 优化FSR1性能
* 大幅减少FSR1的代码量
* 加入了捕获模式选项

# 0.0.3a4

* 更改项目结构
* 加入一个显示各种信息的界面
* 更改1.21.1版本mod要求，现在可以在1.21上使用
* 改进vulkan初始化，现在如果vulkan初始化失败不会使游戏崩溃
* 去除了VK_KHR_format_feature_flags2硬件扩展要求，理论上兼容更多硬件
* 修复算法支持情况误报的bug
* 修复与Fabric版本TACZ的兼容性问题
* 修复issues/7
* 修复重载资源包后渲染位置出错
* 加入了 文言（華夏）翻译 ~~_(闲.jpg)_~~