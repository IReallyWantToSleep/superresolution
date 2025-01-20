package io.homo.superresolution.upscale.none;

import io.homo.superresolution.render.MinecraftRenderingStates;
import io.homo.superresolution.render.gl.framebuffer.OnlyTextureIdFrameBuffer;
import io.homo.superresolution.render.gl.texture.Texture;
import io.homo.superresolution.upscale.AbstractAlgorithm;

import static io.homo.superresolution.render.gl.GlConst.GL_NEAREST;

public class None extends AbstractAlgorithm {
    public int upscaleId = GL_NEAREST;

    public static None create() {
        return new None();
    }

    @Override
    public void init() {
        input = MinecraftRenderingStates.getRenderTarget();
        output = new OnlyTextureIdFrameBuffer(input.getColorTextureId());
    }

    @Override
    public boolean dispatch(float frameTimeDelta) {
        return true;
    }

    @Override
    public void blitToScreen(int width, int height) {
        Texture.blitToScreen(input.width,input.height,width,height,this.input.getColorTextureId());
    }

    public void resize(int width, int height) {}
    public void destroy() {}
}
