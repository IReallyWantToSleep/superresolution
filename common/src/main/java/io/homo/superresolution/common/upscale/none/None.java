package io.homo.superresolution.common.upscale.none;

import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

import static io.homo.superresolution.common.render.gl.GlConst.GL_NEAREST;

public class None extends AbstractAlgorithm {
    public static None create() {
        return new None();
    }

    @Override
    public void init() {
        input = null;
        output = null;
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        return true;
    }

    @Override
    public void blitToScreen(int width, int height) {
        MinecraftRenderHandle.callOnRenderTarget(frameBuffer -> GlTexture.blitToScreen(
                frameBuffer.getWidth(),
                frameBuffer.getHeight(),
                width,
                height,
                frameBuffer.getTextureId(IFrameBuffer.FrameBufferAttachmentType.COLOR)
        ));
    }

    public void resize(int width, int height) {
    }

    public void destroy() {
    }

    public int getInputTextureId() {
        return MinecraftRenderHandle.getRenderTarget().getTextureId(IFrameBuffer.FrameBufferAttachmentType.COLOR);
    }

    public int getOutputTextureId() {
        return MinecraftRenderHandle.getRenderTarget().getTextureId(IFrameBuffer.FrameBufferAttachmentType.COLOR);
    }
}
