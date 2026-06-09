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

import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.Set;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK11.*;

public class VulkanTexture implements ITexture, VulkanLayoutTracked {
    private final VulkanDevice device;
    private final VulkanMemoryAllocator allocator;
    private final TextureDescription description;
    private final boolean isExternal;
    private final long memoryHandle;
    private final boolean exportable;
    private long exportedHandle = -1;
    private long image;
    private long imageMemory;
    private long vmaAllocation = VK_NULL_HANDLE;
    private long imageView;
    private int width;
    private int height;
    private long memorySize;
    private VulkanResourceState currentState = VulkanResourceState.UNDEFINED;

    public VulkanTexture(VulkanDevice device, TextureDescription description) {
        this(device, description, false, -1, false);
    }

    public VulkanTexture(VulkanDevice device, TextureDescription description, long memoryHandle) {
        this(device, description, true, memoryHandle, false);
    }

    public VulkanTexture(VulkanDevice device, TextureDescription description, boolean isExternal, long memoryHandle, boolean exportable) {
        this.device = device;
        this.allocator = device.getMemoryAllocator();
        this.description = description;
        this.width = description.getWidth();
        this.height = description.getHeight();
        this.isExternal = isExternal;
        this.memoryHandle = memoryHandle;
        this.exportable = exportable;

        try (MemoryStack stack = stackPush()) {
            if (!isExternal && !exportable) {
                createImageVma(stack);
            } else {
                createImage(stack);
            }
            if (isExternal) {
                importMemoryFromHandle(stack);
            } else if (exportable) {
                allocateMemory(stack);
                exportMemoryHandle(stack);
            }
            createImageView(stack);
            updateDebugLabels();
        }
    }

    private String debugBaseLabel() {
        String label = description.getLabel();
        if (label != null && !label.isBlank()) {
            return label;
        }
        return "VulkanTexture " + description.getFormat() + " " + width + "x" + height;
    }

    private void updateDebugLabels() {
        String baseLabel = debugBaseLabel();
        device.setDebugName(VK_OBJECT_TYPE_IMAGE, image, baseLabel + " Image");
        device.setDebugName(VK_OBJECT_TYPE_IMAGE_VIEW, imageView, baseLabel + " ImageView");
        if ((isExternal || exportable) && imageMemory != VK_NULL_HANDLE) {
            device.setDebugName(VK_OBJECT_TYPE_DEVICE_MEMORY, imageMemory, baseLabel + " Memory");
        }
    }

    public long getMemorySize() {
        return memorySize;
    }

    public VulkanDevice getDevice() {
        return device;
    }

    private void exportMemoryHandle(MemoryStack stack) {
        exportedHandle = VulkanInterop.IMPL.vkGetMemoryHandle(stack, device.getVkDevice(), imageMemory);
    }

    private void createImage(MemoryStack stack) {
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
        imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        imageInfo.imageType(VK_IMAGE_TYPE_2D);
        imageInfo.extent().width(width);
        imageInfo.extent().height(height);
        imageInfo.extent().depth(1);
        imageInfo.mipLevels(description.getMipmapSettings().getLevels());
        imageInfo.arrayLayers(1);
        imageInfo.format(description.getFormat().vk());
        imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
        imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        imageInfo.usage(translateUsages(Set.copyOf(description.getUsages().getUsages())));
        imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);

        if (isExternal || exportable) {
            VkExternalMemoryImageCreateInfo extInfo;
            //TODO:移到VulkanInterop
            if (OperatingSystemType.isCurrentOS(OperatingSystemType.WINDOWS)) {
                extInfo = VkExternalMemoryImageCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                        .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
                // Here,still WIN32,though. Is this used for renderdoc?
            } else {
                extInfo = VkExternalMemoryImageCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                        .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT);
                // Now it's Linux. Other systems are not supported.
            }
            imageInfo.pNext(extInfo);
        }

        LongBuffer pImage = stack.mallocLong(1);
        VK_CHECK(vkCreateImage(device.getVkDevice(), imageInfo, null, pImage), "Failed to create image");
        image = pImage.get(0);
    }

    private void createImageVma(MemoryStack stack) {
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
        imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
        imageInfo.imageType(VK_IMAGE_TYPE_2D);
        imageInfo.extent().width(width);
        imageInfo.extent().height(height);
        imageInfo.extent().depth(1);
        imageInfo.mipLevels(description.getMipmapSettings().getLevels());
        imageInfo.arrayLayers(1);
        imageInfo.format(description.getFormat().vk());
        imageInfo.tiling(VK_IMAGE_TILING_OPTIMAL);
        imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        imageInfo.usage(translateUsages(Set.copyOf(description.getUsages().getUsages())));
        imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
        imageInfo.samples(VK_SAMPLE_COUNT_1_BIT);

        VmaAllocationCreateInfo allocCI = VmaAllocationCreateInfo.calloc(stack);
        allocCI.usage(Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE);

        PointerBuffer pAllocation = stack.mallocPointer(1);
        image = allocator.createImageVma(imageInfo, allocCI, pAllocation);
        vmaAllocation = pAllocation.get(0);
        imageMemory = allocator.getDeviceMemoryFromAllocation(vmaAllocation);
        this.memorySize = allocator.getImageMemoryRequirements(image);
    }

    private int translateUsages(Set<TextureUsage> usages) {
        int flags = 0;
        for (TextureUsage usage : usages) {
            switch (usage) {
                case Sampler:
                    flags |= VK_IMAGE_USAGE_SAMPLED_BIT;
                    break;
                case Storage:
                    flags |= VK_IMAGE_USAGE_STORAGE_BIT;
                    break;
                case AttachmentColor:
                    flags |= VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                    break;
                case AttachmentDepth:
                    flags |= VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
                    break;
                case TransferSource:
                    flags |= VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
                    break;
                case TransferDestination:
                    flags |= VK_IMAGE_USAGE_TRANSFER_DST_BIT;
                    break;
            }
        }
        return flags;
    }

    private void allocateMemory(MemoryStack stack) {
        if (exportable) {
            VkMemoryDedicatedAllocateInfo dedicatedAllocInfo = VkMemoryDedicatedAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO)
                    .image(image);
            dedicatedAllocInfo.pNext(VulkanInterop.IMPL.createVkExportMemoryAllocateInfo(stack).address());
            imageMemory = allocator.allocateImageMemory(
                    image,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    dedicatedAllocInfo.address());
            this.memorySize = allocator.getImageMemoryRequirements(image);
        } else {
            vmaAllocation = allocator.allocateImageMemoryVma(image, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
            imageMemory = allocator.getDeviceMemoryFromAllocation(vmaAllocation);
            this.memorySize = allocator.getImageMemoryRequirements(image);
        }
    }

    private void importMemoryFromHandle(MemoryStack stack) {
        this.memorySize = allocator.getImageMemoryRequirements(image);
        imageMemory = allocator.allocateImageMemory(
                image,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                VulkanInterop.IMPL.createVkImportMemoryInfo(stack, memoryHandle).address());
    }

    private void createImageView(MemoryStack stack) {
        VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
        viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
        viewInfo.image(image);
        viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
        viewInfo.format(description.getFormat().vk());

        viewInfo.components()
                .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                .a(VK_COMPONENT_SWIZZLE_IDENTITY);

        viewInfo.subresourceRange()
                .aspectMask(getAspectMask())
                .baseMipLevel(0)
                .levelCount(description.getMipmapSettings().getLevels())
                .baseArrayLayer(0)
                .layerCount(1);
        LongBuffer pImageView = stack.mallocLong(1);
        VK_CHECK(vkCreateImageView(device.getVkDevice(), viewInfo, null, pImageView), "Failed to create image view");
        imageView = pImageView.get(0);
    }

    public int getAspectMask() {
        if (description.getFormat().isDepthStencil()) {
            return VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT;
        } else if (description.getFormat().isDepth()) {
            return VK_IMAGE_ASPECT_DEPTH_BIT;
        }
        return VK_IMAGE_ASPECT_COLOR_BIT;
    }

    @Override
    public TextureFormat getTextureFormat() {
        return description.getFormat();
    }

    @Override
    public TextureUsages getTextureUsages() {
        return description.getUsages();
    }

    @Override
    public TextureType getTextureType() {
        return description.getType();
    }

    @Override
    public TextureFilterMode getTextureFilterMode() {
        return description.getFilterMode();
    }

    @Override
    public TextureWrapMode getTextureWrapMode() {
        return description.getWrapMode();
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return description.getMipmapSettings();
    }

    @Override
    public TextureDescription getTextureDescription() {
        return description;
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
        long imageViewToDestroy = imageView;
        long imageToDestroy = image;
        long allocationToDestroy = vmaAllocation;
        long imageMemoryToDestroy = imageMemory;
        imageView = VK_NULL_HANDLE;
        image = VK_NULL_HANDLE;
        vmaAllocation = VK_NULL_HANDLE;
        imageMemory = VK_NULL_HANDLE;

        if (imageViewToDestroy != VK_NULL_HANDLE) {
            device.queueForDestroy(() -> vkDestroyImageView(device.getVkDevice(), imageViewToDestroy, null));
        }

        if (allocationToDestroy != VK_NULL_HANDLE) {
            device.queueForDestroy(() -> allocator.freeImageVma(imageToDestroy, allocationToDestroy));
        } else if (imageMemoryToDestroy != VK_NULL_HANDLE) {
            device.queueForDestroy(() -> allocator.freeImage(imageToDestroy, imageMemoryToDestroy));
        }
    }

    public long getExportedMemoryHandle() {
        if (!exportable) {
            throw new VulkanException("Texture is not exportable");
        }
        if (exportedHandle == -1) {
            throw new VulkanException("Memory handle not exported");
        }
        return exportedHandle;
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth == width && newHeight == height) {
            return;
        }

        destroy();
        this.width = newWidth;
        this.height = newHeight;

        try (MemoryStack stack = stackPush()) {
            createImage(stack);
            if (isExternal) {
                throw new VulkanException("Cannot resize external memory texture");
            } else {
                allocateMemory(stack);
            }
            createImageView(stack);
            updateDebugLabels();
        }
    }

    public void transitionImageLayout(
            VulkanCommandBuffer commandBuffer,
            int newLayout,
            int srcStageMask,
            int dstStageMask,
            int dependencyFlags
    ) {
        if (currentState.layout() == newLayout) {
            return;
        }
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(currentState.layout())
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .subresourceRange(VkImageSubresourceRange.calloc(stack)
                            .aspectMask(getAspectMask())
                            .baseMipLevel(0)
                            .levelCount(description.getMipmapSettings().getLevels())
                            .baseArrayLayer(0)
                            .layerCount(1)
                    );
            vkCmdPipelineBarrier(
                    commandBuffer.getNativeCommandBuffer(),
                    srcStageMask,
                    dstStageMask,
                    dependencyFlags,
                    null,
                    null,
                    barrier
            );
        }
        currentState = new VulkanResourceState(newLayout, 0, dstStageMask, currentState.accessType());
    }

    public int getCurrentLayout() {
        return currentState.layout();
    }

    public void setCurrentLayout(int layout) {
        this.currentState = new VulkanResourceState(
                layout,
                currentState.accessMask(),
                currentState.stageMask(),
                currentState.accessType()
        );
    }

    @Override
    public VulkanResourceState getCurrentResourceState() {
        return currentState;
    }

    @Override
    public void setCurrentResourceState(VulkanResourceState state) {
        this.currentState = state != null ? state : VulkanResourceState.UNDEFINED;
    }

    @Override
    public long handle() {
        return image;
    }

    public long getImageView() {
        return imageView;
    }

    public long getImageMemory() {
        return imageMemory;
    }

    public long getMemoryHandle() {
        if (isExternal) {
            return memoryHandle;
        }
        throw new VulkanException("Texture does not have external memory");
    }
}
