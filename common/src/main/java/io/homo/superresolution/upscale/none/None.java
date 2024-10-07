package io.homo.superresolution.upscale.none;

import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import io.homo.superresolution.upscale.AbstractAlgorithm;
import io.homo.superresolution.render.gl.utils.OnlyTexIdFrameBuffer;

import static io.homo.superresolution.render.gl.GlConst.*;

public class None extends AbstractAlgorithm {
    public int upscaleId = GL_NEAREST;

    @Override
    public void init() {
        input = ResolutionControl.getInstance().getFramebuffer();
        output = new OnlyTexIdFrameBuffer(input.getColorTextureId());
    }

    @Override
    public boolean run(float frameTimeDelta) {
        return true;
    }

    @Override
    public void blitToScreen(int width, int height) {
        this.input.blitToScreen(width,height,true);
    }

    public static None create() {
        return new None();
    }

    public void resize(int width, int height) {}
    public void destroy() {}
}
