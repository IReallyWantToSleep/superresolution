package io.homo.superresolution.core.graphics.impl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

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

    default RenderTarget asMcRenderTarget() {
        throw new RuntimeException();
    }
}
