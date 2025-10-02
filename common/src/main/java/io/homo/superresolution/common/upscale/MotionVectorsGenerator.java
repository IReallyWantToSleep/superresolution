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

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.StructuredUniformBuffer;
import io.homo.superresolution.core.graphics.impl.buffer.UniformStructBuilder;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.Pipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobBuilders;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.buffer.GlBuffer;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.utils.GlTextureCopier;
import org.lwjgl.opengl.GL41;

public class MotionVectorsGenerator {
    public static GlShaderProgram preprocessShader;
    public static GlShaderProgram pass1Shader;
    public static GlShaderProgram pass2Shader;
    public static GlShaderProgram pass3Shader;
    public static Pipeline pipeline;
    public static GlTexture2D currentFrameTexture;
    public static GlTexture2D previousFrameTexture;
    public static GlFrameBuffer gradFrameBuffer;
    public static GlFrameBuffer deltaFrameBuffer;
    public static GlFrameBuffer preprocessFrameBuffer;
    public static GlFrameBuffer motionVectorsFrameBuffer;
    public static GlFrameBuffer inputFrameBuffer;
    public static StructuredUniformBuffer structuredUniformBuffer;
    private static GlBuffer ubo;

    public static void initShaders() {
        preprocessShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/motion_vector/preprocess.frag.glsl"))
                        .name("motion_vector_preprocess")
                        .uniformBuffer("motion_vector_data", 0, (int) structuredUniformBuffer.size())
                        .uniformSamplerTexture("tex_current", 1)
                        .build()
        );
        preprocessShader.compile();

        pass1Shader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/motion_vector/pass1.frag.glsl"))
                        .name("motion_vector_pass1")
                        .uniformBuffer("motion_vector_data", 0, (int) structuredUniformBuffer.size())
                        .uniformSamplerTexture("tex_current", 1)
                        .build()
        );
        pass1Shader.compile();

        pass2Shader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/motion_vector/pass2.frag.glsl"))
                        .name("motion_vector_pass2")
                        .uniformBuffer("motion_vector_data", 0, (int) structuredUniformBuffer.size())
                        .uniformSamplerTexture("tex_current", 1)
                        .uniformSamplerTexture("tex_previous", 2)
                        .build()
        );
        pass2Shader.compile();

        pass3Shader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/motion_vector/pass3.frag.glsl"))
                        .name("motion_vector_pass3")
                        .uniformBuffer("motion_vector_data", 0, (int) structuredUniformBuffer.size())
                        .uniformSamplerTexture("grad_current", 1)
                        .uniformSamplerTexture("delta_time", 2)
                        .build()
        );
        pass3Shader.compile();
    }

    public static void init() {
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
                        .build()
        );
        ubo.setBufferData(structuredUniformBuffer);
        initShaders();

        pipeline = new Pipeline();
        currentFrameTexture = (GlTexture2D) RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .format(TextureFormat.R32F)
                        .usages(TextureUsages.create().sampler().storage())
                        .label("SRMotionVectorsGenerator-currentFrameTexture")
                        .build()
        );
        inputFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RGB16F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight()
        );

        previousFrameTexture = (GlTexture2D) RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .format(TextureFormat.R32F)
                        .usages(TextureUsages.create().sampler().storage())
                        .label("SRMotionVectorsGenerator-previousFrameTexture")
                        .build()
        );
        motionVectorsFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RG16F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight()
        );
        motionVectorsFrameBuffer.label("SRMotionVectorsGenerator-MotionVectorsFrameBuffer");

        gradFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RG32F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight()
        );
        gradFrameBuffer.label("SRMotionVectorsGenerator-GradFrameBuffer");

        deltaFrameBuffer = GlFrameBuffer.create(
                TextureFormat.R32F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight()
        );
        gradFrameBuffer.label("SRMotionVectorsGenerator-PreprocessFrameBuffer");
        preprocessFrameBuffer = GlFrameBuffer.create(
                TextureFormat.R32F,
                null,
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight()
        );

        pipeline.job("preprocess",
                PipelineJobBuilders.graphics(preprocessShader)
                        .resource("tex_current",
                                PipelineJobResource.SamplerTexture.create(
                                        FrameBufferTextureAdapter.ofColor(inputFrameBuffer)
                                )
                        )
                        .targetFramebuffer(preprocessFrameBuffer)
                        .build()
        );

        pipeline.job("copy_preprocess_fbo_to_current_frame_texture",
                PipelineJobBuilders.copyTexture()
                        .from(FrameBufferTextureAdapter.ofColor(preprocessFrameBuffer))
                        .to(currentFrameTexture)
                        .build()
        );

        pipeline.job("pass1",
                PipelineJobBuilders.graphics(pass1Shader)
                        .resource("tex_current",
                                PipelineJobResource.SamplerTexture.create(
                                        currentFrameTexture
                                )
                        )
                        .resource("motion_vector_data",
                                PipelineJobResource.UniformBuffer.create(
                                        ubo
                                )
                        )
                        .targetFramebuffer(gradFrameBuffer)
                        .build()
        );

        pipeline.job("pass2",
                PipelineJobBuilders.graphics(pass2Shader)
                        .resource("tex_current",
                                PipelineJobResource.SamplerTexture.create(
                                        currentFrameTexture
                                )
                        )
                        .resource("tex_previous",
                                PipelineJobResource.SamplerTexture.create(
                                        previousFrameTexture
                                )
                        )
                        .resource("motion_vector_data",
                                PipelineJobResource.UniformBuffer.create(
                                        ubo
                                )
                        )
                        .targetFramebuffer(deltaFrameBuffer)
                        .build()
        );

        pipeline.job("pass3",
                PipelineJobBuilders.graphics(pass3Shader)
                        .resource("grad_current",
                                PipelineJobResource.SamplerTexture.create(
                                        FrameBufferTextureAdapter.ofColor(gradFrameBuffer)
                                )
                        )
                        .resource("delta_time",
                                PipelineJobResource.SamplerTexture.create(
                                        FrameBufferTextureAdapter.ofColor(deltaFrameBuffer)
                                )
                        )
                        .resource("motion_vector_data",
                                PipelineJobResource.UniformBuffer.create(
                                        ubo
                                )
                        )
                        .targetFramebuffer(motionVectorsFrameBuffer)
                        .build()
        );

        pipeline.job("copy_current_frame_texture_to_previous_frame_texture",
                PipelineJobBuilders.copyTexture()
                        .from(currentFrameTexture)
                        .to(previousFrameTexture)
                        .build()
        );

        resize();
    }

    public static void resize() {
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
            ITexture depthTexture
    ) {
        motionVectorsFrameBuffer.clearFrameBuffer();
        GlTextureCopier.copy(
                CopyOperation.create()
                        .src(colorTexture)
                        .dst(inputFrameBuffer.getTexture(FrameBufferAttachmentType.Color))
                        .fromTo(CopyOperation.TextureChancel.R, CopyOperation.TextureChancel.R)
                        .fromTo(CopyOperation.TextureChancel.G, CopyOperation.TextureChancel.G)
                        .fromTo(CopyOperation.TextureChancel.B, CopyOperation.TextureChancel.B)
        );
        structuredUniformBuffer.setFloat("exposure", 3.0f);
        structuredUniformBuffer.setInt("window_radius", 2);
        structuredUniformBuffer.setFloat("min_value", 1e-6f);
        structuredUniformBuffer.setFloat("scale", 4f);
        structuredUniformBuffer.fillBuffer();
        ubo.upload();
        GL41.glDisable(GL41.GL_DEPTH_TEST);
        GL41.glDisable(GL41.GL_CULL_FACE);
        RenderSystems.opengl().device().commandEncoder().begin();
        ICommandBuffer commandBuffer = RenderSystems.current().device().commandEncoder().getCommandBuffer();
        pipeline.executeJob(commandBuffer, "preprocess");
        pipeline.executeJob(commandBuffer, "copy_preprocess_fbo_to_current_frame_texture");
        pipeline.executeJob(commandBuffer, "pass1");
        pipeline.executeJob(commandBuffer, "pass2");
        pipeline.executeJob(commandBuffer, "pass3");
        pipeline.executeJob(commandBuffer, "copy_current_frame_texture_to_previous_frame_texture");
        RenderSystems.opengl().device().commandEncoder().end();
        RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
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