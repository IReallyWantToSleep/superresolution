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

package io.homo.superresolution.core.graphics.opengl.pipeline;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.ColorAttachment;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PassClearState;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.opengl.OpenGLException;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandBuffer;

public class GlRenderPass extends RenderPass {

    public GlRenderPass(GraphicsPipeline pipeline, IFrameBuffer frameBuffer, PassClearState clearState) {
        super(pipeline, frameBuffer, clearState);
    }

    public void bind() {
        if (frameBuffer instanceof IBindableFrameBuffer bindableFrameBuffer) {
            bindableFrameBuffer.bind(FrameBufferBindPoint.All, false);
        } else {
            throw new OpenGLException("FrameBuffer is not bindable");
        }
    }

    public void end(GlCommandBuffer commandBuffer) {
        for (ColorAttachment colorAttachment : colorAttachments()) {
            if (clearState.shouldClearColorOnEnd(colorAttachment.index())) {
                float[] color = clearState.getColorClearValueOnEnd(colorAttachment.index());
                if (color != null) {
                    commandBuffer.getDecoder().clearTextureRGBA(
                            commandBuffer,
                            colorAttachment.texture(),
                            color
                    );
                }
            }
        }
        if (clearState.shouldClearDepthOnEnd()) {
            float depth = clearState.getDepthClearValueOnEnd();
            commandBuffer.getDecoder().clearTextureDepth(
                    commandBuffer,
                    depthStencilAttachment().texture(),
                    depth
            );
        }
        if (clearState.shouldClearStencilOnEnd()) {
            int stencil = clearState.getStencilClearValueOnEnd();
            commandBuffer.getDecoder().clearTextureStencil(
                    commandBuffer,
                    depthStencilAttachment().texture(),
                    stencil
            );
        }
    }

    public void begin(GlCommandBuffer commandBuffer) {
        for (ColorAttachment colorAttachment : colorAttachments()) {
            if (clearState.shouldClearColorOnBegin(colorAttachment.index())) {
                float[] color = clearState.getColorClearValueOnBegin(colorAttachment.index());
                if (color != null) {
                    commandBuffer.getDecoder().clearTextureRGBA(
                            commandBuffer,
                            colorAttachment.texture(),
                            color
                    );
                }
            }
        }
        if (clearState.shouldClearDepthOnBegin()) {
            float depth = clearState.getDepthClearValueOnBegin();
            commandBuffer.getDecoder().clearTextureDepth(
                    commandBuffer,
                    depthStencilAttachment().texture(),
                    depth
            );
        }
        if (clearState.shouldClearStencilOnBegin()) {
            int stencil = clearState.getStencilClearValueOnBegin();
            commandBuffer.getDecoder().clearTextureStencil(
                    commandBuffer,
                    depthStencilAttachment().texture(),
                    stencil
            );
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