package io.homo.superresolution.core.graphics.vulkan.semaphore;

import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExportSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreGetWin32HandleInfoKHR;
import org.lwjgl.vulkan.VkSubmitInfo;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.EXTSemaphoreWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTSemaphoreWin32.glImportSemaphoreWin32HandleEXT;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.*;
import static org.lwjgl.vulkan.VK11.*;

public class VkGlInteropSemaphore {
    private final long vkSemaphoreHandle;
    private final long glSemaphoreHandle;
    private final long semaphoreHandle;
    private final VulkanDevice device;

    private VkGlInteropSemaphore(long vkSemaphoreHandle, long glSemaphoreHandle, long semaphoreHandle, VulkanDevice device) {
        this.vkSemaphoreHandle = vkSemaphoreHandle;
        this.glSemaphoreHandle = glSemaphoreHandle;
        this.semaphoreHandle = semaphoreHandle;
        this.device = device;
    }

    public static VkGlInteropSemaphore create(VulkanDevice vulkanDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExportSemaphoreCreateInfo exportSemaphoreCreateInfo = VkExportSemaphoreCreateInfo.calloc(stack);
            exportSemaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO);
            exportSemaphoreCreateInfo.handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            semaphoreCreateInfo.pNext(exportSemaphoreCreateInfo);
            long[] pVkSemaphore = new long[]{0};
            VK_CHECK(vkCreateSemaphore(
                    vulkanDevice.getVkDevice(),
                    semaphoreCreateInfo,
                    null,
                    pVkSemaphore
            ));
            VkSemaphoreGetWin32HandleInfoKHR semaphoreGetInfo = VkSemaphoreGetWin32HandleInfoKHR.calloc(stack);
            semaphoreGetInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR);
            semaphoreGetInfo.handleType(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            semaphoreGetInfo.semaphore(pVkSemaphore[0]);

            PointerBuffer pExpSemaphore = stack.mallocPointer(1);
            VK_CHECK(vkGetSemaphoreWin32HandleKHR(
                    vulkanDevice.getVkDevice(),
                    semaphoreGetInfo,
                    pExpSemaphore
            ));

            int pGlSemaphores = glGenSemaphoresEXT();
            glImportSemaphoreWin32HandleEXT(pGlSemaphores, GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, pExpSemaphore.get());
            return new VkGlInteropSemaphore(pVkSemaphore[0], pGlSemaphores, pExpSemaphore.get(), vulkanDevice);
        }
    }

    public void destroy() {
        vkDestroySemaphore(
                device.getVkDevice(),
                vkSemaphoreHandle,
                null
        );
        glDeleteSemaphoresEXT((int) glSemaphoreHandle);
    }

    public void signalVulkan() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pWaitSemaphores(null);
            submitInfo.pSignalSemaphores(stack.longs(vkSemaphoreHandle));
            submitInfo.pCommandBuffers(null);
            VK_CHECK(vkQueueSubmit(device.getGraphicsQueue(), submitInfo, VK_NULL_HANDLE));
            VK_CHECK(vkQueueWaitIdle(device.getGraphicsQueue()));
        }
    }


    public void waitVulkan() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pWaitSemaphores(stack.longs(vkSemaphoreHandle));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT));
            submitInfo.pSignalSemaphores(null);
            submitInfo.pCommandBuffers(null);

            VK_CHECK(vkQueueSubmit(device.getGraphicsQueue(), submitInfo, VK_NULL_HANDLE));
            VK_CHECK(vkQueueWaitIdle(device.getGraphicsQueue()));
        }
    }


    public void signalOpenGL() {
        glSignalSemaphoreEXT((int) glSemaphoreHandle, new int[]{}, new int[]{}, new int[]{});
    }


    public void waitOpenGL() {
        glWaitSemaphoreEXT((int) glSemaphoreHandle, new int[]{}, new int[]{}, new int[]{});
    }
}
