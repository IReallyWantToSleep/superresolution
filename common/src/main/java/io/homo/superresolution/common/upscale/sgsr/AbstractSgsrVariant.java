package io.homo.superresolution.common.upscale.sgsr;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.upscale.DispatchResource;

public abstract class AbstractSgsrVariant implements Resizable, Destroyable {
    public abstract void dispatch(RenderTarget input, DispatchResource resource);

    public abstract int getOutputTexture();
}
