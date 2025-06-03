package io.homo.superresolution.common.minecraft;

import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;

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
