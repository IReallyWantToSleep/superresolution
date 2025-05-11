package io.homo.superresolution.common.upscale.none;

import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

public class None extends AbstractAlgorithm {
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
        MinecraftRenderHandle.callOnRenderTarget(frameBuffer -> GlTexture2D.blitToScreen(
                frameBuffer.getWidth(),
                frameBuffer.getHeight(),
                width,
                height,
                frameBuffer.getTextureId(FrameBufferAttachmentType.COLOR)
        ));
    }

    public void resize(int width, int height) {
    }

    public void destroy() {
    }

    public int getInputTextureId() {
        return MinecraftRenderHandle.getRenderTarget().getTextureId(FrameBufferAttachmentType.COLOR);
    }

    public int getOutputTextureId() {
        return MinecraftRenderHandle.getRenderTarget().getTextureId(FrameBufferAttachmentType.COLOR);
    }
}
