package io.homo.superresolution.common.render;

import io.homo.superresolution.common.render.gl.framebuffer.StorageFrameBuffer;

public class HandRenderTarget {
    public static McRenderTargetWrapper handRenderTarget;

    public static McRenderTargetWrapper getHandRenderTarget() {
        if (handRenderTarget == null) {
            handRenderTarget = new McRenderTargetWrapper(new StorageFrameBuffer(true));
            handRenderTarget.setClearColor(0, 0, 0, 0);
            handRenderTarget.resize(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );
            handRenderTarget.clear();
        }
        return handRenderTarget;
    }
}
