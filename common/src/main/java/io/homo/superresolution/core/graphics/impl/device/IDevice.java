package io.homo.superresolution.core.graphics.impl.device;

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.command.ICommandEncoder;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;

public interface IDevice {
    /**
     * 创建一个纹理。
     *
     * @param description 纹理描述对象
     * @return 新创建的纹理对象
     */
    ITexture createTexture(TextureDescription description);

    /**
     * 创建一个着色器程序。
     *
     * @param description 着色器描述对象
     * @return 新创建的着色器程序对象
     */
    IShaderProgram<?> createShaderProgram(ShaderDescription description);

    /**
     * 创建顶点缓冲区
     *
     * @param description 顶点缓冲区描述对象，包含缓冲区大小和用途等参数
     * @return 新创建的顶点缓冲区对象
     */
    IVertexBuffer createVertexBuffer(VertexBufferDescription description);

    /**
     * 创建缓冲区
     *
     * @param description 缓冲区描述对象，包含缓冲区大小和用途等参数
     * @return 新创建的缓冲区对象
     */
    IBuffer createBuffer(BufferDescription description);

    ICommandEncoder commendEncoder();

    ICommandDecoder commandDecoder();

    void submitCommandBuffer(ICommandBuffer commandBuffer);

}
