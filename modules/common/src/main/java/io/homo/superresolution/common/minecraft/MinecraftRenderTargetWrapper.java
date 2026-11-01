/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.core.graphics.impl.framebuffer.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.utils.ColorUtil;

import java.util.List;
#if MC_VER >= MC_26_3 && IS_VULKAN == 1
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
import com.mojang.renderpearl.backend.vulkan.VulkanConst;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.vulkan.VulkanExternalTexture;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_GENERAL;
#endif

#if MC_VER < MC_1_21_4
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30;

import java.util.List;
#endif


public class MinecraftRenderTargetWrapper implements IBindableFrameBuffer {
    public RenderTarget renderTarget;
    private int clearColor = ColorUtil.color(255, 0, 0, 0);


    public MinecraftRenderTargetWrapper(RenderTarget renderTarget) {
        this.renderTarget = renderTarget;
    }

    public static MinecraftRenderTargetWrapper of(RenderTarget renderTarget) {
        if (renderTarget == null) return null;
        MinecraftRenderTargetWrapper wrapper = new MinecraftRenderTargetWrapper(renderTarget);
        wrapper.clearColor = ColorUtil.color(255, 0, 0, 0);
        return wrapper;
    }

    public void clearFrameBuffer() {
        #if MC_VER > MC_26_1_2
        throw new UnsupportedOperationException();
        #else
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
        #endif

    }

    @Override
    public List<ColorAttachment> getColorAttachments() {
        return List.of(new ColorAttachment(0, getTexture(FrameBufferAttachmentType.Color)));
    }

    @Override
    public DepthStencilAttachment getDepthStencilAttachment() {
        return new DepthStencilAttachment(getTexture(FrameBufferAttachmentType.AnyDepth));
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
        #if MC_VER >= MC_26_3 && IS_VULKAN == 1
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            return;
        }
        #endif
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
        #if MC_VER >= MC_26_3 && IS_VULKAN == 1
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            var view = attachmentType == FrameBufferAttachmentType.Color
                    ? renderTarget.getColorTextureView() : renderTarget.getDepthTextureView();
            if (view == null || view.isClosed() || view.texture().isClosed()) {
                return null;
            }
            if (!(view instanceof VulkanGpuTextureView vkView)) {
                throw new IllegalStateException("Minecraft Vulkan render target has a non-Vulkan view");
            }
            return VulkanExternalTexture.fixedLayout(RenderSystems.vulkan().device(),
                    vkView.texture().vkImage(), vkView.vkImageView(),
                    TextureFormat.fromVk(VulkanConst.toVk(view.texture().getFormat())),
                    TextureType.Texture2D, view.getWidth(0), view.getHeight(0), view.mipLevels(),
                    VK_IMAGE_LAYOUT_GENERAL,
                    TextureUsages.create().sampler().transferSource());
        }
        #endif
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
