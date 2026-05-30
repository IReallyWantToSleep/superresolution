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

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.PassClearState;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;

public class VulkanRenderPass extends RenderPass {

    public VulkanRenderPass(VulkanDevice device,
                            IFrameBuffer frameBuffer, PassClearState clearState) {
        super(frameBuffer, clearState);

        if (!(frameBuffer instanceof VulkanFramebuffer)) {
            throw new IllegalArgumentException("FrameBuffer must be a VulkanFramebuffer");
        }
    }

    @Override
    public void execute(ICommandBuffer cmd, IVertexBuffer vertexBuffer) {
    }

    @Override
    public void execute(ICommandBuffer cmd) {
    }

    @Override
    public void destroy() {
    }
}
