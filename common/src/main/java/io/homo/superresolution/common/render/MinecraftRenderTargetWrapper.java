package io.homo.superresolution.common.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import net.minecraft.client.Minecraft;

public class MinecraftRenderTargetWrapper implements IFrameBuffer {
    public RenderTarget renderTarget;

    MinecraftRenderTargetWrapper(RenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    public static MinecraftRenderTargetWrapper of(RenderTarget renderTarget) {
        if (renderTarget == null) return null;
        return new MinecraftRenderTargetWrapper(renderTarget);
    }

    public void clearFrameBuffer() {
        #if MC_VER  < MC_1_21_4
        this.renderTarget.clear(Minecraft.ON_OSX);
        #else
        this.renderTarget.clear();
        #endif
    }

    public void resizeFrameBuffer(int width, int height) {
        #if MC_VER < MC_1_21_4
        this.renderTarget.resize(width, height, Minecraft.ON_OSX);
        #else
        this.renderTarget.resize(width, height);
        #endif
    }

    @Override
    public int getWidth() {
        return renderTarget.width;
    }

    @Override
    public int getHeight() {
        return renderTarget.height;
    }

    @Override
    public void destroy() {
        renderTarget.destroyBuffers();
    }

    public void bind(RenderTargetBindPoint bindPoint, boolean setViewport) {
        if (bindPoint == RenderTargetBindPoint.READ) {
            renderTarget.bindRead();
        } else {
            renderTarget.bindWrite(setViewport);
        }
    }

    public void bind(RenderTargetBindPoint bindPoint) {
        bind(bindPoint, true);
    }

    public void unbind(RenderTargetBindPoint bindPoint) {
        if (bindPoint == RenderTargetBindPoint.READ) {
            renderTarget.unbindRead();
        } else if (bindPoint == RenderTargetBindPoint.WRITE) {
            renderTarget.unbindWrite();
        } else {
            renderTarget.unbindRead();
            renderTarget.unbindWrite();
        }
    }

    @Override
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        return switch (attachmentType) {
            case COLOR -> renderTarget.getColorTextureId();
            case DEPTH, DEPTH_STENCIL -> renderTarget.getDepthTextureId();
        };
    }

    @Override
    public int getFrameBufferId() {
        return renderTarget.frameBufferId;
    }

    @Override
    public void setClearColor(float red, float green, float blue, float alpha) {
        renderTarget.setClearColor(red, green, blue, alpha);
    }

    @Override
    public TextureFormat getColorTextureFormat() {
        return TextureFormat.RGBA8;
    }

    @Override
    public TextureFormat getDepthTextureFormat() {
        return TextureFormat.DEPTH24;
    }


    @Override
    public RenderTarget asMcRenderTarget() {
        return renderTarget;
    }
}
