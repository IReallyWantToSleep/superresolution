package io.homo.superresolution.core.impl.framebuffer;

import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;

public class FrameBufferTextureAdapter implements ITexture {
    private final IFrameBuffer frameBuffer;
    private final FrameBufferAttachmentType attachmentType;

    FrameBufferTextureAdapter(IFrameBuffer frameBuffer, FrameBufferAttachmentType attachmentType) {
        this.frameBuffer = frameBuffer;
        this.attachmentType = attachmentType;
    }

    public static FrameBufferTextureAdapter ofColor(IFrameBuffer frameBuffer) {
        return of(frameBuffer, FrameBufferAttachmentType.COLOR);
    }

    public static FrameBufferTextureAdapter ofDepth(IFrameBuffer frameBuffer) {
        return of(frameBuffer, FrameBufferAttachmentType.ANY_DEPTH);
    }

    public static FrameBufferTextureAdapter of(IFrameBuffer frameBuffer, FrameBufferAttachmentType attachmentType) {
        return new FrameBufferTextureAdapter(frameBuffer, attachmentType);
    }


    @Override
    public int getTextureId() {
        return frameBuffer.getTextureId(attachmentType);
    }

    @Override
    public TextureFormat getTextureFormat() {
        return attachmentType.equals(FrameBufferAttachmentType.COLOR) ? frameBuffer.getColorTextureFormat() : frameBuffer.getDepthTextureFormat();
    }

    @Override
    public int getWidth() {
        return frameBuffer.getWidth();
    }

    @Override
    public int getHeight() {
        return frameBuffer.getHeight();
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("This method is not implemented yet.");
    }

    @Override
    public void resize(int width, int height) {
        throw new UnsupportedOperationException("This method is not implemented yet.");
    }
}
