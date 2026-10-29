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

import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.opengl.EXTSemaphore.*;
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
            long[] pVkSemaphore = new long[]{0};
            int pGlSemaphore = 0;
            long exportedSemaphoreHandle = -1;
            boolean imported = false;
            try {
                VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack);
                semaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
                semaphoreCreateInfo.pNext(VulkanInterop.IMPL.createVkExportSemaphoreCreateInfo(stack).address());
                VK_CHECK(vkCreateSemaphore(
                        vulkanDevice.getVkDevice(),
                        semaphoreCreateInfo,
                        null,
                        pVkSemaphore
                ));
                vulkanDevice.setDebugName(VK_OBJECT_TYPE_SEMAPHORE, pVkSemaphore[0], "VkGlInteropSemaphore");
                exportedSemaphoreHandle = VulkanInterop.IMPL.vkGetSemaphoreHandleKHR(
                        stack,
                        vulkanDevice.getVkDevice(),
                        VulkanInterop.IMPL.createVkSemaphoreGetHandleInfoKHR(stack, pVkSemaphore[0])
                );

                pGlSemaphore = glGenSemaphoresEXT();
                VulkanInterop.IMPL.glImportSemaphoreHandleEXT(stack, pGlSemaphore, exportedSemaphoreHandle);
                imported = true;
                return new VkGlInteropSemaphore(
                        pVkSemaphore[0],
                        pGlSemaphore,
                        exportedSemaphoreHandle,
                        vulkanDevice
                );
            } catch (Throwable throwable) {
                try {
                    if (pGlSemaphore != 0) {
                        glDeleteSemaphoresEXT(pGlSemaphore);
                    }
                } finally {
                    try {
                        if (pVkSemaphore[0] != 0L) {
                            vkDestroySemaphore(vulkanDevice.getVkDevice(), pVkSemaphore[0], null);
                        }
                    } finally {
                        if (imported) {
                            VulkanInterop.closeImportedExportedHandle(exportedSemaphoreHandle);
                        } else {
                            VulkanInterop.closeUnimportedExportedHandle(exportedSemaphoreHandle);
                        }
                    }
                }
                throw throwable;
            }
        }
    }

    public long getVkSemaphoreHandle() {
        return vkSemaphoreHandle;
    }

    public long getGlSemaphoreHandle() {
        return glSemaphoreHandle;
    }

    public long getSemaphoreHandle() {
        return semaphoreHandle;
    }

    public void destroy() {
        try {
            vkDestroySemaphore(
                    device.getVkDevice(),
                    vkSemaphoreHandle,
                    null
            );
        } finally {
            try {
                glDeleteSemaphoresEXT((int) glSemaphoreHandle);
            } finally {
                VulkanInterop.closeImportedExportedHandle(semaphoreHandle);
            }
        }
    }

    public void signalVulkan(int[] textures, int[] buffers, int[] dstLayouts) {
        glSignalSemaphoreEXT(
                (int) glSemaphoreHandle,
                buffers == null ? new int[]{} : buffers,
                textures == null ? new int[]{} : textures,
                dstLayouts == null ? new int[]{} : dstLayouts
        );
        GL20.glFlush();
    }

    public void waitVulkanSignal(int[] textures, int[] buffers, int[] srcLayouts) {
        glWaitSemaphoreEXT(
                (int) glSemaphoreHandle,
                buffers == null ? new int[]{} : buffers,
                textures == null ? new int[]{} : textures,
                srcLayouts == null ? new int[]{} : srcLayouts
        );
    }

    public void signalVulkan() {
        signalVulkan(null, null, null);
    }

    public void waitVulkanSignal() {
        waitVulkanSignal(null, null, null);
    }
}
