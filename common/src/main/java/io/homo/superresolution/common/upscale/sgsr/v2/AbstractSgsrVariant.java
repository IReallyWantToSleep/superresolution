package io.homo.superresolution.common.upscale.sgsr.v2;

import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Resizable;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.upscale.DispatchResource;

public abstract class AbstractSgsrVariant implements Resizable, Destroyable {
    protected IFrameBuffer output;

    public void setOutput(IFrameBuffer output) {
        this.output = output;
    }

    public abstract void dispatch(DispatchResource resource, Sgsr2 sgsr);

    public abstract void init(Sgsr2 sgsr);
}
