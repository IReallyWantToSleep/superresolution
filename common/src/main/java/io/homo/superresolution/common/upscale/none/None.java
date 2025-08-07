package io.homo.superresolution.common.upscale.none;

import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;

public class None extends AbstractAlgorithm {
    @Override
    public void init() {
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        return true;
    }

    public void resize(int width, int height) {
    }

    public void destroy() {
    }

    public int getInputTextureId() {
        return MinecraftRenderHandle.getRenderTarget().getTextureId(FrameBufferAttachmentType.Color);
    }

    public int getOutputTextureId() {
        return MinecraftRenderHandle.getRenderTarget().getTextureId(FrameBufferAttachmentType.Color);
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return MinecraftRenderHandle.getRenderTarget();
    }
}
