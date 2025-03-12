package io.homo.superresolution.common.render.interop;

import io.homo.superresolution.common.render.vulkan.*;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.render.vulkan.texture.TextureUsage;
import io.homo.superresolution.common.render.vulkan.texture.VkAllocatedImage;
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
    private final int currentLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    public VkAllocatedImage vkImage = new VkAllocatedImage();
    public long vkImageView;
    public int glId = GL_NULL_HANDLE;
    public int width;
    public int height;
    public SharedMemory memory = new SharedMemory();
    protected TextureFormat format;
    protected TextureUsage usage;

    public SharedTexture(int width, int height, VkDeviceManager deviceManager) {
        this.deviceManager = deviceManager;
        this.width = width;
        this.height = height;
    }

    public SharedTexture setFormat(TextureFormat format) {
        this.format = format;
        return this;
    }

    public SharedTexture setUsage(TextureUsage usage) {
        this.usage = usage;
        return this;
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
                    .format(this.format.vk())
                    .extent(VkExtent3D.calloc(stack).set(width, height, 1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(
                            VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT |
                                    VK_IMAGE_USAGE_TRANSFER_DST_BIT |
                                    VK_IMAGE_USAGE_TRANSFER_SRC_BIT |
                                    usage.getValue()
                    )
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);

            LongBuffer ptr = stack.callocLong(1);
            VK_CHECK(vkCreateImage(deviceManager.device, imageInfo, null, ptr), "Failed to create Vulkan image");
            vkImage.image = ptr.get(0);
        }
    }

    private void VK_TransferImage(VkCommandBuffer cmdBuff, long image) {
        Utils.transitionImageLayout(
                cmdBuff,
                image,
                format.vk(),
                VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                false
        );
        int finalLayout = usage == TextureUsage.sampledImage ? VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL : VK_IMAGE_LAYOUT_GENERAL;
        Utils.transitionImageLayout(
                cmdBuff,
                image,
                format.vk(),
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                finalLayout,
                false
        );
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

    private int GL_CreateGLTexture2D() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer texture = stack.callocInt(1);
            glGenTextures(texture);
            return texture.get(0);
        }
    }

    private void GL_CreateTextureStorage() {
        if (memory.glRef != 0) {
            glBindTexture(GL_TEXTURE_2D, glId);
            glTextureStorageMem2DEXT(
                    glId,
                    1,
                    format.gl(),
                    width,
                    height,
                    memory.glRef,
                    0
            );
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glBindTexture(GL_TEXTURE_2D, 0);
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
        VK_CreateSRV(vkImage.image, format.vk());
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
        clean();
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