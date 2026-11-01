/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.backend.vulkan.VulkanCommandEncoder;
import io.homo.superresolution.common.mixin.core.accessor.FrontendCommandEncoderAccessor;
import io.homo.superresolution.common.mixin.core.accessor.VulkanCommandEncoderAccessor;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferState;
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.EnumSet;

public class B3DVulkanCommandBuffer extends VulkanCommandBuffer {
    public B3DVulkanCommandBuffer(
            CommandEncoder b3dEncoder
    ) {
        super(
                RenderSystems.vulkan().device(),
                new FakeVulkanCommandPool(((VulkanCommandEncoderAccessor) ((FrontendCommandEncoderAccessor)b3dEncoder).backend()).currentCommandBuffer()),
                CommandBufferBehavior.OneTimeSubmit
        );
    }

    private static class FakeVulkanCommandPool extends VulkanCommandPool {
        VkCommandBuffer commandBuffer;

        public FakeVulkanCommandPool(VkCommandBuffer commandBuffer) {
            super(RenderSystems.vulkan().device(), RenderSystems.vulkan().device().getMainQueue(), EnumSet.noneOf(CommandPoolFlags.class), "");
            this.commandBuffer = commandBuffer;
        }

        @Override
        VkCommandBuffer createNativeCommandBuffer() {
            return commandBuffer;
        }

        @Override
        public void destroy() {

        }
    }
    @Override
    public synchronized CommandBufferState state() {
        return CommandBufferState.Recording;
    }
    @Override
    public synchronized void begin() {
    }

    @Override
    public synchronized void end() {
    }

    @Override
    public synchronized void destroy() {

    }
}
