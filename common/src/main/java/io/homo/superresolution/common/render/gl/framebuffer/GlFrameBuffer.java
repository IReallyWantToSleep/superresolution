package io.homo.superresolution.common.render.gl.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.RenderTargetBindPoint;
import net.minecraft.client.Minecraft;

@Deprecated
public class GlFrameBuffer extends RenderTarget implements IFrameBuffer {
    public GlFrameBuffer(boolean useDepth) {
        super(useDepth);
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    #if MC_VER < MC_1_21_4
    @Override
    public void clear() {
        this.clear(Minecraft.ON_OSX);
    }

    @Override
    public void resize(int width, int height) {
        this.resize(width, height, Minecraft.ON_OSX);
    }
    #endif

    public void bind(RenderTargetBindPoint bindPoint, boolean setViewport) {
        if (bindPoint == RenderTargetBindPoint.READ) {
            this.bindRead();
        } else {
            this.bindWrite(setViewport);
        }
    }

    public void bind(RenderTargetBindPoint bindPoint) {
        bind(bindPoint, true);
    }

    public void unbind(RenderTargetBindPoint bindPoint) {
        if (bindPoint == RenderTargetBindPoint.READ) {
            this.unbindRead();
        } else {
            this.unbindWrite();
        }

    }

    @Override
    public int getFrameBufferId() {
        return this.frameBufferId;
    }

    @Override
    public RenderTarget asMcRenderTarget() {
        return this;
    }
}
