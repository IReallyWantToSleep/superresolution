package io.homo.superresolution.common.render.utils;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.render.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.utils.ColorUtil;

#if MC_VER < MC_1_21_1
import net.minecraft.client.Minecraft;
#endif


import static io.homo.superresolution.common.render.gl.Gl.glBindFramebuffer;

public class MinecraftRenderTargetWrapper implements IFrameBuffer {
    public RenderTarget renderTarget;
    private final int clearColor = ColorUtil.color(255, 0, 0, 0);

    MinecraftRenderTargetWrapper(RenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    public static MinecraftRenderTargetWrapper of(RenderTarget renderTarget) {
        if (renderTarget == null) return null;
        MinecraftRenderTargetWrapper wrapper = new MinecraftRenderTargetWrapper(renderTarget);
        return wrapper;
    }

    public void clearFrameBuffer() {
        #if MC_VER < MC_1_21_4
        this.renderTarget.clear(Minecraft.ON_OSX);
        #elif MC_VER > MC_1_21_4
        com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                java.util.Objects.requireNonNull(renderTarget.getColorTexture()),
                clearColor,
                java.util.Objects.requireNonNull(renderTarget.getDepthTexture()),
                0.0f
        );
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

    public void bind(FrameBufferBindPoint bindPoint, boolean setViewport) {
        #if MC_VER > MC_1_21_4
        glBindFramebuffer(GlFrameBuffer.resolveBindTarget(bindPoint), MinecraftRenderTargetUtil.getFboId(renderTarget));
        #else
        if (bindPoint == FrameBufferBindPoint.READ) {
            renderTarget.bindRead();
        } else {
            renderTarget.bindWrite(setViewport);
        }
        #endif

    }

    public void bind(FrameBufferBindPoint bindPoint) {
        bind(bindPoint, true);
    }

    public void unbind(FrameBufferBindPoint bindPoint) {
        glBindFramebuffer(GlFrameBuffer.resolveBindTarget(bindPoint), 0);
    }

    @Override
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        #if MC_VER > MC_1_21_4
        return switch (attachmentType) {
            case COLOR -> MinecraftRenderTargetUtil.getColorTexId(renderTarget);
            case DEPTH, DEPTH_STENCIL -> MinecraftRenderTargetUtil.getDepthTexId(renderTarget);
        };
        #else
        return switch (attachmentType) {
            case COLOR -> renderTarget.getColorTextureId();
            case DEPTH, DEPTH_STENCIL -> renderTarget.getDepthTextureId();
        };
        #endif

    }

    @Override
    public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
        return switch (attachmentType) {
            case COLOR -> FrameBufferTextureAdapter.ofColor(this);
            case DEPTH, DEPTH_STENCIL -> FrameBufferTextureAdapter.ofDepth(this);
        };
    }

    @Override
    public int getFrameBufferId() {
        #if MC_VER > MC_1_21_4
        return MinecraftRenderTargetUtil.getFboId(renderTarget);
        #else
        return renderTarget.frameBufferId;
        #endif
    }

    @Override
    public void setClearColor(float red, float green, float blue, float alpha) {
        #if MC_VER > MC_1_21_4
        clearColor = ColorUtil.color((int) (alpha * 255), (int) (red * 255), (int) (green * 255), (int) (blue * 255));
        #else
        renderTarget.setClearColor(red, green, blue, alpha);
        #endif

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
