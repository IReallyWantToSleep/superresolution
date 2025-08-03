package io.homo.superresolution.common.minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.core.graphics.impl.framebuffer.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.utils.ColorUtil;

#if MC_VER < MC_1_21_4
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30;
#endif


public class MinecraftRenderTargetWrapper implements IBindableFrameBuffer {
    public RenderTarget renderTarget;
    private int clearColor = ColorUtil.color(255, 0, 0, 0);

    MinecraftRenderTargetWrapper(RenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    public static MinecraftRenderTargetWrapper of(RenderTarget renderTarget) {
        if (renderTarget == null) return null;
        MinecraftRenderTargetWrapper wrapper = new MinecraftRenderTargetWrapper(renderTarget);
        wrapper.clearColor = ColorUtil.color(255, 0, 0, 0);
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
        org.lwjgl.opengl.GL30.glBindFramebuffer(GlFrameBuffer.resolveBindTarget(bindPoint), MinecraftRenderTargetUtil.getFboId(renderTarget));
        #else
        if (bindPoint == FrameBufferBindPoint.Read) {
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
        org.lwjgl.opengl.GL30.glBindFramebuffer(GlFrameBuffer.resolveBindTarget(bindPoint), 0);
    }

    @Override
    public int getTextureId(FrameBufferAttachmentType attachmentType) {
        #if MC_VER > MC_1_21_4
        return switch (attachmentType) {
            case Color -> MinecraftRenderTargetUtil.getColorTexId(renderTarget);
            case AnyDepth, Depth, DepthStencil -> MinecraftRenderTargetUtil.getDepthTexId(renderTarget);
        };
        #else
        return switch (attachmentType) {
            case Color -> renderTarget.getColorTextureId();
            case AnyDepth, Depth, DepthStencil -> renderTarget.getDepthTextureId();
        };
        #endif

    }

    @Override
    public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
        return switch (attachmentType) {
            case Color -> FrameBufferTextureAdapter.ofColor(this);
            case AnyDepth, Depth, DepthStencil -> FrameBufferTextureAdapter.ofDepth(this);
        };
    }

    @Override
    public long handle() {
        #if MC_VER > MC_1_21_4
        return MinecraftRenderTargetUtil.getFboId(renderTarget);
        #else
        return renderTarget.frameBufferId;
        #endif
    }

    @Override
    public void setClearColorRGBA(float red, float green, float blue, float alpha) {
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
        #if MC_VER > MC_1_21_5
        return TextureFormat.DEPTH32F;
        #else
        return TextureFormat.DEPTH32F;
        #endif
    }


    @Override
    public RenderTarget asMcRenderTarget() {
        return renderTarget;
    }
}
