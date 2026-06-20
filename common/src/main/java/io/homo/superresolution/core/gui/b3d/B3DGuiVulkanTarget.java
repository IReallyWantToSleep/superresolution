/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.gui.b3d;

#if MC_VER >= MC_26_2
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.vulkan.VulkanExternalTexture;
import io.homo.superresolution.core.graphics.vulkan.VulkanFramebuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import java.lang.reflect.Method;
import java.util.OptionalDouble;

import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_GENERAL;

public final class B3DGuiVulkanTarget implements AutoCloseable {
    private static Method vkImageMethod;
    private static Method vkImageViewMethod;

    private final int width;
    private final int height;
    private final int generation;
    private GpuTexture texture;
    private GpuTextureView textureView;
    private GpuSampler sampler;
    private VulkanExternalTexture srColorTexture;
    private VulkanTexture srDepthTexture;
    private VulkanFramebuffer framebuffer;

    private B3DGuiVulkanTarget(int width, int height, int generation) {
        this.width = width;
        this.height = height;
        this.generation = generation;
        create();
    }

    public static B3DGuiVulkanTarget create(int width, int height, int generation) {
        return new B3DGuiVulkanTarget(width, height, generation);
    }

    private void create() {
        int usage = GpuTexture.USAGE_TEXTURE_BINDING
                | GpuTexture.USAGE_RENDER_ATTACHMENT
                | GpuTexture.USAGE_COPY_SRC
                | GpuTexture.USAGE_COPY_DST;
        texture = RenderSystem.getDevice().createTexture(
                "SR UI Vulkan Target",
                usage,
                GpuFormat.RGBA8_UNORM,
                width,
                height,
                1,
                1
        );
        textureView = RenderSystem.getDevice().createTextureView(texture);
        sampler = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR,
                FilterMode.LINEAR,
                1,
                OptionalDouble.of(0.0)
        );

        long image = readVkImage(texture);
        long imageView = readVkImageView(textureView);
        srColorTexture = VulkanExternalTexture.fixedLayout(
                RenderSystems.vulkan().device(),
                image,
                imageView,
                TextureFormat.RGBA8,
                TextureType.Texture2D,
                width,
                height,
                1,
                VK_IMAGE_LAYOUT_GENERAL,
                TextureUsages.create()
                        .sampler()
                        .attachmentColor()
                        .transferSource()
                        .transferDestination()
        );
        srDepthTexture = (VulkanTexture) RenderSystems.vulkan().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .format(TextureFormat.DEPTH32F_STENCIL8)
                        .size(width, height)
                        .usages(TextureUsages.create().attachmentDepth())
                        .label("SR UI Vulkan Target Depth")
                        .build()
        );
        framebuffer = (VulkanFramebuffer) RenderSystems.vulkan().device().createFramebuffer(
                FramebufferDescription.create()
                        .colorAttachment(srColorTexture)
                        .depthAttachment(srDepthTexture)
                        .size(width, height)
                        .label("SR UI b3d Vulkan Framebuffer")
                        .build()
        );
    }

    private static long readVkImage(GpuTexture texture) {
        try {
            if (vkImageMethod == null) {
                vkImageMethod = texture.getClass().getMethod("vkImage");
            }
            return (Long) vkImageMethod.invoke(texture);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to read b3d Vulkan texture image", t);
        }
    }

    private static long readVkImageView(GpuTextureView view) {
        try {
            if (vkImageViewMethod == null) {
                vkImageViewMethod = view.getClass().getMethod("vkImageView");
            }
            return (Long) vkImageViewMethod.invoke(view);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to read b3d Vulkan texture view", t);
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int generation() {
        return generation;
    }

    public GpuTextureView textureView() {
        return textureView;
    }

    public GpuSampler sampler() {
        return sampler;
    }

    public VulkanFramebuffer framebuffer() {
        return framebuffer;
    }

    @Override
    public void close() {
        if (framebuffer != null) {
            framebuffer.destroy();
            framebuffer = null;
        }
        if (srDepthTexture != null) {
            srDepthTexture.destroy();
            srDepthTexture = null;
        }
        if (srColorTexture != null) {
            srColorTexture.destroy();
            srColorTexture = null;
        }
        if (sampler != null) {
            sampler.close();
            sampler = null;
        }
        if (textureView != null) {
            textureView.close();
            textureView = null;
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
    }
}
#else
public final class B3DGuiVulkanTarget implements AutoCloseable {
    @Override
    public void close() {
    }
}
#endif
