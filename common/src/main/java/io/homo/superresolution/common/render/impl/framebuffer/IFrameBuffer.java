package io.homo.superresolution.common.render.impl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;

public interface IFrameBuffer extends Destroyable {
    int getWidth();

    int getHeight();

    void clearFrameBuffer();

    void resizeFrameBuffer(int width, int height);


    void bind(FrameBufferBindPoint bindPoint, boolean setViewport);

    void bind(FrameBufferBindPoint bindPoint);

    void unbind(FrameBufferBindPoint bindPoint);

    int getTextureId(FrameBufferAttachmentType attachmentType);

    ITexture getTexture(FrameBufferAttachmentType attachmentType);

    int getFrameBufferId();

    void setClearColor(float red, float green, float blue, float alpha);

    TextureFormat getColorTextureFormat();

    TextureFormat getDepthTextureFormat();

    RenderTarget asMcRenderTarget();

}
