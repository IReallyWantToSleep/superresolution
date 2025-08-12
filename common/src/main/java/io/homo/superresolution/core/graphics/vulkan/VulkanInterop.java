package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.EXTMemoryObjectFD;
import org.lwjgl.opengl.EXTMemoryObjectWin32;
import org.lwjgl.opengl.EXTSemaphoreFD;
import org.lwjgl.opengl.EXTSemaphoreWin32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.*;

import org.lwjgl.vulkan.KHRExternalSemaphoreWin32;
import org.lwjgl.vulkan.KHRExternalSemaphoreFd;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.vulkan.VK11.*;

public class VulkanInterop {
    public static final VulkanInteropExt IMPL;

    static {
        if (Platform.currentPlatform.getOS().type == OSType.WINDOWS) {
            IMPL = new WindowsVulkanInteropExtImpl();
        } else {
            IMPL = new LinuxVulkanInteropExtImpl();
        }
    }

    public interface VulkanInteropExt {
        void glImportSemaphoreHandleEXT(
                MemoryStack stack,
                int semaphore,
                long handle
        );

        long vkGetSemaphoreHandleKHR(
                MemoryStack stack,
                VkDevice device,
                Struct pGetHandleInfo
        );


        Struct createVkSemaphoreGetHandleInfoKHR(MemoryStack stack, long pVkSemaphore);

        Struct createVkExportSemaphoreCreateInfo(MemoryStack stack);

        Struct createVkExportMemoryAllocateInfo(MemoryStack stack);

        Struct createVkImportMemoryInfo(MemoryStack stack, long handle);

        long vkGetMemoryHandle(MemoryStack stack, VkDevice device, long memory);

        void glImportMemoryEXT(int memoryObject, long size, long handle);
    }

    private static class LinuxVulkanInteropExtImpl implements VulkanInteropExt {

        @Override
        public void glImportMemoryEXT(int memoryObject, long size, long handle) {
            EXTMemoryObjectFD.glImportMemoryFdEXT(
                    memoryObject,
                    size,
                    EXTMemoryObjectFD.GL_HANDLE_TYPE_OPAQUE_FD_EXT,
                    (int) handle
            );
        }

        @Override
        public void glImportSemaphoreHandleEXT(MemoryStack stack, int semaphore, long handle) {
            EXTSemaphoreFD.glImportSemaphoreFdEXT(semaphore, EXTSemaphoreFD.GL_HANDLE_TYPE_OPAQUE_FD_EXT, (int) handle);
        }

        @Override
        public long vkGetSemaphoreHandleKHR(MemoryStack stack, VkDevice device, Struct pGetHandleInfo) {
            int[] fd = new int[]{0};
            KHRExternalSemaphoreFd.vkGetSemaphoreFdKHR(device, (VkSemaphoreGetFdInfoKHR) pGetHandleInfo, fd);
            return fd[0];
        }

        @Override
        public Struct createVkSemaphoreGetHandleInfoKHR(MemoryStack stack, long pVkSemaphore) {
            VkSemaphoreGetFdInfoKHR semaphoreGetInfo = VkSemaphoreGetFdInfoKHR.calloc(stack);
            semaphoreGetInfo.sType(KHRExternalSemaphoreFd.VK_STRUCTURE_TYPE_SEMAPHORE_GET_FD_INFO_KHR);
            semaphoreGetInfo.handleType(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_FD_BIT);
            semaphoreGetInfo.semaphore(pVkSemaphore);
            return semaphoreGetInfo;
        }

        @Override
        public Struct createVkExportSemaphoreCreateInfo(MemoryStack stack) {
            VkExportSemaphoreCreateInfo exportSemaphoreCreateInfo = VkExportSemaphoreCreateInfo.calloc(stack);
            exportSemaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO);
            exportSemaphoreCreateInfo.handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_FD_BIT);
            return exportSemaphoreCreateInfo;
        }

        @Override
        public Struct createVkExportMemoryAllocateInfo(MemoryStack stack) {
            return VkExportMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT);
        }

        @Override
        public Struct createVkImportMemoryInfo(MemoryStack stack, long handle) {
            return VkImportMemoryFdInfoKHR.calloc(stack)
                    .sType(KHRExternalMemoryFd.VK_STRUCTURE_TYPE_IMPORT_MEMORY_FD_INFO_KHR)
                    .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT)
                    .fd((int) handle);
        }

        @Override
        public long vkGetMemoryHandle(MemoryStack stack, VkDevice device, long memory) {
            VkMemoryGetFdInfoKHR getInfo = VkMemoryGetFdInfoKHR.calloc(stack)
                    .sType(KHRExternalMemoryFd.VK_STRUCTURE_TYPE_MEMORY_GET_FD_INFO_KHR)
                    .memory(memory)
                    .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT);

            int[] fd = new int[1];
            VK_CHECK(KHRExternalMemoryFd.vkGetMemoryFdKHR(device, getInfo, fd),
                    "Failed to export Linux memory handle");
            return fd[0];
        }
    }

    private static class WindowsVulkanInteropExtImpl implements VulkanInteropExt {
        @Override
        public void glImportMemoryEXT(int memoryObject, long size, long handle) {
            EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT(
                    memoryObject,
                    size,
                    EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT,
                    handle
            );
        }

        @Override
        public void glImportSemaphoreHandleEXT(MemoryStack stack, int semaphore, long handle) {
            EXTSemaphoreWin32.glImportSemaphoreWin32HandleEXT(semaphore, EXTSemaphoreWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, (int) handle);
        }

        @Override
        public long vkGetSemaphoreHandleKHR(MemoryStack stack, VkDevice device, Struct pGetHandleInfo) {
            PointerBuffer fd = stack.callocPointer(1);
            KHRExternalSemaphoreWin32.vkGetSemaphoreWin32HandleKHR(
                    device,
                    (VkSemaphoreGetWin32HandleInfoKHR) pGetHandleInfo,
                    fd
            );
            return fd.get(0);
        }

        @Override
        public Struct createVkSemaphoreGetHandleInfoKHR(MemoryStack stack, long pVkSemaphore) {
            VkSemaphoreGetWin32HandleInfoKHR semaphoreGetInfo = VkSemaphoreGetWin32HandleInfoKHR.calloc(stack);
            semaphoreGetInfo.sType(KHRExternalSemaphoreWin32.VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR);
            semaphoreGetInfo.handleType(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            semaphoreGetInfo.semaphore(pVkSemaphore);
            return semaphoreGetInfo;
        }

        @Override
        public Struct createVkExportSemaphoreCreateInfo(MemoryStack stack) {
            VkExportSemaphoreCreateInfo exportSemaphoreCreateInfo = VkExportSemaphoreCreateInfo.calloc(stack);
            exportSemaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO);
            exportSemaphoreCreateInfo.handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT);
            return exportSemaphoreCreateInfo;
        }

        @Override
        public Struct createVkExportMemoryAllocateInfo(MemoryStack stack) {
            return VkExportMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);
        }

        @Override
        public Struct createVkImportMemoryInfo(MemoryStack stack, long handle) {
            return VkImportMemoryWin32HandleInfoKHR.calloc(stack)
                    .sType(KHRExternalMemoryWin32.VK_STRUCTURE_TYPE_IMPORT_MEMORY_WIN32_HANDLE_INFO_KHR)
                    .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT)
                    .handle(handle);
        }

        @Override
        public long vkGetMemoryHandle(MemoryStack stack, VkDevice device, long memory) {
            VkMemoryGetWin32HandleInfoKHR getInfo = VkMemoryGetWin32HandleInfoKHR.calloc(stack)
                    .sType(KHRExternalMemoryWin32.VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR)
                    .memory(memory)
                    .handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT);

            PointerBuffer pHandle = stack.mallocPointer(1);
            VK_CHECK(KHRExternalMemoryWin32.vkGetMemoryWin32HandleKHR(device, getInfo, pHandle),
                    "Failed to export Windows memory handle");
            return pHandle.get(0);
        }
    }
}
