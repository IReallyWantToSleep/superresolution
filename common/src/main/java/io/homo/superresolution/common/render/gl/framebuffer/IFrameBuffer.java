package io.homo.superresolution.common.render.gl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.RenderTargetBindPoint;

public interface IFrameBuffer {
    int getWidth();

    int getHeight();

    void clear();

    void resize(int width, int height);

    void destroyBuffers();

    void bind(RenderTargetBindPoint bindPoint, boolean setViewport);

    void bind(RenderTargetBindPoint bindPoint);

    void unbind(RenderTargetBindPoint bindPoint);

    int getColorTextureId();

    int getDepthTextureId();

    int getFrameBufferId();

    void setClearColor(float red, float green, float blue, float alpha);

    RenderTarget asMcRenderTarget();
}
