package io.homo.superresolution.core.graphics.impl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.Destroyable;

/**
 * 帧缓冲区
 */
public interface IFrameBuffer extends Destroyable, GpuObject {
    /**
     * 获取帧缓冲区宽度
     *
     * @return 当前宽度（像素单位）
     */
    int getWidth();

    /**
     * 获取帧缓冲区高度
     *
     * @return 当前高度（像素单位）
     */
    int getHeight();

    /**
     * 清除帧缓冲区内容
     * <p>使用预设的清除颜色/深度值重置所有附件</p>
     */
    void clearFrameBuffer();

    /**
     * 调整帧缓冲区尺寸
     *
     * @param width  新宽度（像素单位）
     * @param height 新高度（像素单位）
     * @throws IllegalArgumentException 当尺寸值无效时抛出
     */
    void resizeFrameBuffer(int width, int height);

    /**
     * 获取附件纹理的标识符
     *
     * @param attachmentType 附件类型（如颜色附件0、深度附件等）
     * @return 纹理ID，无附件时返回0
     */
    int getTextureId(FrameBufferAttachmentType attachmentType);

    /**
     * 获取附件纹理对象
     *
     * @param attachmentType 附件类型
     * @return 纹理实例，无附件时返回null
     */
    ITexture getTexture(FrameBufferAttachmentType attachmentType);

    /**
     * 设置清除颜色（RGBA格式）
     *
     * @param red   红色分量 [0,1]
     * @param green 绿色分量 [0,1]
     * @param blue  蓝色分量 [0,1]
     * @param alpha 透明度 [0,1]
     */
    void setClearColorRGBA(float red, float green, float blue, float alpha);

    /**
     * 获取颜色附件纹理格式
     *
     * @return 主颜色附件格式
     */
    TextureFormat getColorTextureFormat();

    /**
     * 获取深度附件纹理格式
     *
     * @return 深度附件格式，无深度附件时返回null
     */
    TextureFormat getDepthTextureFormat();

    /**
     * 转换为Minecraft原生渲染目标
     * <p><b>默认不支持转换</b>，需实现类覆盖</p>
     *
     * @return Minecraft RenderTarget实例
     * @throws UnsupportedOperationException 默认实现抛出异常
     */
    default RenderTarget asMcRenderTarget() {
        throw new UnsupportedOperationException("Minecraft render target conversion not implemented");
    }

    default void label(String label) {
    }
}