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

package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.texture.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanExternalTexture implements ITexture, VulkanLayoutTracked {

    private final VulkanDevice device;
    private final long image;
    private final long imageView;
    private final boolean ownsView;
    private final TextureFormat format;
    private final TextureType type;
    private final int width;
    private final int height;
    private final int aspectMask;
    private final int mipLevels;

    private int currentLayout;
    private boolean destroyed = false;

    public VulkanExternalTexture(
            VulkanDevice device,
            long image,
            long imageView,
            TextureFormat format,
            TextureType type,
            int width,
            int height,
            int mipLevels,
            int initialLayout
    ) {
        this.device = device;
        this.image = image;
        this.imageView = imageView;
        this.ownsView = false;
        this.format = format;
        this.type = type;
        this.width = width;
        this.height = height;
        this.mipLevels = mipLevels;
        this.currentLayout = initialLayout;
        this.aspectMask = resolveAspectMask(format);
    }

    private VulkanExternalTexture(
            VulkanDevice device,
            long image,
            long imageView,
            boolean ownsView,
            TextureFormat format,
            TextureType type,
            int width,
            int height,
            int mipLevels,
            int initialLayout
    ) {
        this.device = device;
        this.image = image;
        this.imageView = imageView;
        this.ownsView = ownsView;
        this.format = format;
        this.type = type;
        this.width = width;
        this.height = height;
        this.mipLevels = mipLevels;
        this.currentLayout = initialLayout;
        this.aspectMask = resolveAspectMask(format);
    }


    public static VulkanExternalTexture withAutoView(
            VulkanDevice device,
            long image,
            TextureFormat format,
            TextureType type,
            int width,
            int height,
            int mipLevels,
            int initialLayout
    ) {
        long view;
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format.vk());
            viewInfo.components()
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            viewInfo.subresourceRange()
                    .aspectMask(resolveAspectMask(format))
                    .baseMipLevel(0)
                    .levelCount(mipLevels)
                    .baseArrayLayer(0)
                    .layerCount(1);
            var pView = stack.mallocLong(1);
            if (vkCreateImageView(device.getVkDevice(), viewInfo, null, pView) != VK_SUCCESS) {
                throw new RuntimeException("VulkanExternalTexture: 创建 VkImageView 失败");
            }
            view = pView.get(0);
        }
        return new VulkanExternalTexture(
                device, image, view, true, format, type, width, height, mipLevels, initialLayout
        );
    }

    private static int resolveAspectMask(TextureFormat format) {
        if (format.isDepthStencil()) {
            return VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT;
        } else if (format.isDepth()) {
            return VK_IMAGE_ASPECT_DEPTH_BIT;
        }
        return VK_IMAGE_ASPECT_COLOR_BIT;
    }

    public int getCurrentLayout() {
        return currentLayout;
    }

    public void setCurrentLayout(int layout) {
        this.currentLayout = layout;
    }

    public int getMipLevels() {
        return mipLevels;
    }

    public int getAspectMask() {
        return aspectMask;
    }

    public void transitionImageLayout(
            VulkanCommandBuffer commandBuffer,
            int newLayout,
            int srcStageMask,
            int dstStageMask,
            int srcAccessMask,
            int dstAccessMask
    ) {
        if (currentLayout == newLayout) {
            return;
        }
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .srcAccessMask(srcAccessMask)
                    .dstAccessMask(dstAccessMask)
                    .oldLayout(currentLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .subresourceRange(VkImageSubresourceRange.calloc(stack)
                            .aspectMask(aspectMask)
                            .baseMipLevel(0)
                            .levelCount(mipLevels)
                            .baseArrayLayer(0)
                            .layerCount(1));
            vkCmdPipelineBarrier(
                    commandBuffer.getNativeCommandBuffer(),
                    srcStageMask,
                    dstStageMask,
                    0,
                    null,
                    null,
                    barrier
            );
        }
        currentLayout = newLayout;
    }

    @Override
    public long handle() {
        return image;
    }

    public long getImageView() {
        return imageView;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return format;
    }

    @Override
    public TextureUsages getTextureUsages() {
        return TextureUsages.create();
    }

    @Override
    public TextureType getTextureType() {
        return type;
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return getTextureDescription().getFilterMode();
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return getTextureDescription().getWrapMode();
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return getTextureDescription().getMipmapSettings();
    }

    @Override
    public TextureDescription getTextureDescription() {
        return TextureDescription.create()
                .format(format)
                .type(type)
                .size(width, height)
                .usages(TextureUsages.create())
                .filterMode(TextureFilterMode.Linear)
                .wrapMode(TextureWrapMode.ClampToEdge)
                .mipmapSettings(TextureMipmapSettings.manual(mipLevels))
                .label("VulkanExternalTexture")
                .build();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }
        if (ownsView && imageView != VK_NULL_HANDLE) {
            vkDestroyImageView(device.getVkDevice(), imageView, null);
        }
        destroyed = true;
    }
}
