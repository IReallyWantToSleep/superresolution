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
     * 等待GPU执行完渲染指令
     */
    void finish();
}