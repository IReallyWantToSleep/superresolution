package io.homo.superresolution.core.graphics.system;

import io.homo.superresolution.core.graphics.impl.DrawObject;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

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
     * 获取硬件
     */
    IDevice device();

    /**
     * 将纹理清空为指定颜色。
     *
     * @param texture 目标纹理
     * @param color   RGBA颜色数组，每个分量范围为0~255
     */
    default void clearTextureRGBA(ITexture texture, int[] color) {
        float[] clearColor = new float[]{
                color[0] / 255.0f,
                color[1] / 255.0f,
                color[2] / 255.0f,
                color[3] / 255.0f
        };
        clearTextureRGBA(texture, clearColor);
    }

    /**
     * 将纹理清空为指定颜色。
     *
     * @param texture 目标纹理
     * @param color   RGBA颜色数组，每个分量范围为0~1
     */
    void clearTextureRGBA(ITexture texture, float[] color);

    /**
     * 将纹理清空为指定深度值。
     *
     * @param texture 目标纹理
     * @param depth   深度值，范围为0~1
     */
    void clearTextureDepth(ITexture texture, float depth);

    /**
     * 将纹理清空为指定模板值。
     *
     * @param texture 目标纹理
     * @param stencil 模板值，范围为0~255
     */
    void clearTextureStencil(ITexture texture, int stencil);


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
     * 复制缓冲区
     *
     * @param src       源
     * @param dst       目标
     * @param srcOffset 源偏移量
     * @param dstOffset 目标偏移量
     * @param size      拷贝数据大小
     */
    void copyBuffer(IBuffer src, IBuffer dst, long srcOffset, long dstOffset, long size);

    /**
     * 执行绘制命令
     *
     * @param shaderProgram 着色器程序对象
     * @param frameBuffer   目标帧缓冲区
     * @param drawObject
     * @param firstVertex   起始顶点索引
     * @param vertexCount   要绘制的顶点数量
     */
    void draw(IShaderProgram<?> shaderProgram, IFrameBuffer frameBuffer, DrawObject drawObject, int firstVertex, int vertexCount);


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