package io.homo.superresolution.common.render.impl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.render.RenderTargetBindPoint;

public interface IFrameBuffer extends Destroyable, Resizable {
    int getWidth();

    int getHeight();

    void clear();

    void bind(RenderTargetBindPoint bindPoint, boolean setViewport);

    void bind(RenderTargetBindPoint bindPoint);

    void unbind(RenderTargetBindPoint bindPoint);

    int getColorTextureId();

    int getDepthTextureId();

    int getFrameBufferId();

    void setClearColor(float red, float green, float blue, float alpha);

    RenderTarget asMcRenderTarget();
}
