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

package io.homo.superresolution.common.upscale.fsr1;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.*;
import io.homo.superresolution.core.graphics.impl.pipeline.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformAccess;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.math.Vector3i;

import java.util.Optional;

public class FSR1 extends AbstractAlgorithm {
    private IShaderProgram<?> fsr1EASUShader;
    private IShaderProgram<?> fsr1RCASShader;
    private Pipeline fsrUpscalePipeline;
    private ITexture fsr1TempTexture;
    private IFrameBuffer outputFbo;
    private ITexture output;
    private StructuredUniformBuffer fsr1UBOData;
    private IBuffer fsr1UBO;

    @Override
    public void init() {
        fsr1UBOData = UniformStructBuilder.start()
                .vec2Entry("renderViewportSize")
                .vec2Entry("containerTextureSize")
                .vec2Entry("upscaledViewportSize")
                .floatEntry("sharpness")
                .build();
        fsr1UBO = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .size(fsr1UBOData.size())
                        .usage(BufferUsage.UBO)
                        .build()
        );
        fsr1UBO.setBufferData(fsr1UBOData);
        initShader();
        fsrUpscalePipeline = new Pipeline();
        fsr1TempTexture = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getRenderWidth())
                        .height(RenderHandlerManager.getRenderHeight())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .usages(TextureUsages.create().sampler().storage())
                        .label("Fsr1TempTexture")
                        .build()
        );
        output = RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(RenderHandlerManager.getScreenWidth())
                        .height(RenderHandlerManager.getScreenHeight())
                        .format(SuperResolutionConfig.getInternalTextureFormat())
                        .usages(TextureUsages.create().sampler().storage())
                        .label("Fsr1OutputTexture")
                        .build()
        );
        outputFbo = GlFrameBuffer.create(
                output,
                null,
                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight()
        );
        outputFbo.label("Fsr1OutputFbo");
        fsrUpscalePipeline.job("fsr1_easu",
                PipelineJobBuilders.compute(fsr1EASUShader)
                        .resource("inImage",
                                PipelineJobResource.SamplerTexture.create(() -> Optional.ofNullable(getResources().colorTexture()))
                        )
                        .resource("outImage",
                                PipelineJobResource.StorageTexture.create(
                                        fsr1TempTexture,
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("fsr1_data",
                                PipelineJobResource.UniformBuffer.create(
                                        fsr1UBO
                                )
                        )
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build()
        );
        fsrUpscalePipeline.job("fsr1_rcas",
                PipelineJobBuilders.compute(fsr1RCASShader)
                        .resource("inImage",
                                PipelineJobResource.StorageTexture.create(
                                        fsr1TempTexture,
                                        PipelineResourceAccess.Read
                                )
                        )
                        .resource("outImage",
                                PipelineJobResource.StorageTexture.create(
                                        output,
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("fsr1_data",
                                PipelineJobResource.UniformBuffer.create(
                                        fsr1UBO
                                )
                        )
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build()
        );
        this.resize(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
    }

    public void initShader() {
        int fp16 = SuperResolutionConfig.SPECIAL.FSR1.FP16.get() ? checkFP16Support() : 0;
        fsr1EASUShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(ShaderSource.file(ShaderType.COMPUTE, "/shader/fsr1/fsr1_main.comp.glsl"))
                        .name("FSR1_EASU")
                        .addDefine("FSR_FP16_CRITERIA", String.valueOf(fp16))
                        .addDefine("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                        .addDefine("FSR_EASU", String.valueOf(1))
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT", SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("fsr1_data", 0, (int) fsr1UBOData.size())
                        .uniformSamplerTexture("inImage", 0)
                        .uniformStorageTexture("outImage", ShaderUniformAccess.Write, 1)
                        .build()
        );
        fsr1RCASShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(ShaderSource.file(ShaderType.COMPUTE, "/shader/fsr1/fsr1_main.comp.glsl"))
                        .name("FSR1_RCAS")
                        .addDefine("FSR_FP16_CRITERIA", String.valueOf(fp16))
                        .addDefine("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                        .addDefine("FSR_RCAS", String.valueOf(1))
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT", SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("fsr1_data", 0, (int) fsr1UBOData.size())
                        .uniformStorageTexture("inImage", ShaderUniformAccess.Read, 0)
                        .uniformStorageTexture("outImage", ShaderUniformAccess.Write, 1)
                        .build()
        );
        if (fp16 == 2) {
            if (fsr1EASUShader instanceof GlShaderProgram) {
                ((GlShaderProgram) fsr1EASUShader).compile(true);
            }
            if (fsr1RCASShader instanceof GlShaderProgram) {
                ((GlShaderProgram) fsr1RCASShader).compile(true);
            }
        } else {
            fsr1EASUShader.compile();
            fsr1RCASShader.compile();
        }
    }

    private Vector3i getWorkGroupSize() {
        int workRegionDim = 16;
        int dispatchX = (RenderHandlerManager.getScreenWidth() + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (RenderHandlerManager.getScreenHeight() + (workRegionDim - 1)) / workRegionDim;
        return new Vector3i(
                dispatchX,
                dispatchY,
                1
        );
    }

    public static int checkFP16Support() {
        if (GraphicsCapabilities.hasGLExtension("GL_EXT_shader_16bit_storage") &&
                GraphicsCapabilities.hasGLExtension("GL_EXT_shader_explicit_arithmetic_types")
        ) {
            return 1;
        }
        if (GraphicsCapabilities.hasGLExtension("GL_NV_gpu_shader5")) {
            return 2;
        }
        return 0;
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        RenderSystems.current().device().commandEncoder().begin();
        fsr1UBOData.setVec2("renderViewportSize", RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight());
        fsr1UBOData.setVec2("containerTextureSize", RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight());
        fsr1UBOData.setVec2("upscaledViewportSize", RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight());
        fsr1UBOData.setFloat("sharpness", SuperResolutionConfig.getSharpness());
        fsr1UBOData.fillBuffer();
        fsr1UBO.upload();
        PipelineComputeJob easuJob = (PipelineComputeJob) fsrUpscalePipeline.job("fsr1_easu");
        PipelineJobResource.SamplerTexture inImageResource =
                (PipelineJobResource.SamplerTexture) easuJob.resource("inImage");

        if (inImageResource != null && getResources().colorTexture() != null) {
            inImageResource.setResource(getResources().colorTexture());
        }
        fsrUpscalePipeline.execute(RenderSystems.current().device().commandEncoder().getCommandBuffer());
        RenderSystems.current().device().submitCommandBuffer(
                RenderSystems.current()
                        .device()
                        .commandEncoder()
                        .end()
        );
        return true;
    }


    @Override
    public void destroy() {
        output.destroy();
        fsr1TempTexture.destroy();
        fsr1EASUShader.destroy();
        fsr1RCASShader.destroy();
        fsr1UBOData.free();
        fsr1UBO.destroy();
        outputFbo.destroy();
    }

    @Override
    public void resize(int width, int height) {
        fsr1TempTexture.resize(width, height);
        output.resize(width, height);
        outputFbo.resizeFrameBuffer(width, height);
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFbo;
    }

    @Override
    public int getOutputTextureId() {
        return Math.toIntExact(output.handle());
    }
}
