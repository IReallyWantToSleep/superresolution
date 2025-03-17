package io.homo.superresolution.common.render;

import io.homo.superresolution.common.render.impl.framebuffer.MinecraftRenderTarget;

public class HandRenderTarget {
    public static MinecraftRenderTarget handRenderTarget;

    public static MinecraftRenderTarget getHandRenderTarget() {
        if (handRenderTarget == null) {
            handRenderTarget = new MinecraftRenderTarget(true);
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
