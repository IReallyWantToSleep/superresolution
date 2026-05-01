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
import io.homo.superresolution.core.graphics.impl.command.CommandPoolFlags;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerDescription;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandBuffer;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandDecoder;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandPool;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlPipelineDescriptorSet;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlRenderPass;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.*;
import io.homo.superresolution.core.graphics.opengl.vertex.GlVertexBuffer;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_RG;

public class GlDevice implements IDevice {
    private final GlCommandDecoder commandDecoder;
    private final GlCommandPool defaultCommandPool;

    public GlDevice() {
        this.commandDecoder = new GlCommandDecoder(this);
        this.defaultCommandPool = new GlCommandPool(this, java.util.EnumSet.of(CommandPoolFlags.Reset));
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
    public GlSampler createSampler(SamplerDescription description) {
        return new GlSampler(description);
    }

    @Override
    public GlTextureView createTextureView(TextureViewDescription description) {
        return GlTextureView.create(description);
    }

    @Override
    public GlFrameBuffer createFramebuffer(FramebufferDescription description) {
        ITexture colorTex = description.getColorAttachment();
        ITexture depthTex = description.getDepthAttachment();

        if (colorTex == null && description.getColorFormat() != null) {
            colorTex = createTexture(TextureDescription.create()
                    .type(TextureType.Texture2D)
                    .format(description.getColorFormat())
                    .size(description.getWidth(), description.getHeight())
                    .usages(TextureUsages.create().storage().sampler().attachmentColor())
                    .build());
        }
        if (depthTex == null && description.getDepthFormat() != null) {
            depthTex = createTexture(TextureDescription.create()
                    .type(TextureType.Texture2D)
                    .format(description.getDepthFormat())
                    .size(description.getWidth(), description.getHeight())
                    .usages(TextureUsages.create().storage().sampler().attachmentDepth())
                    .build());
        }

        GlFrameBuffer fbo = GlFrameBuffer.create(
                colorTex,
                depthTex,
                description.getWidth(),
                description.getHeight()
        );
        if (description.getLabel() != null) {
            fbo.label(description.getLabel());
        }
        return fbo;
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
    public GlRenderPass createRenderPass(RenderPass.Builder builder) {
        return new GlRenderPass(
                builder.getFrameBuffer(),
                builder.getClearState()
        );
    }

    @Override
    public GlPipelineDescriptorSet createDescriptorSet(IShaderProgram shader) {
        return new GlPipelineDescriptorSet(shader);
    }

    @Override
    public GlComputePipeline createComputePipeline(ComputePipeline.Builder builder) {
        return new GlComputePipeline(
                builder.shader(),
                createDescriptorSet(builder.shader())
        );
    }

    @Override
    public GlGraphicsPipeline createGraphicsPipeline(GraphicsPipeline.Builder builder) {
        return new GlGraphicsPipeline(
                builder.shader(),
                builder.renderPass(),
                builder.rasterization(),
                builder.depthStencil(),
                builder.colorBlend(),
                builder.dynamicStates(),
                builder.primitiveType(),
                builder.vertexFormat(),
                createDescriptorSet(builder.shader())
        );
    }

    @Override
    public GlCommandBuffer createCommandBuffer() {
        return defaultCommandPool.createCommandBuffer();
    }

    @Override
    public GlCommandPool createCommandPool(CommandPoolFlags... flags) {
        EnumSet<CommandPoolFlags> poolFlags = EnumSet.noneOf(CommandPoolFlags.class);
        if (flags != null) {
            Collections.addAll(poolFlags, flags);
        }
        return new GlCommandPool(this, poolFlags);
    }

    @Override
    public GlCommandPool defaultCommandPool() {
        return defaultCommandPool;
    }

    @Override
    public GlCommandDecoder commandDecoder() {
        return commandDecoder;
    }

    @Override
    public void submitCommandBuffer(ICommandBuffer commandBuffer) {
        commandBuffer.submit(this);
    }

    public GlImportableTexture2D createTextureImportable(ITexture exportedTexture) {
        if (exportedTexture instanceof VulkanTexture) {
            return new GlImportableTexture2D((VulkanTexture) exportedTexture);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public void uploadTextureData(ITexture texture, ByteBuffer data, int x, int y, int width, int height) {
        if (!(texture instanceof GlTexture2D glTexture)) {
            throw new IllegalArgumentException("uploadTextureData: 仅支持 GlTexture2D，实际类型: " + texture.getClass().getSimpleName());
        }
        TextureFormat format = texture.getTextureFormat();
        int pixelFormat = switch (format) {
            case RGBA8, RGBA16, RGBA16F, RGBA32F -> GL_RGBA;
            case RGB8, RGB16F -> GL_RGB;
            case R8, R16F, R32F, R32UI, R16_SNORM -> GL_RED;
            case RG8, RG16F, RG32F -> GL_RG;
            default -> throw new IllegalArgumentException("uploadTextureData: 不支持的纹理格式: " + format);
        };
        glTexture.uploadData(0, x, y, width, height, pixelFormat, GL_UNSIGNED_BYTE, data, 1);
    }
}
