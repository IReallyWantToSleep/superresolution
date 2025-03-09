package io.homo.superresolution.common.upscale.sgsr;

import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.render.gl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.upscale.DispatchResource;

public abstract class AbstractSgsrVariant implements Resizable, Destroyable {
    protected IFrameBuffer output;

    public void setOutput(IFrameBuffer output) {
        this.output = output;
    }

    public abstract void dispatch(DispatchResource resource, Sgsr sgsr);

    public abstract void init(Sgsr sgsr);
}
