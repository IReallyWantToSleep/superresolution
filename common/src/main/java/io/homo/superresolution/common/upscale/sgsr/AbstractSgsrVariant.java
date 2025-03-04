package io.homo.superresolution.common.upscale.sgsr;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.upscale.DispatchResource;

public abstract class AbstractSgsrVariant implements Resizable, Destroyable {
    protected RenderTarget output;

    public void setOutput(RenderTarget output) {
        this.output = output;
    }

    public abstract void dispatch(DispatchResource resource, Sgsr sgsr);

    public abstract void init(Sgsr sgsr);
}
