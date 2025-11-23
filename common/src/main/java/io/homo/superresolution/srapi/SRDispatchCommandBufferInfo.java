/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.srapi;

import org.lwjgl.vulkan.VkCommandBuffer;

public class SRDispatchCommandBufferInfo {
    public SRRenderApiType renderApiType;

    public static class OpenGLCommandBuffer {
    }

    public static class VulkanCommandBuffer {
        public long commandBuffer;

        public VulkanCommandBuffer() {
            this.commandBuffer = 0;
        }

        public VulkanCommandBuffer(long commandBuffer) {
            this.commandBuffer = commandBuffer;
        }

        public VulkanCommandBuffer(VkCommandBuffer commandBuffer) {
            this.commandBuffer = commandBuffer != null ? commandBuffer.address() : 0;
        }

        public long getCommandBuffer() {
            return commandBuffer;
        }

        public void setCommandBuffer(long commandBuffer) {
            this.commandBuffer = commandBuffer;
        }
    }

    private OpenGLCommandBuffer openglCommandBuffer;
    private VulkanCommandBuffer vulkanCommandBuffer;

    public static SRDispatchCommandBufferInfo createOpenGL() {
        SRDispatchCommandBufferInfo info = new SRDispatchCommandBufferInfo();
        info.renderApiType = SRRenderApiType.OPENGL;
        info.openglCommandBuffer = new OpenGLCommandBuffer();
        return info;
    }

    public static SRDispatchCommandBufferInfo createVulkan(long commandBuffer) {
        SRDispatchCommandBufferInfo info = new SRDispatchCommandBufferInfo();
        info.renderApiType = SRRenderApiType.VULKAN;
        info.vulkanCommandBuffer = new VulkanCommandBuffer(commandBuffer);
        return info;
    }

    public static SRDispatchCommandBufferInfo createVulkan(VkCommandBuffer commandBuffer) {
        SRDispatchCommandBufferInfo info = new SRDispatchCommandBufferInfo();
        info.renderApiType = SRRenderApiType.VULKAN;
        info.vulkanCommandBuffer = new VulkanCommandBuffer(commandBuffer);
        return info;
    }

    private SRDispatchCommandBufferInfo() {
    }

    public SRRenderApiType getRenderApiType() {
        return renderApiType;
    }

    public OpenGLCommandBuffer getOpenglCommandBuffer() {
        return openglCommandBuffer;
    }

    public VulkanCommandBuffer getVulkanCommandBuffer() {
        return vulkanCommandBuffer;
    }

    public long getVulkanCommandBufferAddress() {
        if (renderApiType == SRRenderApiType.VULKAN && vulkanCommandBuffer != null) {
            return vulkanCommandBuffer.commandBuffer;
        }
        return 0;
    }
}
