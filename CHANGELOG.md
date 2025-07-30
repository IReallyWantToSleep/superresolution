# 0.8.0-alpha.1

* 支持Minecraft 1.21.5+
* 大幅重构底层代码与配置系统，彻底弃用老API
* 移植FSR2 v2.3.3
* 新增选项：FSR2是否启用FP16模式
* 新增兼容性着色器编译器，提升Intel显卡兼容性 ~~（🐕⑩驱动跟AMD坐一桌）~~
* 新增性能图表显示
* 更新Glslang
* 修复致死量Bug
* ~~增加致死量Bug~~

# 0.7.1-alpha.3

* 修复1.21.1+UI错位问题
* 新增事件：AlgorithmDispatchFinishEvent
* 新增事件：LevelRenderStartEvent
* 新增事件：LevelRenderEndEvent

# 0.7.1-alpha.2

* 新增选项：PostChain注入黑名单
* 添加日志来记录已经注入的PostChain

# 0.7.1-alpha.1

* 现在模组需要`GL_ARB_direct_state_access`OpenGL扩展
* 将多数的GL调用由传统方式改为DSA调用，或许会提升性能
* 优化FSR2性能
* 修复AMD显卡上的异常崩溃
* 修复issues/55
* 修复issues/34
* 修复issues/60
* 修复issues/56

# 0.7.0-alpha.3

* 优化着色器的编译与缓存
* 修复issues/57 (FSR2曝光问题)

# 0.7.0-alpha.2

* 修正缓存目录创建逻辑
* 修复issues/58

# 0.7.0-alpha.1

* 现在模组必须需要`GL_ARB_gl_spirv`OpenGL扩展
* 将FSR2的后端移植到Java（严格意义上算重写）
* 将模组自己实现的着色器编译器改为Glslang
* 删除AbstractAlgorithm.blitToScreen方法
* 新增配置选项：打开配置界面时是否暂停游戏
* 去除配置选项：跳过加载依赖库
* C++依赖库添加`Linux x64`与`Android armv8`版本
* 重命名部分类
* 重构着色器相关类
* 重构GlPipeline
* 修复issues/28

# 0.6.2-alpha.1

* 更改开发人员API
* 完善配置系统
* 添加对一些Vulkan信息的显示
* 去除文言（華夏）翻译
* FSR2目前仅限在开发环境运行
* 修复部分选项无法应用的bug
* 添加实验性的运动矢量生成
* 修复issues/43

# 0.6.1-alpha.1

* 完善开发人员API
* 补全某些缺少的许可证文件
* 完善国际化
* 信息界面的一些文本有了更显眼的颜色
* 自动检查可用算法
* 优化UI全景图渲染
* 修复信息界面的可展开文本列表渲染长文本时的错误
* 修复issues/42/41/40/39
* 修复Forge上配置在重启游戏后丢失的问题
* 删除UI中的虚化效果
* 现在会保存并恢复大多数的OpenGL状态
* 移除对SodiumOptionsAPI的依赖

# 0.6.0-alpha.1

警告：该版本国际化尚不完善 Warning: The internationalization of this version is not yet complete

* 新的开发人员API
* ~~添加对1.21.5的支持~~ _(暂时放弃)_
* 添加SGSR1
* 添加启用RenderDoc以及Imgui的调试选项 (仅开发环境可用)
* 添加用于调试的GL对象标签
* 添加SGSR2的计算着色器3通道版本
* 在配置界面添加了虚化效果
* 在配置界面添加了全景图渲染
* 切换帧缓冲区实现
* 与Sodium Options API模组更好的兼容性
* 优化OpenGL状态保存
* 优化Vulkan与OpenGL信息获取方式
* 优化配置界面某些文本
* 删除了一些冗余代码

# 0.5.0-alpha.2

警告：该版本国际化尚不完善 Warning: The internationalization of this version is not yet complete

* 更换图标 (感谢[yu](https://center.mcmod.cn/773788/)为本模组绘制的图标)
* 更改贡献者清单颜色
* 添加着色器缓存 _(但默认不启用因为用了更卡)_
* 添加跳过初始化Vulkan与跳过加载依赖库选项
* 修改配置界面滚动速度
* 修改ModelView矩阵捕获方式
* 修改版本号格式 _(0.0.5a1等于0.5.0-alpha.1)_
* 优化检测Vulkan支持的逻辑
* 优化算法初始化失败逻辑
* 优化屏幕大小的更新逻辑
* 修复issues/30
* 修复issues/29

# 0.0.5a1

警告：该版本国际化尚不完善 Warning: The internationalization of this version is not yet complete

* 优化了空值处理
* 实验性的SGSR
* 实验性的移动设备支持
* 美化信息界面 ~~_(指打断动画)_~~
* 在信息界面添加了更多内容
* 改进Vulkan兼容性检测
* 修复屏幕大小获取错误的问题
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