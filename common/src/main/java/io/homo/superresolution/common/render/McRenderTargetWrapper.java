package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.gl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.gl.framebuffer.StorageFrameBuffer;
import net.minecraft.client.Minecraft;

public class McRenderTargetWrapper implements IFrameBuffer {
    public final RenderTarget renderTarget;

    public McRenderTargetWrapper(RenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    public McRenderTargetWrapper(boolean useDepth) {
        this.renderTarget = new StorageFrameBuffer(useDepth);
    }

    @Override
    public int getWidth() {
        return renderTarget.width;
    }

    @Override
    public int getHeight() {
        return renderTarget.height;
    }

    public void clear() {
        #if MC_VER  < MC_1_21_4
        this.renderTarget.clear(Minecraft.ON_OSX);
        #else
        this.renderTarget.clear();
        #endif
    }

    public void resize(int width, int height) {
        #if MC_VER  < MC_1_21_4
        this.renderTarget.resize(width, height, Minecraft.ON_OSX);
        #else
        this.renderTarget.resize(width,height);
        #endif
    }

    public void destroyBuffers() {
        this.renderTarget.destroyBuffers();
    }

    public void bind(RenderTargetBindPoint bindPoint, boolean setViewport) {
        if (bindPoint == RenderTargetBindPoint.READ) {
            this.renderTarget.bindRead();
        } else {
            this.renderTarget.bindWrite(setViewport);
        }
    }

    public void bind(RenderTargetBindPoint bindPoint) {
        bind(bindPoint, true);
    }

    public void unbind(RenderTargetBindPoint bindPoint) {
        if (bindPoint == RenderTargetBindPoint.READ) {
            this.renderTarget.unbindRead();
        } else {
            this.renderTarget.unbindWrite();
        }

    }

    public int getColorTextureId() {
        return renderTarget.getColorTextureId();
    }

    public int getDepthTextureId() {
        return renderTarget.getDepthTextureId();
    }

    public int getFrameBufferId() {
        return renderTarget.frameBufferId;
    }

    public void setClearColor(float red, float green, float blue, float alpha) {
        renderTarget.setClearColor(red, green, blue, alpha);
    }

    @Override
    public RenderTarget asMcRenderTarget() {
        return renderTarget;
    }

}
