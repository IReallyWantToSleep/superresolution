package io.homo.superresolution.common.upscale.none;

import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.framebuffer.OnlyTextureIdFrameBuffer;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

import static io.homo.superresolution.common.render.gl.GlConst.GL_NEAREST;

public class None extends AbstractAlgorithm {
    public int upscaleId = GL_NEAREST;

    public static None create() {
        return new None();
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        output = new OnlyTextureIdFrameBuffer(input.getColorTextureId());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        return true;
    }

    @Override
    public void blitToScreen(int width, int height) {
        GlTexture.blitToScreen(input.width, input.height, width, height, this.input.getColorTextureId());
    }

    public void resize(int width, int height) {
    }

    public void destroy() {
    }
}
