package io.homo.superresolution.common.render.impl.framebuffer;

import net.minecraft.client.Minecraft;

public class MinecraftRenderTarget extends LegacyStorageFrameBuffer {
    public MinecraftRenderTarget(boolean useDepth) {
        super(useDepth);
    }
}