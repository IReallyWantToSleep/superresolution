package io.homo.superresolution.common.upscale.sgsr.variants;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.AbstractSgsrVariant;

public class Sgsr3PassCompute extends AbstractSgsrVariant {
    @Override
    public void dispatch(RenderTarget input, DispatchResource resource) {

    }

    @Override
    public int getOutputTexture() {
        return 0;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void resize(int width, int height) {

    }
}
