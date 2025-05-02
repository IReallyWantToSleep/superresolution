package io.homo.superresolution.core.interop.texture;

import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.interop.memory.SharedMemory;
import io.homo.superresolution.core.vulkan.texture.TextureUsage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.core.vulkan.Utils.VK_CHECK;
import static org.lwjgl.opengl.EXTMemoryObjectFD.*;
import static org.lwjgl.opengl.EXTMemoryObjectFD.GL_HANDLE_TYPE_OPAQUE_FD_EXT;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.vulkan.KHRExternalMemoryFd.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.vulkan.VK11.*;

public class VkToGlTexture {
    private final VkDevice device;
    private final VkPhysicalDevice physicalDevice;
    private final int width;
    private final int height;
    private final TextureFormat format;
    private final TextureUsage usage;
    private final SharedMemory sharedMemory = new SharedMemory();
    private long vkImage = VK_NULL_HANDLE;
    private long vkMemory = VK_NULL_HANDLE;
    private long vkImageView = VK_NULL_HANDLE;
    // OpenGL资源
    private int glTexture = GL_NONE;
    private int glMemoryObject = GL_NONE;

    public VkToGlTexture(VkDevice device,
                         VkPhysicalDevice physicalDevice,
                         int width,
                         int height,
                         TextureFormat format,
                         TextureUsage usage) {
        this.device = device;
        this.physicalDevice = physicalDevice;
        this.width = width;
        this.height = height;
        this.format = format;
        this.usage = usage;
    }

    // 创建Vulkan图像和内存
    public void createVulkanResources() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 创建可导出的Vulkan图像
            VkExternalMemoryImageCreateInfo extImgInfo = VkExternalMemoryImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT);

            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .pNext(extImgInfo)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(format.vk())
                    .extent(VkExtent3D.calloc(stack)
                            .width(width)
                            .height(height)
                            .depth(1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(usage.getValue() | VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

            LongBuffer pImage = stack.mallocLong(1);
            VK_CHECK(vkCreateImage(device, imageInfo, null, pImage),
                    "Failed to create Vulkan image");
            vkImage = pImage.get(0);

            // 分配可导出内存
            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, vkImage, memReqs);

            VkExportMemoryAllocateInfo exportInfo = VkExportMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .pNext(exportInfo)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(findMemoryType(memReqs.memoryTypeBits(),
                            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

            LongBuffer pMemory = stack.mallocLong(1);
            VK_CHECK(vkAllocateMemory(device, allocInfo, null, pMemory),
                    "Failed to allocate Vulkan memory");
            vkMemory = pMemory.get(0);
            vkBindImageMemory(device, vkImage, vkMemory, 0);

            // 获取文件描述符
            VkMemoryGetFdInfoKHR getFdInfo = VkMemoryGetFdInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_GET_FD_INFO_KHR)
                    .memory(vkMemory)
                    .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT);

            IntBuffer pFd = stack.mallocInt(1);
            KHRExternalMemoryFd.vkGetMemoryFdKHR(device, getFdInfo, pFd);
            sharedMemory.fd = pFd.get(0);
        }
    }

    // 创建OpenGL资源
    public void createGLResources() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 导入内存到OpenGL
            IntBuffer pMemObj = stack.mallocInt(1);
            glCreateMemoryObjectsEXT(pMemObj);
            glMemoryObject = pMemObj.get(0);
            glImportMemoryFdEXT(glMemoryObject,
                    getMemorySize(),
                    GL_HANDLE_TYPE_OPAQUE_FD_EXT,
                    sharedMemory.fd);

            // 创建纹理对象
            IntBuffer pTexture = stack.mallocInt(1);
            glGenTextures(pTexture);
            glTexture = pTexture.get(0);

            glBindTexture(GL_TEXTURE_2D, glTexture);
            glTexStorageMem2DEXT(GL_TEXTURE_2D,
                    1,
                    format.gl(),
                    width,
                    height,
                    glMemoryObject,
                    0);
            setTextureParameters();
            glBindTexture(GL_TEXTURE_2D, GL_NONE);
        }
    }

    // 设置纹理参数
    private void setTextureParameters() {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    // 创建Vulkan图像视图
    public void createImageView() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(vkImage)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format.vk())
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            LongBuffer pView = stack.mallocLong(1);
            VK_CHECK(vkCreateImageView(device, viewInfo, null, pView),
                    "Failed to create image view");
            vkImageView = pView.get(0);
        }
    }

    // 清理资源
    public void cleanup() {
        // 清理OpenGL资源
        if (glTexture != GL_NONE) {
            glDeleteTextures(glTexture);
            glTexture = GL_NONE;
        }
        if (glMemoryObject != GL_NONE) {
            glDeleteMemoryObjectsEXT(glMemoryObject);
            glMemoryObject = GL_NONE;
        }

        // 清理Vulkan资源
        if (vkImageView != VK_NULL_HANDLE) {
            vkDestroyImageView(device, vkImageView, null);
            vkImageView = VK_NULL_HANDLE;
        }
        if (vkImage != VK_NULL_HANDLE) {
            vkDestroyImage(device, vkImage, null);
            vkImage = VK_NULL_HANDLE;
        }
        if (vkMemory != VK_NULL_HANDLE) {
            vkFreeMemory(device, vkMemory, null);
            vkMemory = VK_NULL_HANDLE;
        }
    }

    // 查找兼容的内存类型
    private int findMemoryType(int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc();
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps);

        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0
                    && (memProps.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable memory type!");
    }

    // 获取内存大小
    private long getMemorySize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(device, vkImage, memReqs);
            return memReqs.size();
        }
    }

    // Getter方法
    public long getVkImage() {
        return vkImage;
    }

    public int getGlTexture() {
        return glTexture;
    }

    public long getVkImageView() {
        return vkImageView;
    }
}