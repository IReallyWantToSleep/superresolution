package io.homo.superresolution.common.render.utils;

import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;

public class HandRenderTarget {
    public static IFrameBuffer handRenderTarget;

    public static IFrameBuffer getHandRenderTarget() {
        if (handRenderTarget == null) {
            handRenderTarget = GlFrameBuffer.create(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );
            handRenderTarget.setClearColor(0, 0, 0, 0);
            handRenderTarget.resizeFrameBuffer(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );
            handRenderTarget.clearFrameBuffer();
        }
        return handRenderTarget;
    }
}
