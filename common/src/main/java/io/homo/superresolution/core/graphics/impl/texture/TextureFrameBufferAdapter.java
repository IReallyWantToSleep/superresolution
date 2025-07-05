package io.homo.superresolution.core.graphics.impl.texture;

import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;

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
    public void bind(FrameBufferBindPoint bindPoint, boolean setViewport) {

    }

    @Override
    public void bind(FrameBufferBindPoint bindPoint) {

    }

    @Override
    public void unbind(FrameBufferBindPoint bindPoint) {

    }

    @Override
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        return attachmentType == FrameBufferAttachmentType.Color ? texture.handle() : -1;
    }

    @Override
    public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
        return this.texture;
    }

    @Override
    public int handle() {
        return -100;
    }

    @Override
    public void setClearColorRGBA(float red, float green, float blue, float alpha) {

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
    public void destroy() {

    }
}
