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

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
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
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreCreateInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            semaphoreCreateInfo.pNext(VulkanInterop.IMPL.createVkExportSemaphoreCreateInfo(stack).address());
            long[] pVkSemaphore = new long[]{0};
            VK_CHECK(vkCreateSemaphore(
                    vulkanDevice.getVkDevice(),
                    semaphoreCreateInfo,
                    null,
                    pVkSemaphore
            ));
            long pExpSemaphore = VulkanInterop.IMPL.vkGetSemaphoreHandleKHR(
                    stack,
                    vulkanDevice.getVkDevice(),
                    VulkanInterop.IMPL.createVkSemaphoreGetHandleInfoKHR(stack, pVkSemaphore[0])
            );

            int pGlSemaphores = glGenSemaphoresEXT();
            VulkanInterop.IMPL.glImportSemaphoreHandleEXT(stack, pGlSemaphores, pExpSemaphore);
            return new VkGlInteropSemaphore(pVkSemaphore[0], pGlSemaphores, pExpSemaphore, vulkanDevice);
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
        vkDestroySemaphore(
                device.getVkDevice(),
                vkSemaphoreHandle,
                null
        );
        glDeleteSemaphoresEXT((int) glSemaphoreHandle);
    }

    public void signalOpenGL(int[] textures, int[] buffers, int[] dstLayouts) {
        glSignalSemaphoreEXT(
                (int) glSemaphoreHandle,
                buffers == null ? new int[]{} : buffers,
                textures == null ? new int[]{} : textures,
                dstLayouts == null ? new int[]{} : dstLayouts
        );
        GL20.glFlush();
    }

    public void waitOpenGL(int[] textures, int[] buffers, int[] srcLayouts) {
        glWaitSemaphoreEXT(
                (int) glSemaphoreHandle,
                buffers == null ? new int[]{} : buffers,
                textures == null ? new int[]{} : textures,
                srcLayouts == null ? new int[]{} : srcLayouts
        );
    }

    public void signalOpenGL() {
        signalOpenGL(null, null, null);
    }

    public void waitOpenGL() {
        waitOpenGL(null, null, null);
    }
}
