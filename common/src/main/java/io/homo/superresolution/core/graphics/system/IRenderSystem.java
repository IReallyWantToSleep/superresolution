package io.homo.superresolution.core.graphics.system;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttribute;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;

public interface IRenderSystem {
    /**
     * 初始化渲染系统。
     * <p>
     * 在调用任何渲染相关方法前，需先初始化渲染系统。
     */
    void initRenderSystem();

    /**
     * 销毁渲染系统，释放相关资源。
     * <p>
     * 当渲染系统不再使用时应调用此方法，避免资源泄漏。
     */
    void destroyRenderSystem();

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
     * 将纹理清空为指定颜色。
     *
     * @param texture 目标纹理
     * @param color   RGBA颜色数组，每个分量范围为0~255
     */
    void clearTextureRGBA(ITexture texture, int[] color);

    /**
     * 将纹理清空为指定颜色。
     *
     * @param texture 目标纹理
     * @param color   RGBA颜色数组，每个分量范围为0~1
     */
    void clearTextureRGBA(ITexture texture, float[] color);


    /**
     * 将源纹理的指定区域复制到目标纹理的指定区域。
     *
     * @param src      源纹理
     * @param dst      目标纹理
     * @param srcX0    源区域起始X坐标
     * @param srcY0    源区域起始Y坐标
     * @param srcX1    源区域结束X坐标
     * @param srcY1    源区域结束Y坐标
     * @param srcLevel 源纹理的mipmap层级
     * @param dstX0    目标区域起始X坐标
     * @param dstY0    目标区域起始Y坐标
     * @param dstX1    目标区域结束X坐标
     * @param dstY1    目标区域结束Y坐标
     * @param dstLevel 目标纹理的mipmap层级
     */
    void copyTexture(
            ITexture src,
            ITexture dst,
            int srcX0,
            int srcY0,
            int srcX1,
            int srcY1,
            int srcLevel,
            int dstX0,
            int dstY0,
            int dstX1,
            int dstY1,
            int dstLevel
    );

    /**
     * 创建顶点缓冲区
     *
     * @param description 顶点缓冲区描述对象，包含缓冲区大小和用途等参数
     * @return 新创建的顶点缓冲区对象
     */
    IVertexBuffer createVertexBuffer(VertexBufferDescription description);

    /**
     * 执行绘制命令
     *
     * @param shaderProgram 着色器程序对象
     * @param drawObject
     * @param firstVertex   起始顶点索引
     * @param vertexCount   要绘制的顶点数量
     */
    void draw(IShaderProgram<?> shaderProgram, DrawObject drawObject, int firstVertex, int vertexCount);

    /**
     * 执行计算着色器任务。
     *
     * @param shaderProgram 着色器程序对象
     * @param x             X方向工作组数
     * @param y             Y方向工作组数
     * @param z             Z方向工作组数
     */
    void dispatchCompute(IShaderProgram<?> shaderProgram, int x, int y, int z);


    /**
     * 销毁顶点缓冲区并释放资源
     *
     * @param vertexBuffer 要销毁的顶点缓冲区
     */
    void destroyVertexBuffer(IVertexBuffer vertexBuffer);

    /**
     * 获取渲染状态
     *
     * @return 渲染状态
     */
    IRenderState renderState();

    /**
     * 等待GPU执行完渲染指令
     */
    void finish();
}