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
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanTextureView implements ITextureView {
    private final VulkanDevice device;
    private final ITexture parent;
    private final TextureViewDescription viewDescription;
    private long imageView;

    public VulkanTextureView(VulkanDevice device, TextureViewDescription description) {
        this.device = device;
        this.parent = description.getParent();
        this.viewDescription = description;

        if (!(parent instanceof VulkanTexture vkParent)) {
            throw new IllegalArgumentException("Parent texture must be a VulkanTexture");
        }

        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(vkParent.handle())
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(parent.getTextureFormat().vk());

            viewInfo.components()
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);

            viewInfo.subresourceRange()
                    .aspectMask(getAspectMask())
                    .baseMipLevel(description.getBaseMipLevel())
                    .levelCount(description.getMipLevelCount())
                    .baseArrayLayer(0)
                    .layerCount(1);

            LongBuffer pImageView = stack.mallocLong(1);
            VK_CHECK(vkCreateImageView(device.getVkDevice(), viewInfo, null, pImageView), "Failed to create image view");
            imageView = pImageView.get(0);
        }
    }

    private int getAspectMask() {
        if (parent.getTextureFormat().isDepthStencil()) {
            return VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT;
        } else if (parent.getTextureFormat().isDepth()) {
            return VK_IMAGE_ASPECT_DEPTH_BIT;
        }
        return VK_IMAGE_ASPECT_COLOR_BIT;
    }

    @Override
    public ITexture getParent() {
        return parent;
    }

    @Override
    public TextureViewDescription getViewDescription() {
        return viewDescription;
    }

    @Override
    public long handle() {
        return imageView;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return parent.getTextureFormat();
    }

    @Override
    public TextureUsages getTextureUsages() {
        return parent.getTextureUsages();
    }

    @Override
    public TextureType getTextureType() {
        return parent.getTextureType();
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return parent.getTextureFilterMode();
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return parent.getTextureWrapMode();
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return parent.getMipmapSettings();
    }

    @Override
    public TextureDescription getTextureDescription() {
        return parent.getTextureDescription();
    }

    @Override
    public int getWidth() {
        return viewDescription.getWidth();
    }

    @Override
    public int getHeight() {
        return viewDescription.getHeight();
    }

    @Override
    public void destroy() {
        if (imageView != VK_NULL_HANDLE) {
            vkDestroyImageView(device.getVkDevice(), imageView, null);
            imageView = VK_NULL_HANDLE;
        }
    }

    @Override
    public String toString() {
        return "VulkanTextureView{" +
                "imageView=" + imageView +
                ", viewDescription=" + viewDescription +
                '}';
    }
}


