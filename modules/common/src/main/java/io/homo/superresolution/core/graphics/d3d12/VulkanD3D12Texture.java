package io.homo.superresolution.core.graphics.d3d12;

import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanExternalTexture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExternalMemoryImageCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkImportMemoryWin32HandleInfoKHR;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryDedicatedAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkMemoryWin32HandlePropertiesKHR;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.*;
import static org.lwjgl.vulkan.VK11.*;

/** Vulkan image bound directly to a borrowed D3D12 committed resource. */
public final class VulkanD3D12Texture extends VulkanExternalTexture {
    private final VulkanDevice device;
    private final D3D12Texture2D source;
    private final long memory;
    private D3D12Texture2D.ExternalBorrowLease sourceBorrow;
    private boolean destroyed;

    public VulkanD3D12Texture(VulkanDevice device, D3D12Texture2D source) {
        this(device, source, source.device().withLifecycleLock(() -> create(device, source)));
    }

    private VulkanD3D12Texture(VulkanDevice device, D3D12Texture2D source, Imported imported) {
        super(device, imported.image(), imported.view(), source.getTextureFormat(), source.getTextureType(),
                source.getWidth(), source.getHeight(), 1, VK_IMAGE_LAYOUT_UNDEFINED);
        this.device = device;
        this.source = source;
        this.memory = imported.memory();
        this.sourceBorrow = imported.borrow();
    }

    private static Imported create(VulkanDevice device, D3D12Texture2D source) {
        if (!source.isShared()) {
            throw new IllegalArgumentException("Vulkan requires a shared D3D12 committed texture");
        }
        D3D12Texture2D.ExternalBorrowLease borrow = source.borrowExternal();
        long image = VK_NULL_HANDLE;
        long memory = VK_NULL_HANDLE;
        long view = VK_NULL_HANDLE;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExternalMemoryImageCreateInfo external = VkExternalMemoryImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_D3D12_RESOURCE_BIT);
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO).pNext(external)
                    .imageType(VK_IMAGE_TYPE_2D).format(source.getTextureFormat().vk())
                    .mipLevels(1).arrayLayers(1).samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL).sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT
                            | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);
            imageInfo.extent().set(source.getWidth(), source.getHeight(), 1);
            var result = stack.mallocLong(1);
            VK_CHECK(vkCreateImage(device.getVkDevice(), imageInfo, null, result),
                    "Create D3D12 imported Vulkan image");
            image = result.get(0);
            VkMemoryRequirements requirements = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device.getVkDevice(), image, requirements);
            VkMemoryWin32HandlePropertiesKHR properties = VkMemoryWin32HandlePropertiesKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_WIN32_HANDLE_PROPERTIES_KHR);
            VK_CHECK(vkGetMemoryWin32HandlePropertiesKHR(device.getVkDevice(),
                    VK_EXTERNAL_MEMORY_HANDLE_TYPE_D3D12_RESOURCE_BIT, source.sharedHandleLocked(), properties),
                    "Query D3D12 resource Vulkan memory types");
            if (source.allocationSize() < requirements.size()) {
                throw new IllegalStateException("D3D12 allocation is smaller than the Vulkan image requirements");
            }
            VkImportMemoryWin32HandleInfoKHR importInfo = VkImportMemoryWin32HandleInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMPORT_MEMORY_WIN32_HANDLE_INFO_KHR)
                    .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_D3D12_RESOURCE_BIT)
                    .handle(source.sharedHandleLocked());
            VkMemoryDedicatedAllocateInfo dedicated = VkMemoryDedicatedAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO).image(image).pNext(importInfo.address());
            VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO).pNext(dedicated)
                    .allocationSize(source.allocationSize())
                    .memoryTypeIndex(device.getMemoryAllocator().findMemoryType(
                            requirements.memoryTypeBits() & properties.memoryTypeBits(),
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));
            VK_CHECK(vkAllocateMemory(device.getVkDevice(), allocation, null, result),
                    "Import D3D12 committed resource into Vulkan");
            memory = result.get(0);
            VK_CHECK(vkBindImageMemory(device.getVkDevice(), image, memory, 0),
                    "Bind D3D12 imported Vulkan image");
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).image(image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D).format(source.getTextureFormat().vk());
            viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0)
                    .levelCount(1).baseArrayLayer(0).layerCount(1);
            VK_CHECK(vkCreateImageView(device.getVkDevice(), viewInfo, null, result),
                    "Create D3D12 imported Vulkan image view");
            view = result.get(0);
            return new Imported(image, memory, view, borrow);
        } catch (RuntimeException | Error failure) {
            if (view != VK_NULL_HANDLE) vkDestroyImageView(device.getVkDevice(), view, null);
            if (image != VK_NULL_HANDLE) vkDestroyImage(device.getVkDevice(), image, null);
            if (memory != VK_NULL_HANDLE) vkFreeMemory(device.getVkDevice(), memory, null);
            borrow.close();
            throw failure;
        }
    }

    @Override
    public synchronized void destroy() {
        if (destroyed) {
            return;
        }
        // The interop owner has retired all encoder/presentation users first.
        vkDestroyImageView(device.getVkDevice(), getImageView(), null);
        vkDestroyImage(device.getVkDevice(), handle(), null);
        vkFreeMemory(device.getVkDevice(), memory, null);
        sourceBorrow.close();
        sourceBorrow = null;
        destroyed = true;
    }

    private record Imported(long image, long memory, long view, D3D12Texture2D.ExternalBorrowLease borrow) {
    }
}
