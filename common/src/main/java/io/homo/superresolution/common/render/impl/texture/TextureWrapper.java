package io.homo.superresolution.common.render.impl.texture;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.RenderTargetBindPoint;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;

public class TextureWrapper implements IFrameBuffer {
    private final ITexture texture;

    TextureWrapper(ITexture texture) {
        this.texture = texture;
    }

    public static TextureWrapper of(ITexture texture) {
        return new TextureWrapper(texture);
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
    public int getColorTextureId() {
        return texture.getTextureId();
    }

    @Override
    public int getDepthTextureId() {
        return 0;
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
        return null;
    }

    @Override
    public RenderTarget asMcRenderTarget() {
        return new RenderTarget(false) {
            @Override
            public int getColorTextureId() {
                width = getWidth();
                height = getHeight();
                return super.getColorTextureId();
            }
        };
    }

    @Override
    public void destroy() {

    }
}
