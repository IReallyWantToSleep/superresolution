package io.homo.superresolution.core.graphics.impl.framebuffer;

/**
 * 可绑定帧缓冲接口
 */
public interface IBindableFrameBuffer extends IFrameBuffer {

    /**
     * 绑定帧缓冲区并可选设置视口
     *
     * @param bindPoint   绑定目标（如颜色/深度）
     * @param setViewport true时自动设置视口为缓冲区尺寸
     */
    void bind(FrameBufferBindPoint bindPoint, boolean setViewport);

    /**
     * 绑定帧缓冲区（不修改视口）
     *
     * @param bindPoint 绑定目标（如颜色/深度）
     */
    void bind(FrameBufferBindPoint bindPoint);

    /**
     * 解绑帧缓冲区
     *
     * @param bindPoint 要解绑的目标
     */
    void unbind(FrameBufferBindPoint bindPoint);
}
