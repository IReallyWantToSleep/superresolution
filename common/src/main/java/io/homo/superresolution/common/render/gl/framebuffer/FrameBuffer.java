package io.homo.superresolution.common.render.gl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;

public class FrameBuffer extends RenderTarget {
    public FrameBuffer(boolean useDepth){
        super(useDepth);
    }
}
