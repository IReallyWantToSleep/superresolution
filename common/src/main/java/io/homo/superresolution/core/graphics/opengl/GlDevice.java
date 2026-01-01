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

package io.homo.superresolution.core.graphics.opengl;

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandBuffer;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandDecoder;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlPipelineDescriptorSet;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlRenderPass;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture1D;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexBuffer;

public class GlDevice implements IDevice {
    private GlCommandDecoder commandDecoder;

    public GlDevice() {
        this.commandDecoder = new GlCommandDecoder(this);
    }

    @Override
    public ITexture createTexture(TextureDescription description) {
        if (description.getType() == TextureType.Texture2D) {
            return GlTexture2D.create(description);
        }
        if (description.getType() == TextureType.Texture1D) {
            return GlTexture1D.create(description);
        }
        return null;
    }

    @Override
    public GlShaderProgram createShaderProgram(ShaderDescription description) {
        return new GlShaderProgram(description);
    }

    @Override
    public GlVertexBuffer createVertexBuffer(VertexBufferDescription description) {
        return GlVertexBuffer.create(description);
    }

    @Override
    public GlBuffer createBuffer(BufferDescription description) {
        return new GlBuffer(description);
    }

    @Override
    public RenderPass createRenderPass(RenderPass.Builder builder) {
        return new GlRenderPass(
                (GraphicsPipeline) builder.getPipeline(),
                builder.getFrameBuffer(),
                builder.getClearState()
        );
    }

    @Override
    public PipelineDescriptorSet createDescriptorSet(IShaderProgram shader) {
        return new GlPipelineDescriptorSet(shader);
    }

    @Override
    public ComputePipeline createComputePipeline(ComputePipeline.Builder builder) {
        return new GlComputePipeline(
                builder.shader(),
                createDescriptorSet(builder.shader())
        );
    }

    @Override
    public GraphicsPipeline createGraphicsPipeline(GraphicsPipeline.Builder builder) {
        return new GlGraphicsPipeline(
                builder.shader(),
                builder.rasterization(),
                builder.depthStencil(),
                builder.colorBlend(),
                builder.dynamicStates(),
                builder.vertexFormat(),
                createDescriptorSet(builder.shader())
        );
    }

    @Override
    public ICommandBuffer createCommandBuffer() {
        return new GlCommandBuffer(this);
    }

    @Override
    public ICommandDecoder commandDecoder() {
        return commandDecoder;
    }

    @Override
    public void submitCommandBuffer(ICommandBuffer commandBuffer) {
        commandBuffer.submit(this);
    }
}
