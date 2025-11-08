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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.*;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.Pipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobBuilders;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import org.lwjgl.opengl.GL41;

import java.util.Optional;

public class GUIAntiAliasingPostprocessing {
    private IShaderProgram<?> sgsrShader;
    private Pipeline pipeline;
    private ITexture output;

    public IFrameBuffer getOutputFramebuffer() {
        return outputFbo;
    }

    private IFrameBuffer outputFbo;
    private StructuredUniformBuffer buffer;
    private IBuffer ubo;
    private ITexture input;

    public void init() {
        buffer = UniformStructBuilder.start()
                .vec4Entry("ViewportInfo")
                .build();
        ubo = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .usage(BufferUsage.Ubo)
                        .size(buffer.size())
                        .build()
        );
        ubo.setBufferData(buffer);
        output = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .usages(TextureUsages.create().sampler().storage().attachmentColor())
                        .label("Sgsr1Output")
                        .build()
        );
        outputFbo = GlFrameBuffer.create(
                output,
                null,
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight()
        );
        outputFbo.label("Sgsr1OutputFbo");
        sgsrShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.Vertex, "/shader/sgsr/v1/sgsr1_shader.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.Fragment, "/shader/sgsr/v1/sgsr1_shader.frag.glsl"))
                        .name("SGSRV1")
                        .addDefine("UseEdgeDirection", "")
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT", SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("sgsr1_data", 0, (int) buffer.size())
                        .uniformSamplerTexture("ps0", 1)
                        .build()
        );
        sgsrShader.compile();
        pipeline = new Pipeline();
        pipeline.job("sgsr1_main",
                PipelineJobBuilders.graphics(sgsrShader)
                        .resource("ps0",
                                PipelineJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(input)
                                )
                        )
                        .resource("sgsr1_data",
                                PipelineJobResource.UniformBuffer.create(
                                        ubo
                                )
                        )
                        .targetFramebuffer(outputFbo)
                        .build()
        );

        this.resize(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
    }

    public void resize(int width, int height) {
        output.resize(width, height);
        outputFbo.resizeFrameBuffer(width, height);
    }

    public void dispatch(ITexture inputTexture) {
        buffer.setVec4(
                "ViewportInfo",
                1.0f / inputTexture.getWidth(),
                1.0f / inputTexture.getHeight(),
                inputTexture.getWidth(),
                inputTexture.getHeight()

        );
        buffer.fillBuffer();
        ubo.upload();
        outputFbo.clearFrameBuffer();
        GL41.glDisable(GL41.GL_DEPTH_TEST);
        GL41.glDisable(GL41.GL_CULL_FACE);
        RenderSystems.current().device().commandEncoder().begin();
        pipeline.execute(RenderSystems.current().device().commandEncoder().getCommandBuffer());
        RenderSystems.current().device().submitCommandBuffer(
                RenderSystems.current()
                        .device()
                        .commandEncoder()
                        .end()
        );
        GL41.glEnable(GL41.GL_DEPTH_TEST);
        GL41.glEnable(GL41.GL_CULL_FACE);
    }
}
