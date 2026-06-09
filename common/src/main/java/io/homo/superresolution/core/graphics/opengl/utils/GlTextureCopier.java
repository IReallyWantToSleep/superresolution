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

package io.homo.superresolution.core.graphics.opengl.utils;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.FullscreenQuad;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.state.ColorBlendAttachment;
import io.homo.superresolution.core.graphics.impl.pipeline.state.CullMode;
import io.homo.superresolution.core.graphics.impl.pipeline.state.DynamicStateFlags;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlRenderPass;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;
import org.lwjgl.opengl.GL43;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GlTextureCopier {
    private static final Map<String, RenderPass> programMap = new HashMap<>();
    private static final Map<String, GlGraphicsPipeline> graphicsPipelineMap = new HashMap<>();
    private static final Map<String, GlComputePipeline> computeProgramMap = new HashMap<>();
    private static GlSampler sampler;
    private static IFrameBuffer cachedFrameBuffer;

    public static IFrameBuffer getCachedFrameBuffer() {
        return cachedFrameBuffer;
    }


    private static GlComputePipeline getOrCreateComputeProgram(CopyOperation copyOperation) {
        String key = mappingKey(copyOperation.getMappings());
        if (computeProgramMap.containsKey(key)) {
            return computeProgramMap.get(key);
        }
        ShaderDescription.Builder builder =
                ShaderDescription.compute(new ShaderSource(ShaderType.Compute, "/shader/copy.comp.glsl", true));
        builder.name("copy-texture." + key.replace("->", "to").replace(",", "+"));
        builder.addDefine("COPY_CHANNEL", String.valueOf(copyOperation.getMappings().size()));
        for (int i = 0; i < copyOperation.getMappings().size(); i++) {
            CopyOperation.ChannelMapping map = copyOperation.getMappings().get(i);
            builder.addDefine("COPY_SRC_CHANNEL" + i, String.valueOf(map.src.ordinal()));
            builder.addDefine("COPY_DST_CHANNEL" + i, String.valueOf(map.dst.ordinal()));
        }
        builder.addDefine("COPY_DST_FORMAT", copyOperation.getDstTexture().getTextureFormat().getGlslFormatQualifier());

        builder.uniformSamplerTexture("tex", 0);
        builder.uniformStorageTexture("outImage", ShaderResourceAccess.Write, 0);

        GlShaderProgram program = RenderSystems.opengl().device().createShaderProgram(builder.build());
        program.compile();
        GlComputePipeline computePipeline = (GlComputePipeline) GlComputePipeline.builder()
                .shader(program)
                .build(RenderSystems.opengl().device());
        computeProgramMap.put(key, computePipeline);
        return computePipeline;
    }

    private static RenderPass getOrCreateProgram(CopyOperation copyOperation) {
        String key = mappingKey(copyOperation.getMappings());
        if (programMap.containsKey(key)) {
            return programMap.get(key);
        }
        ShaderDescription.Builder builder =
                ShaderDescription.graphics(
                        new ShaderSource(ShaderType.Fragment, "/shader/copy.frag.glsl", true),
                        new ShaderSource(ShaderType.Vertex, "/shader/copy.vert.glsl", true)
                );

        builder.addDefine("COPY_CHANNEL", String.valueOf(copyOperation.getMappings().size()));
        for (int i = 0; i < copyOperation.getMappings().size(); i++) {
            CopyOperation.ChannelMapping map = copyOperation.getMappings().get(i);
            builder.addDefine("COPY_SRC_CHANNEL" + i, String.valueOf(map.src.ordinal()));
            builder.addDefine("COPY_DST_CHANNEL" + i, String.valueOf(map.dst.ordinal()));
        }

        builder.uniformSamplerTexture("tex", 0);

        GlShaderProgram program = RenderSystems.opengl().device().createShaderProgram(builder.build());
        program.compile();
        RenderPass renderPass = RenderPass.builder()
                .frameBuffer(getCachedFrameBuffer())
                .build(RenderSystems.opengl().device());
        GlGraphicsPipeline graphicsPipeline = (GlGraphicsPipeline) GlGraphicsPipeline.builder()
                .shader(program)
                .renderPass(renderPass)
                .primitiveType(PrimitiveType.TriangleStrip)
                .rasterization(r -> r.cullMode(CullMode.None))
                .depthStencil(r -> r.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                .dynamicStates(DynamicStateFlags.Viewport)
                .colorBlend(r -> r.addAttachment(ColorBlendAttachment.alphaBlend()))
                .vertexFormat(FullscreenQuad.getVertexFormat())
                .build(RenderSystems.opengl().device());
        programMap.put(key, renderPass);
        graphicsPipelineMap.put(key, graphicsPipeline);
        return renderPass;
    }

    private static String mappingKey(List<CopyOperation.ChannelMapping> mappings) {
        return mappings.stream()
                .map(m -> m.src.ordinal() + "->" + m.dst.ordinal())
                .collect(Collectors.joining(","));
    }


    public static void copy(CopyOperation copyOperation) {
        GlDebug.pushGroup(GlDebug.nextCopyId(), "CopyTexture");
        if (sampler == null) {
            sampler = GlSampler.create(GlSampler.SamplerType.NearestClamp);
        }
        if (copyOperation.getDstTexture().getTextureFormat().getGlslFormatQualifier() != null) {
            GlComputePipeline pipeline = getOrCreateComputeProgram(copyOperation);
            //用线性采样，防止出现锯齿
            GL43.glBindSampler(0, (int) sampler.handle());
            pipeline.descriptorSet()
                    .storageImage("outImage", copyOperation.getDstTexture())
                    .samplerTexture("tex", copyOperation.getSrcTexture());
            pipeline.descriptorSet().update();
            ICommandBuffer commandBuffer = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
            commandBuffer.begin();
            commandBuffer.bindPipeline(pipeline);
            commandBuffer.dispatch(
                    (int) Math.ceil((double) copyOperation.getSrcTexture().getWidth() / 16),
                    (int) Math.ceil((double) copyOperation.getSrcTexture().getHeight() / 16),
                    1
            );
            commandBuffer.end();
            RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
            GL43.glBindSampler(0, 0);
            GlDebug.popGroup();
            return;
        }

        try (GlState state = new GlState(GlState.STATE_READ_FBO | GlState.STATE_DRAW_FBO)) {
            if (cachedFrameBuffer == null) {
                cachedFrameBuffer = RenderSystems.current().device().createFramebuffer(
                        FramebufferDescription.create()
                                .colorAttachment(copyOperation.getDstTexture())
                                .build()
                );
                cachedFrameBuffer.label("CopyOperationTempFrameBuffer");
            }
            GlRenderPass pass = (GlRenderPass) getOrCreateProgram(copyOperation);
            GlGraphicsPipeline graphicsPipeline = graphicsPipelineMap.get(mappingKey(copyOperation.getMappings()));
            graphicsPipeline.descriptorSet()
                    .samplerTexture("tex", copyOperation.getSrcTexture());
            graphicsPipeline.descriptorSet().update();
            IVertexBuffer vertexBuffer = FullscreenQuad.create(RenderSystems.opengl().device());
            ICommandBuffer commandBuffer = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
            commandBuffer.begin();
            commandBuffer.beginRenderPass(pass);
            commandBuffer.bindPipeline(graphicsPipeline);
            commandBuffer.draw(vertexBuffer, 4, 0);
            commandBuffer.endRenderPass();
            commandBuffer.end();
            RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
        }
        GlDebug.popGroup();
    }
}
