package io.homo.superresolution.common.render.interop;

import io.homo.superresolution.common.render.vulkan.TextureFormat;
import io.homo.superresolution.common.render.vulkan.VkAllocatedImage;
import io.homo.superresolution.common.render.vulkan.VkDeviceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;
import static io.homo.superresolution.common.render.vulkan.Utils.VK_CHECK;
import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.vkGetMemoryWin32HandleKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;

public class SharedTexture {
    private final VkDeviceManager deviceManager;
    public VkAllocatedImage vkImage = new VkAllocatedImage();
    public long vkImageView;
    public int glId = GL_NULL_HANDLE;
    public TextureFormat format;
    public int width;
    public int height;
    public SharedMemory memory = new SharedMemory();

    public SharedTexture(int width, int height, TextureFormat format, VkDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
        this.width = width;
        this.height = height;
        this.format = format;

    }

    private void VK_CreateImage() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExternalMemoryImageCreateInfo imageCreatePNext = VkExternalMemoryImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);

            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .pNext(imageCreatePNext)
                    .flags(0)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(TextureFormat.toVK(this.format))
                    .extent(VkExtent3D.calloc(stack).set(width, height, 1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_STORAGE_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

            LongBuffer ptr = stack.callocLong(1);
            VK_CHECK(vkCreateImage(deviceManager.device, imageInfo, null, ptr), "Failed to create Vulkan image");
            vkImage.image = ptr.get(0);
        }
    }

    private void VK_TransferImage(VkCommandBuffer cmdBuff, long image) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer transferBarrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .newLayout(VK_IMAGE_LAYOUT_GENERAL)
                    .srcAccessMask(0)
                    .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1)
                    );

            vkCmdPipelineBarrier(cmdBuff,
                    VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                    null, null, transferBarrier);
            VkImageMemoryBarrier.Buffer useBarrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK_IMAGE_LAYOUT_GENERAL)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1)
                    );

            vkCmdPipelineBarrier(cmdBuff,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                    null, null, useBarrier);
        }
    }

    private void VK_CreateImageMemory() {
        if (vkImage.memory == VK_NULL_HANDLE) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
                vkGetImageMemoryRequirements(deviceManager.device, vkImage.image, memReqs);
                VkExportMemoryAllocateInfo exportAllocInfo = VkExportMemoryAllocateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO)
                        .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
                VkMemoryDedicatedAllocateInfo dedicatedAllocInfo = VkMemoryDedicatedAllocateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO)
                        .image(vkImage.image);
                exportAllocInfo.pNext(dedicatedAllocInfo.address());
                VkMemoryAllocateInfo memAllocInfo = VkMemoryAllocateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                        .pNext(exportAllocInfo.address())
                        .allocationSize(memReqs.size())
                        .memoryTypeIndex(getMemoryTypeIndex(memReqs.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));
                LongBuffer ptr = stack.callocLong(1);
                VK_CHECK(vkAllocateMemory(deviceManager.device, memAllocInfo, null, ptr), "Failed to allocate memory");
                vkImage.memory = ptr.get(0);
                vkImage.allocationSize = memReqs.size();
                VK_CHECK(vkBindImageMemory(deviceManager.device, vkImage.image, vkImage.memory, 0), "Failed to bind memory");
            }
        }
    }

    private void VkGL_CreateSharedMemory(long memoryPtr, long size) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryGetWin32HandleInfoKHR memoryGetInfo = VkMemoryGetWin32HandleInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR)
                    .memory(memoryPtr)
                    .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);

            PointerBuffer handlePtr = stack.callocPointer(1);
            vkGetMemoryWin32HandleKHR(deviceManager.device, memoryGetInfo, handlePtr);
            memory.vkHandle = handlePtr.get(0);

            IntBuffer glMemory = stack.callocInt(1);
            glCreateMemoryObjectsEXT(glMemory);
            memory.glRef = glMemory.get(0);
            glImportMemoryWin32HandleEXT(
                    memory.glRef,
                    size,
                    GL_HANDLE_TYPE_OPAQUE_WIN32_EXT,
                    memory.vkHandle
            );
        }
    }

    private int getMemoryTypeIndex(int typeBits, int properties) {
        int memoryTypeCount = deviceManager.deviceMemoryProperties.memoryTypeCount();
        for (int i = 0; i < memoryTypeCount; i++) {
            if ((typeBits & 1) != 0 && (deviceManager.deviceMemoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
            typeBits >>= 1;
        }
        return VK_MAX_MEMORY_TYPES;
    }

    private void VK_CreateSRV(long inputImage, int format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo info = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(inputImage)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format)
                    .subresourceRange(VkImageSubresourceRange.calloc(stack).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).layerCount(1).levelCount(1));

            LongBuffer ptr = stack.callocLong(1);
            VK_CHECK(vkCreateImageView(deviceManager.device, info, null, ptr), "Failed to create Vulkan image view");
            vkImageView = ptr.get(0);
        }
    }

    /*
        private void VkGL_CreateSharedMemory(long memoryPtr, long size) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkMemoryGetWin32HandleInfoKHR memoryGetInfo = VkMemoryGetWin32HandleInfoKHR.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR)
                        .memory(memoryPtr)
                        .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
                PointerBuffer handlePtr = stack.callocPointer(1);
                vkGetMemoryWin32HandleKHR(deviceManager.device, memoryGetInfo, handlePtr);
                memory.vkHandle = handlePtr.get(0);
                IntBuffer glMemory = stack.callocInt(1);
                glCreateMemoryObjectsEXT(glMemory);
                memory.glRef = glMemory.get(0);
                glImportMemoryWin32HandleEXT((int) memory.glRef, size, GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, memory.vkHandle);
            }
        }
    */
    private int GL_CreateGLTexture2D() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer texture = stack.callocInt(1);
            glGenTextures(texture);


            return texture.get(0);
        }
    }

    private void GL_CreateTextureStorage() {
        if (memory.glRef != 0) {
            glTextureStorageMem2DEXT(
                    glId,
                    1,
                    TextureFormat.toGL(format),
                    width,
                    height,
                    memory.glRef, // 直接使用 GLuint，无需强制转换
                    0
            );
            glBindTexture(GL_TEXTURE_2D, glId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            throw new IllegalStateException("OpenGL memory object not initialized");
        }
    }

    public void updateTexture() {
        if (glId != GL_NULL_HANDLE) glDeleteTextures(glId);
        glId = GL_NULL_HANDLE;
        glId = GL_CreateGLTexture2D();
        GL_CreateTextureStorage();
    }

    public void create() {
        VK_CreateImage();
        VK_CreateImageMemory();
        VkCommandBuffer cmdBuf = deviceManager.application.beginOneTimeSubmitCmd();
        VK_TransferImage(cmdBuf, vkImage.image);
        deviceManager.application.endOneTimeSubmitCmd();
        VK_CreateSRV(vkImage.image, TextureFormat.toVK(format));
        VkGL_CreateSharedMemory(vkImage.memory, vkImage.allocationSize);
        updateTexture();
    }


    public void clean() {
        if (glId != GL_NULL_HANDLE) {
            glDeleteTextures(glId);
            glId = GL_NULL_HANDLE;
        }
        if (vkImageView != VK_NULL_HANDLE) {
            vkDestroyImageView(deviceManager.device, vkImageView, null);
            vkImageView = VK_NULL_HANDLE;
        }
        vkImage.destroy(deviceManager.device);
    }

    public void resize(int width, int height) {
        // 显式解除纹理与内存的绑定
        if (glId != GL_NULL_HANDLE) {
            glDeleteTextures(glId);
            glId = GL_NULL_HANDLE;
        }
        if (memory.glRef != 0) {
            glDeleteMemoryObjectsEXT(new int[]{memory.glRef});
            memory.glRef = 0;
        }
        // 清理 Vulkan 资源
        clean();
        // 更新尺寸并重新创建
        this.width = width;
        this.height = height;
        create();
    }

    public void startWrite() {
    }

    public void endWrite() {
        updateTexture();
    }

    public void startRead() {
        updateTexture();
    }

    public void endRead() {
    }
}