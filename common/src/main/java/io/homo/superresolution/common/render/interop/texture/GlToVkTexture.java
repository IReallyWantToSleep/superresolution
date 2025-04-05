package io.homo.superresolution.common.render.interop.texture;

import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.render.interop.memory.SharedMemory;
import io.homo.superresolution.common.render.vulkan.VkDeviceManager;
import io.homo.superresolution.common.render.vulkan.texture.VkAllocatedImage;
import org.lwjgl.opengl.EXTMemoryObjectFD;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.common.render.vulkan.Utils.VK_CHECK;
import static org.lwjgl.opengl.EXTMemoryObjectFD.*;
import static org.lwjgl.opengl.EXTMemoryObjectFD.GL_HANDLE_TYPE_OPAQUE_FD_EXT;
import static org.lwjgl.opengl.EXTSemaphoreFD.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.vulkan.KHRExternalMemoryFd.*;
import static org.lwjgl.vulkan.KHRExternalSemaphoreFd.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.vulkan.VK11.*;

public class GlToVkTexture {
    private final VkDeviceManager deviceManager;
    public VkAllocatedImage vkImage = new VkAllocatedImage();
    public int glMemoryObject;
    public int glTextureId;
    public int width;
    public int height;
    public SharedMemory memory = new SharedMemory();
    protected TextureFormat format;

    public GlToVkTexture(int width, int height, VkDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
        this.width = width;
        this.height = height;
    }

    public GlToVkTexture setFormat(TextureFormat format) {
        this.format = format;
        return this;
    }

    private void createGLResources() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pMemObj = stack.callocInt(1);
            glCreateMemoryObjectsEXT(pMemObj);
            glMemoryObject = pMemObj.get(0);
            glNamedBufferStorageMemEXT(
                    glMemoryObject,
                    (long) width * height * 4,
                    0,
                    GL_HANDLE_TYPE_OPAQUE_FD_EXT
            );
            IntBuffer pFd = stack.callocInt(1);
            //glGetMemoryFdEXT(glMemoryObject, GL_HANDLE_TYPE_OPAQUE_FD_EXT, pFd);
            memory.fd = pFd.get(0);
            IntBuffer pTexture = stack.callocInt(1);
            glGenTextures(pTexture);
            glTextureId = pTexture.get(0);
            glBindTexture(GL_TEXTURE_2D, glTextureId);
            glTexStorageMem2DEXT(GL_TEXTURE_2D, 1, format.gl(), width, height, glMemoryObject, 0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    // Vulkan端：导入OpenGL资源
    private void createVulkanResources() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 创建Vulkan图像
            VkExternalMemoryImageCreateInfo extMemInfo = VkExternalMemoryImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT);

            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .pNext(extMemInfo)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(format.vk())
                    .extent(VkExtent3D.calloc(stack).set(width, height, 1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

            LongBuffer pImage = stack.callocLong(1);
            VK_CHECK(vkCreateImage(deviceManager.device, imageInfo, null, pImage),
                    "创建Vulkan图像失败");
            vkImage.image = pImage.get(0);

            // 导入内存
            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(deviceManager.device, vkImage.image, memReqs);

            VkImportMemoryFdInfoKHR importInfo = VkImportMemoryFdInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMPORT_MEMORY_FD_INFO_KHR)
                    .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT)
                    .fd(memory.fd);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .pNext(importInfo)
                    .allocationSize(memReqs.size());

            LongBuffer pMemory = stack.callocLong(1);
            VK_CHECK(vkAllocateMemory(deviceManager.device, allocInfo, null, pMemory),
                    "导入内存失败");
            vkImage.memory = pMemory.get(0);
            vkBindImageMemory(deviceManager.device, vkImage.image, vkImage.memory, 0);
        }
    }

    // 创建完整共享链路
    public void create() {
        createGLResources();
        createVulkanResources();
    }

    // 资源清理
    public void cleanup() {
        // 清理OpenGL资源
        if (glTextureId != 0) {
            glDeleteTextures(glTextureId);
            glTextureId = 0;
        }
        if (glMemoryObject != 0) {
            glDeleteMemoryObjectsEXT(glMemoryObject);
            glMemoryObject = 0;
        }

        // 清理Vulkan资源
        if (vkImage.image != VK_NULL_HANDLE) {
            vkDestroyImage(deviceManager.device, vkImage.image, null);
            vkImage.image = VK_NULL_HANDLE;
        }
        if (vkImage.memory != VK_NULL_HANDLE) {
            vkFreeMemory(deviceManager.device, vkImage.memory, null);
            vkImage.memory = VK_NULL_HANDLE;
        }
    }

    // 获取Vulkan图像视图 (可选)
    public long createImageView() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(vkImage.image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format.vk())
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            LongBuffer pView = stack.callocLong(1);
            VK_CHECK(vkCreateImageView(deviceManager.device, viewInfo, null, pView),
                    "创建图像视图失败");
            return pView.get(0);
        }
    }
}
