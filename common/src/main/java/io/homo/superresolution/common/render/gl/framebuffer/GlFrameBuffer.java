package io.homo.superresolution.common.render.gl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;

public class GlFrameBuffer extends RenderTarget {
    public GlFrameBuffer(boolean useDepth) {
        super(useDepth);
    }
}
