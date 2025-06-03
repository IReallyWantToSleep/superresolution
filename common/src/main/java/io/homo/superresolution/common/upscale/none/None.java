package io.homo.superresolution.common.upscale.none;

import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;

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

    @Override
    public IFrameBuffer getInputFrameBuffer() {
        return MinecraftRenderHandle.getRenderTarget();
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return MinecraftRenderHandle.getRenderTarget();
    }
}
