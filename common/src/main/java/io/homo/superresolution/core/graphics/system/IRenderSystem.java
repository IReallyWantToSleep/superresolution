package io.homo.superresolution.core.graphics.system;

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
    void clearTexture(ITexture texture, int[] color);

    /**
     * 将纹理清空为指定颜色。
     *
     * @param texture 目标纹理
     * @param color   RGBA颜色数组，每个分量范围为0~1
     */
    void clearTexture(ITexture texture, float[] color);


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
     * 设置当前使用的着色器程序。
     *
     * @param shaderProgram 着色器程序对象
     */
    void setShaderProgram(IShaderProgram<?> shaderProgram);

    /**
     * 执行计算着色器任务。
     *
     * @param x X方向工作组数
     * @param y Y方向工作组数
     * @param z Z方向工作组数
     */
    void dispatchCompute(int x, int y, int z);


    /**
     * 创建顶点缓冲区
     *
     * @param description 顶点缓冲区描述对象，包含缓冲区大小和用途等参数
     * @return 新创建的顶点缓冲区对象
     */
    IVertexBuffer createVertexBuffer(VertexBufferDescription description);

    /**
     * 上传顶点数据到缓冲区
     *
     * @param vertexBuffer 目标顶点缓冲区
     * @param data         顶点数据数组
     * @param offset       数据在数组中的起始偏移量
     * @param length       要上传的数据长度（以字节为单位）
     */
    void uploadVertexData(IVertexBuffer vertexBuffer, float[] data, int offset, int length);

    /**
     * 设置顶点属性布局
     *
     * @param attributes 顶点属性描述数组，定义每个属性的数据类型和布局
     */
    void setVertexAttributes(VertexAttribute[] attributes);

    /**
     * 执行绘制命令
     *
     * @param primitiveType 图元类型（如三角形、线条等）
     * @param vertexBuffer  要使用的顶点缓冲区
     * @param firstVertex   起始顶点索引
     * @param vertexCount   要绘制的顶点数量
     */
    void draw(PrimitiveType primitiveType, IVertexBuffer vertexBuffer, int firstVertex, int vertexCount);

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
}