# 这是？
这是SR模组的本机库，实现了Glslang，Streamline，NGX极其简单的JNI包装。

此外，它还负责SR模组与各个超分辨率算法的交互，这部分的API设计主要参考~~复制~~AMD FFX SDK。

# 想要构建它？
请参考[构建本机库](docs/build.md)文档。

# 想要添加新算法？
请参考[如何添加一个新的超分辨率提供器](docs/add_upscale_provider.md)文档。

# 许可证
超分辨率模组本身的许可证是GPL-3.0，但本机库是LGPL-3.0许可证。