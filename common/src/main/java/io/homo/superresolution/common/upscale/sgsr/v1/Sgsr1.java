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

package io.homo.superresolution.common.upscale.sgsr.v1;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.FullscreenQuad;
import io.homo.superresolution.core.graphics.impl.buffer.*;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobBuilders;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobResource;
import io.homo.superresolution.core.graphics.impl.grape.RenderGrape;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.state.ColorBlendAttachment;
import io.homo.superresolution.core.graphics.impl.pipeline.state.CullMode;
import io.homo.superresolution.core.graphics.impl.pipeline.state.DynamicStateFlags;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import org.lwjgl.opengl.GL41;

import java.util.Optional;

public class Sgsr1 extends AbstractAlgorithm {
    private IShaderProgram sgsrShader;
    private GraphicsPipeline sgsrPipeline;
    private RenderGrape pipeline;
    private ITexture output;
    private IFrameBuffer outputFbo;
    private StructuredData buffer;
    private IBuffer ubo;

    @Override
    public void init() {
        buffer = Std140StructBuilder.start()
                .vec4Entry("ViewportInfo")
                .build();
        ubo = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .usage(BufferUsage.Ubo)
                        .size(buffer.size())
                        .build());
        ubo.setBufferData(buffer);
        output = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .usages(TextureUsages.create().sampler().storage().attachmentColor())
                        .label("Sgsr1Output")
                        .build());
        outputFbo = GlFrameBuffer.create(
                output,
                null,
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight());
        outputFbo.label("Sgsr1OutputFbo");
        sgsrShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/sgsr/v1/sgsr1_shader.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/sgsr/v1/sgsr1_shader.frag.glsl"))
                        .name("SGSRV1")
                        .addDefine("UseEdgeDirection", "")
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT",
                                SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("sgsr1_data", 0, (int) buffer.size())
                        .uniformSamplerTexture("ps0", 1)
                        .build());
        sgsrShader.compile();
        sgsrPipeline = (GraphicsPipeline) GlGraphicsPipeline.builder()
                .shader(sgsrShader)
                .rasterization(r -> r.cullMode(CullMode.None))
                .depthStencil(r -> r.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                .dynamicStates(DynamicStateFlags.Viewport)
                .colorBlend(r -> r.addAttachment(ColorBlendAttachment.alphaBlend()))
                .vertexFormat(FullscreenQuad.getVertexFormat())
                .build(RenderSystems.opengl().device());
        pipeline = new RenderGrape();
        pipeline.add("sgsr1_main",
                GrapeJobBuilders.graphics(sgsrPipeline)
                        .resource("ps0",
                                GrapeJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(getResources().colorTexture())))
                        .resource("sgsr1_data",
                                GrapeJobResource.UniformBuffer.create(
                                        ubo))
                        .targetFramebuffer(outputFbo)
                        .build());

        this.resize(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        buffer.setVec4(
                "ViewportInfo",
                1.0f / dispatchResource.renderWidth(),
                1.0f / dispatchResource.renderHeight(),
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight()

        );
        buffer.fillBuffer();
        ubo.upload();
        outputFbo.clearFrameBuffer();
        GL41.glDisable(GL41.GL_DEPTH_TEST);
        GL41.glDisable(GL41.GL_CULL_FACE);
        RenderSystems.current().device().commandDecoder().beginCommandBuffer();
        pipeline.execute(RenderSystems.current().device().commandDecoder().currentCommandBuffer());
        RenderSystems.current().device().commandDecoder().endAndSubmitCommandBuffer();
        GL41.glEnable(GL41.GL_DEPTH_TEST);
        GL41.glEnable(GL41.GL_CULL_FACE);
        return true;
    }

    @Override
    public void resize(int width, int height) {
        output.resize(width, height);
        outputFbo.resizeFrameBuffer(width, height);
    }

    @Override
    public void destroy() {
        output.destroy();
        sgsrShader.destroy();
        sgsrPipeline.destroy();
        pipeline.destroy();
        outputFbo.destroy();
        buffer.free();
        ubo.destroy();
    }

    @Override
    public int getOutputTextureId() {
        return Math.toIntExact(output.handle());
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFbo;
    }
}