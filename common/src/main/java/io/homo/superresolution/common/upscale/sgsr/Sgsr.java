package io.homo.superresolution.common.upscale.sgsr;

import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.framebuffer.FrameBuffer;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;

public class Sgsr extends AbstractAlgorithm {
    public static Sgsr create() {
        return new Sgsr();
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        output = new FrameBuffer(false);
        this.resize(AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        return false;
    }

    @Override
    public void blitToScreen(int width, int height) {

    }
}
