package io.homo.superresolution.common.render.impl.texture;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.utils.RenderTargetBindPoint;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;

public class TextureFrameBufferAdapter implements IFrameBuffer {
    private final ITexture texture;

    TextureFrameBufferAdapter(ITexture texture) {
        this.texture = texture;
    }

    public static TextureFrameBufferAdapter of(ITexture texture) {
        return new TextureFrameBufferAdapter(texture);
    }

    @Override
    public int getWidth() {
        return texture.getWidth();
    }

    @Override
    public int getHeight() {
        return texture.getHeight();
    }

    @Override
    public void clearFrameBuffer() {

    }

    @Override
    public void resizeFrameBuffer(int width, int height) {
        texture.resize(width, height);
    }

    @Override
    public void bind(RenderTargetBindPoint bindPoint, boolean setViewport) {

    }

    @Override
    public void bind(RenderTargetBindPoint bindPoint) {

    }

    @Override
    public void unbind(RenderTargetBindPoint bindPoint) {

    }

    @Override
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        return attachmentType == FrameBufferAttachmentType.COLOR ? texture.getTextureId() : -1;
    }

    @Override
    public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
        return this.texture;
    }

    @Override
    public int getFrameBufferId() {
        return -100;
    }

    @Override
    public void setClearColor(float red, float green, float blue, float alpha) {

    }

    @Override
    public TextureFormat getColorTextureFormat() {
        return texture.getTextureFormat();
    }

    @Override
    public TextureFormat getDepthTextureFormat() {
        return texture.getTextureFormat();
    }

    @Override
    public RenderTarget asMcRenderTarget() {
        throw new RuntimeException();
    }

    @Override
    public void destroy() {

    }
}
