package io.homo.superresolution.common.render.impl.framebuffer;

import java.util.*;

public class RenderTargetCache {
    private static final Map<IFrameBuffer, FrameBufferRenderTargetAdapter> cached = new HashMap<>();

    public static FrameBufferRenderTargetAdapter cacheOf(IFrameBuffer frameBuffer) {
        if (cached.get(frameBuffer) == null) {
            FrameBufferRenderTargetAdapter renderTarget = FrameBufferRenderTargetAdapter.ofRenderTarget(frameBuffer);
            cached.put(frameBuffer, renderTarget);
            return renderTarget;
        } else {
            return cached.get(frameBuffer).bindFrameBuffer(frameBuffer);
        }
    }
}
