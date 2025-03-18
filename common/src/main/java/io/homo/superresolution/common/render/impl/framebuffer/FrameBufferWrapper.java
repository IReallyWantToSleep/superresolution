package io.homo.superresolution.common.render.impl.framebuffer;

import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;

public class FrameBufferWrapper implements ITexture {
    private final IFrameBuffer frameBuffer;
    private final boolean depth;

    FrameBufferWrapper(IFrameBuffer frameBuffer, boolean depth) {
        this.frameBuffer = frameBuffer;
        this.depth = depth;
    }

    public static FrameBufferWrapper ofColor(IFrameBuffer frameBuffer) {
        return new FrameBufferWrapper(frameBuffer, false);
    }

    public static FrameBufferWrapper ofDepth(IFrameBuffer frameBuffer) {
        return new FrameBufferWrapper(frameBuffer, true);
    }


    @Override
    public int getTextureId() {
        return depth ? frameBuffer.getTextureId(IFrameBuffer.FrameBufferAttachmentType.DEPTH) : frameBuffer.getTextureId(IFrameBuffer.FrameBufferAttachmentType.COLOR);
    }

    @Override
    public TextureFormat getTextureFormat() {
        return depth ? frameBuffer.getDepthTextureFormat() : frameBuffer.getColorTextureFormat();
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
        frameBuffer.destroy();
    }

    @Override
    public void resize(int width, int height) {
        frameBuffer.resizeFrameBuffer(width, height);
    }
}
