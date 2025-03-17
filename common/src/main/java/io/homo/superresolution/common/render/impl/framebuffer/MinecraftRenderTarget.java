package io.homo.superresolution.common.render.impl.framebuffer;

import net.minecraft.client.Minecraft;

public class MinecraftRenderTarget extends StorageFrameBuffer {
    public MinecraftRenderTarget(boolean useDepth) {
        super(useDepth);
    }

    public void clearFrameBuffer() {
        #if MC_VER  < MC_1_21_4
        this.clear(Minecraft.ON_OSX);
        #else
        this.clear();
        #endif
    }

    public void resizeFrameBuffer(int width, int height) {
        #if MC_VER  < MC_1_21_4
        this.resize(width, height, Minecraft.ON_OSX);
        #else
        this.resize(width, height);
        #endif
    }
}
