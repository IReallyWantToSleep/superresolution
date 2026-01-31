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

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.FullscreenQuad;
import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.StructuredUniformBuffer;
import io.homo.superresolution.core.graphics.impl.buffer.UniformStructBuilder;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobBuilders;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobResource;
import io.homo.superresolution.core.graphics.impl.grape.RenderGrape;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.state.ColorBlendAttachment;
import io.homo.superresolution.core.graphics.impl.pipeline.state.CullMode;
import io.homo.superresolution.core.graphics.impl.pipeline.state.DynamicStateFlags;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.utils.GlTextureCopier;
import org.lwjgl.opengl.GL41;

public class MotionVectorsGenerator {
    public static GlShaderProgram preprocessShader;
    public static GlShaderProgram pass1Shader;
    public static GlShaderProgram pass2Shader;
    public static GlShaderProgram pass3Shader;
    public static GraphicsPipeline preprocessPipeline;
    public static GraphicsPipeline pass1Pipeline;
    public static GraphicsPipeline pass2Pipeline;
    public static GraphicsPipeline pass3Pipeline;
    public static RenderGrape pipeline;
    public static GlTexture2D currentFrameTexture;
    public static GlTexture2D previousFrameTexture;
    public static GlFrameBuffer gradFrameBuffer;
    public static GlFrameBuffer deltaFrameBuffer;
    public static GlFrameBuffer preprocessFrameBuffer;
    public static GlFrameBuffer motionVectorsFrameBuffer;
    public static GlFrameBuffer inputFrameBuffer;
    public static StructuredUniformBuffer structuredUniformBuffer;
    private static GlBuffer ubo;
    private static boolean isInit;

    public static void initShaders() {
        preprocessShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/motion_vector/preprocess.frag.glsl"))
                        .name("motion_vector_preprocess")
                        .uniformBuffer("motion_vector_data", 0, (int) structuredUniformBuffer.size())
                        .uniformSamplerTexture("tex_current", 1)
                        .build());
        preprocessShader.compile();
        preprocessPipeline = GlGraphicsPipeline.builder()
                .shader(preprocessShader)
                .rasterization(r -> r.cullMode(CullMode.None))
                .depthStencil(r -> r.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                .dynamicStates(DynamicStateFlags.Viewport)
                .colorBlend(r -> r.addAttachment(ColorBlendAttachment.alphaBlend()))
                .vertexFormat(FullscreenQuad.getVertexFormat())
                .build(RenderSystems.opengl().device());

        pass1Shader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/motion_vector/pass1.frag.glsl"))
                        .name("motion_vector_pass1")
                        .uniformBuffer("motion_vector_data", 0, (int) structuredUniformBuffer.size())
                        .uniformSamplerTexture("tex_current", 1)
                        .build());
        pass1Shader.compile();
        pass1Pipeline = GlGraphicsPipeline.builder()
                .shader(pass1Shader)
                .rasterization(r -> r.cullMode(CullMode.None))
                .depthStencil(r -> r.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                .dynamicStates(DynamicStateFlags.Viewport)
                .colorBlend(r -> r.addAttachment(ColorBlendAttachment.alphaBlend()))
                .vertexFormat(FullscreenQuad.getVertexFormat())
                .build(RenderSystems.opengl().device());

        pass2Shader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/motion_vector/pass2.frag.glsl"))
                        .name("motion_vector_pass2")
                        .uniformBuffer("motion_vector_data", 0, (int) structuredUniformBuffer.size())
                        .uniformSamplerTexture("tex_current", 1)
                        .uniformSamplerTexture("tex_previous", 2)
                        .build());
        pass2Shader.compile();
        pass2Pipeline = (GraphicsPipeline) GlGraphicsPipeline.builder()
                .shader(pass2Shader)
                .rasterization(r -> r.cullMode(CullMode.None))
                .depthStencil(r -> r.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                .dynamicStates(DynamicStateFlags.Viewport)
                .colorBlend(r -> r.addAttachment(ColorBlendAttachment.alphaBlend()))
                .vertexFormat(FullscreenQuad.getVertexFormat())
                .build(RenderSystems.opengl().device());

        pass3Shader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/motion_vector/pass3.frag.glsl"))
                        .name("motion_vector_pass3")
                        .uniformBuffer("motion_vector_data", 0, (int) structuredUniformBuffer.size())
                        .uniformSamplerTexture("grad_current", 1)
                        .uniformSamplerTexture("delta_time", 2)
                        .build());
        pass3Shader.compile();
        pass3Pipeline = GlGraphicsPipeline.builder()
                .shader(pass3Shader)
                .rasterization(r -> r.cullMode(CullMode.None))
                .depthStencil(r -> r.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                .dynamicStates(DynamicStateFlags.Viewport)
                .colorBlend(r -> r.addAttachment(ColorBlendAttachment.alphaBlend()))
                .vertexFormat(FullscreenQuad.getVertexFormat())
                .build(RenderSystems.opengl().device());
    }

    public static void init() {
        if (isInit || !SuperResolutionConfig.isGenerateMotionVectors())
            return;
        motionVectorsFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RG16F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());
        motionVectorsFrameBuffer.label("SRMotionVectorsGenerator-MotionVectorsFrameBuffer");
        if (!SuperResolutionConfig.isGenerateMotionVectors())
            return;

        structuredUniformBuffer = UniformStructBuilder.start()
                .floatEntry("exposure")
                .intEntry("window_radius")
                .floatEntry("min_value")
                .floatEntry("scale")
                .build();
        ubo = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .size(structuredUniformBuffer.size())
                        .usage(BufferUsage.Ubo)
                        .build());
        ubo.setBufferData(structuredUniformBuffer);
        initShaders();

        pipeline = new RenderGrape();
        currentFrameTexture = (GlTexture2D) RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .format(TextureFormat.R32F)
                        .usages(TextureUsages.create().sampler().storage())
                        .label("SRMotionVectorsGenerator-currentFrameTexture")
                        .build());
        inputFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RGB16F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());

        previousFrameTexture = (GlTexture2D) RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .format(TextureFormat.R32F)
                        .usages(TextureUsages.create().sampler().storage())
                        .label("SRMotionVectorsGenerator-previousFrameTexture")
                        .build());

        gradFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RG32F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());
        gradFrameBuffer.label("SRMotionVectorsGenerator-GradFrameBuffer");

        deltaFrameBuffer = GlFrameBuffer.create(
                TextureFormat.R32F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());
        gradFrameBuffer.label("SRMotionVectorsGenerator-PreprocessFrameBuffer");
        preprocessFrameBuffer = GlFrameBuffer.create(
                TextureFormat.R32F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());

        pipeline.add("preprocess",
                GrapeJobBuilders.graphics(preprocessPipeline)
                        .resource("tex_current",
                                GrapeJobResource.SamplerTexture.create(
                                        FrameBufferTextureAdapter.ofColor(inputFrameBuffer)))
                        .targetFramebuffer(preprocessFrameBuffer)
                        .build());

        pipeline.add("copy_preprocess_fbo_to_current_frame_texture",
                GrapeJobBuilders.copyTexture()
                        .from(FrameBufferTextureAdapter.ofColor(preprocessFrameBuffer))
                        .to(currentFrameTexture)
                        .build());

        pipeline.add("pass1",
                GrapeJobBuilders.graphics(pass1Pipeline)
                        .resource("tex_current",
                                GrapeJobResource.SamplerTexture.create(
                                        currentFrameTexture))
                        .resource("motion_vector_data",
                                GrapeJobResource.UniformBuffer.create(
                                        ubo))
                        .targetFramebuffer(gradFrameBuffer)
                        .build());

        pipeline.add("pass2",
                GrapeJobBuilders.graphics(pass2Pipeline)
                        .resource("tex_current",
                                GrapeJobResource.SamplerTexture.create(
                                        currentFrameTexture))
                        .resource("tex_previous",
                                GrapeJobResource.SamplerTexture.create(
                                        previousFrameTexture))
                        .resource("motion_vector_data",
                                GrapeJobResource.UniformBuffer.create(
                                        ubo))
                        .targetFramebuffer(deltaFrameBuffer)
                        .build());

        pipeline.add("pass3",
                GrapeJobBuilders.graphics(pass3Pipeline)
                        .resource("grad_current",
                                GrapeJobResource.SamplerTexture.create(
                                        FrameBufferTextureAdapter.ofColor(gradFrameBuffer)))
                        .resource("delta_time",
                                GrapeJobResource.SamplerTexture.create(
                                        FrameBufferTextureAdapter.ofColor(deltaFrameBuffer)))
                        .resource("motion_vector_data",
                                GrapeJobResource.UniformBuffer.create(
                                        ubo))
                        .targetFramebuffer(motionVectorsFrameBuffer)
                        .build());

        pipeline.add("copy_current_frame_texture_to_previous_frame_texture",
                GrapeJobBuilders.copyTexture()
                        .from(currentFrameTexture)
                        .to(previousFrameTexture)
                        .build());

        resize();
        isInit = true;
    }

    public static void resize() {
        if (!isInit)
            return;

        int width = RenderHandlerManager.getRenderWidth();
        int height = RenderHandlerManager.getRenderHeight();

        currentFrameTexture.resize(width, height);
        previousFrameTexture.resize(width, height);
        motionVectorsFrameBuffer.resizeFrameBuffer(width, height);
        gradFrameBuffer.resizeFrameBuffer(width, height);
        deltaFrameBuffer.resizeFrameBuffer(width, height);
        preprocessFrameBuffer.resizeFrameBuffer(width, height);
        inputFrameBuffer.resizeFrameBuffer(width, height);
    }

    public static void update(
            ITexture colorTexture,
            ITexture depthTexture) {
        if (!isInit)
            init();
        if (!isInit)
            return;
        motionVectorsFrameBuffer.clearFrameBuffer();
        GlTextureCopier.copy(
                CopyOperation.create()
                        .src(colorTexture)
                        .dst(inputFrameBuffer.getTexture(FrameBufferAttachmentType.Color))
                        .fromTo(CopyOperation.TextureChancel.R, CopyOperation.TextureChancel.R)
                        .fromTo(CopyOperation.TextureChancel.G, CopyOperation.TextureChancel.G)
                        .fromTo(CopyOperation.TextureChancel.B, CopyOperation.TextureChancel.B));
        structuredUniformBuffer.setFloat("exposure", 3.0f);
        structuredUniformBuffer.setInt("window_radius", 2);
        structuredUniformBuffer.setFloat("min_value", 1e-6f);
        structuredUniformBuffer.setFloat("scale", 4f);
        structuredUniformBuffer.fillBuffer();
        ubo.upload();
        GL41.glDisable(GL41.GL_DEPTH_TEST);
        GL41.glDisable(GL41.GL_CULL_FACE);
        RenderSystems.opengl().device().commandDecoder().beginCommandBuffer();
        ICommandBuffer commandBuffer = RenderSystems.current().device().commandDecoder().currentCommandBuffer();
        pipeline.execute(commandBuffer, "preprocess");
        pipeline.execute(commandBuffer, "copy_preprocess_fbo_to_current_frame_texture");
        pipeline.execute(commandBuffer, "pass1");
        pipeline.execute(commandBuffer, "pass2");
        pipeline.execute(commandBuffer, "pass3");
        pipeline.execute(commandBuffer, "copy_current_frame_texture_to_previous_frame_texture");
        RenderSystems.opengl().device().commandDecoder().endAndSubmitCommandBuffer();
        GL41.glEnable(GL41.GL_DEPTH_TEST);
        GL41.glEnable(GL41.GL_CULL_FACE);
    }

    public static void destroy() {
        currentFrameTexture.destroy();
        previousFrameTexture.destroy();
        motionVectorsFrameBuffer.destroy();
        gradFrameBuffer.destroy();
        deltaFrameBuffer.destroy();
        preprocessFrameBuffer.destroy();
        preprocessShader.destroy();
        pass1Shader.destroy();
        pass2Shader.destroy();
        pass3Shader.destroy();
        inputFrameBuffer.destroy();
    }

    public static IFrameBuffer getMotionVectorsFrameBuffer() {
        return motionVectorsFrameBuffer;
    }
}