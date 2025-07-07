package io.homo.superresolution.common.minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;

import java.util.*;

public class RenderTargetCache {
    private static final Map<IBindableFrameBuffer, FrameBufferRenderTargetAdapter> cached = new HashMap<>();

    public static RenderTarget cacheOf(IBindableFrameBuffer frameBuffer) {
        if (cached.get(frameBuffer) == null) {
            FrameBufferRenderTargetAdapter renderTarget = FrameBufferRenderTargetAdapter.ofRenderTarget(frameBuffer);
            cached.put(frameBuffer, renderTarget);
            return renderTarget;
        } else {
            return cached.get(frameBuffer).bindFrameBuffer(frameBuffer);
        }
    }
}
