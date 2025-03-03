package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.gl.framebuffer.StorageFrameBuffer;
import net.minecraft.client.Minecraft;

public class HandRenderTarget {
    public static RenderTarget handRenderTarget;

    public static RenderTarget getHandRenderTarget() {
        if (handRenderTarget == null) {
            handRenderTarget = new StorageFrameBuffer(true);
            handRenderTarget.setClearColor(0, 0, 0, 0);
            handRenderTarget.resize(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight(),
                    Minecraft.ON_OSX
            );
            handRenderTarget.clear(Minecraft.ON_OSX);
        }
        return handRenderTarget;
    }
}
