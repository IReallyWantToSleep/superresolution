package io.homo.superresolution.common.render.impl.framebuffer;

import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;

public class FrameBufferTextureAdapter implements ITexture {
    private final IFrameBuffer frameBuffer;
    private final boolean depth;

    FrameBufferTextureAdapter(IFrameBuffer frameBuffer, boolean depth) {
        this.frameBuffer = frameBuffer;
        this.depth = depth;
    }

    public static FrameBufferTextureAdapter ofColor(IFrameBuffer frameBuffer) {
        return new FrameBufferTextureAdapter(frameBuffer, false);
    }

    public static FrameBufferTextureAdapter ofDepth(IFrameBuffer frameBuffer) {
        return new FrameBufferTextureAdapter(frameBuffer, true);
    }


    @Override
    public int getTextureId() {
        return depth ? frameBuffer.getTextureId(FrameBufferAttachmentType.DEPTH) : frameBuffer.getTextureId(FrameBufferAttachmentType.COLOR);
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
