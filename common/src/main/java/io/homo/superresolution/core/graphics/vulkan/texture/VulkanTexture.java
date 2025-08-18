package io.homo.superresolution.core.graphics.vulkan.texture;

import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanInterop;
import io.homo.superresolution.core.graphics.vulkan.utils.VulkanException;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.Set;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK11.*;

public class VulkanTexture implements ITexture {
    private final VulkanDevice device;
    private final TextureDescription description;
    private final boolean isExternal;
    private final long memoryHandle;
    private final boolean exportable;
    private long exportedHandle = -1;
    private long image;
    private long imageMemory;
    private long imageView;
    private int width;
    private int height;

    public long getMemorySize() {
        return memorySize;
    }

    private long memorySize;

    public VulkanTexture(VulkanDevice device, TextureDescription description) {
        this(device, description, false, -1, false);
    }

    @Override
    public TextureMipmapSettings getMipmapSettings() {
        return description.getMipmapSettings();
    }

    public VulkanTexture(VulkanDevice device, TextureDescription description, long memoryHandle) {
        this(device, description, true, memoryHandle, false);
    }

    public VulkanDevice getDevice() {
        return device;
    }

    public VulkanTexture(VulkanDevice device, TextureDescription description, boolean isExternal, long memoryHandle, boolean exportable) {
        this.device = device;
        this.description = description;
        this.width = description.getWidth();
        this.height = description.getHeight();
        this.isExternal = isExternal;
        this.memoryHandle = memoryHandle;
        this.exportable = exportable;

        try (MemoryStack stack = stackPush()) {
            createImage(stack);
            if (isExternal) {
                importMemoryFromHandle(stack);
            } else {
                allocateMemory(stack);
                if (exportable) {
                    exportMemoryHandle(stack);
                }
            }
            createImageView(stack);
        }
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
            VkExternalMemoryImageCreateInfo extInfo = VkExternalMemoryImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);

            imageInfo.pNext(extInfo);
        }

        LongBuffer pImage = stack.mallocLong(1);
        VK_CHECK(vkCreateImage(device.getVkDevice(), imageInfo, null, pImage), "Failed to create image");
        image = pImage.get(0);
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
        VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
        vkGetImageMemoryRequirements(device.getVkDevice(), image, memRequirements);
        this.memorySize = memRequirements.size();

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memorySize)
                .memoryTypeIndex(findMemoryType(
                        memRequirements.memoryTypeBits(),
                        VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

        if (exportable) {
            allocInfo.pNext(VulkanInterop.IMPL.createVkExportMemoryAllocateInfo(stack).address());
        }

        LongBuffer pMemory = stack.mallocLong(1);
        VK_CHECK(vkAllocateMemory(device.getVkDevice(), allocInfo, null, pMemory),
                "Failed to allocate image memory");
        imageMemory = pMemory.get(0);

        vkBindImageMemory(device.getVkDevice(), image, imageMemory, 0);
    }


    private void importMemoryFromHandle(MemoryStack stack) {
        VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
        vkGetImageMemoryRequirements(device.getVkDevice(), image, memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .pNext(VulkanInterop.IMPL.createVkImportMemoryInfo(stack, memoryHandle).address())
                .allocationSize(memRequirements.size())
                .memoryTypeIndex(findMemoryType(
                        memRequirements.memoryTypeBits(),
                        VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

        LongBuffer pMemory = stack.mallocLong(1);
        VK_CHECK(vkAllocateMemory(device.getVkDevice(), allocInfo, null, pMemory),
                "Failed to import external memory");
        imageMemory = pMemory.get(0);
        VK_CHECK(vkBindImageMemory(device.getVkDevice(), image, imageMemory, 0),
                "Failed to bind imported memory to image");
    }


    private int findMemoryType(int typeFilter, int properties) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0 &&
                        (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }
        throw new VulkanException("Failed to find suitable memory type");
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

    private int getAspectMask() {
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
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void destroy() {
        if (imageView != VK_NULL_HANDLE) {
            vkDestroyImageView(device.getVkDevice(), imageView, null);
            imageView = VK_NULL_HANDLE;
        }

        if (image != VK_NULL_HANDLE) {
            vkDestroyImage(device.getVkDevice(), image, null);
            image = VK_NULL_HANDLE;
        }

        if (imageMemory != VK_NULL_HANDLE) {
            vkFreeMemory(device.getVkDevice(), imageMemory, null);
            imageMemory = VK_NULL_HANDLE;
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

    @Override
    public void resize(int newWidth, int newHeight) {
        if (newWidth == width && newHeight == height) return;

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
        }
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

    @Override
    public TextureDescription getTextureDescription() {
        return description;
    }
}