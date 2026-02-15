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

package io.homo.superresolution.common.upscale.sgsr.v2.variants;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.AbstractSgsrVariant;
import io.homo.superresolution.common.upscale.sgsr.v2.Sgsr2;
import io.homo.superresolution.common.upscale.sgsr.v2.SgsrUtils;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobBuilders;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobResource;
import io.homo.superresolution.core.graphics.impl.grape.GrapeResourceAccess;
import io.homo.superresolution.core.graphics.impl.grape.RenderGrape;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import org.joml.Vector3i;

import java.util.Optional;

public class Sgsr2PassCompute extends AbstractSgsrVariant {
    private GlShaderProgram convertShader;
    private GlShaderProgram upscaleShader;
    private ComputePipeline convertPipeline;
    private ComputePipeline upscalePipeline;
    private RenderGrape sgsrPipeline;
    private ITexture PrevLumaHistory;
    private ITexture YCoCgColor;

    private ITexture MotionDepthClipAlphaBuffer;
    private ITexture PrevHistoryOutput;
    private ITexture HistoryOutput;

    private Vector3i getWorkGroupSize() {
        int dispatchX = SgsrUtils.divideRoundUp(RenderHandlerManager.getScreenWidth(), 8);
        int dispatchY = SgsrUtils.divideRoundUp(RenderHandlerManager.getScreenHeight(), 8);
        return new Vector3i(
                dispatchX,
                dispatchY,
                1);
    }

    @Override
    public void dispatch(DispatchResource resource, Sgsr2 sgsr) {
        swapHistoryOutput();
        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        commandBuffer.begin();
        sgsrPipeline.execute(commandBuffer, "convert");
        sgsrPipeline.execute(commandBuffer, "upscale");
        commandBuffer.end();
        RenderSystems.current().device().submitCommandBuffer(commandBuffer);
    }

    @Override
    public void init(Sgsr2 sgsr) {
        convertShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.Compute, "/shader/sgsr/2pass_cs/sgsr2_convert.comp.glsl",
                                true))
                        .name("SGSR_2PCS_A")
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT",
                                SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("InputColor", 1)
                        .uniformSamplerTexture("InputDepth", 2)
                        .uniformSamplerTexture("InputVelocity", 3)
                        .uniformStorageTexture("MotionDepthClipAlphaBuffer", 0)
                        .uniformStorageTexture("YCoCgColor", 1)
                        .build());
        convertShader.compile();
        convertPipeline = (ComputePipeline) GlComputePipeline.builder()
                .shader(convertShader)
                .build(RenderSystems.opengl().device());
        upscaleShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.Compute, "/shader/sgsr/2pass_cs/sgsr2_upscale.comp.glsl",
                                true))
                        .name("SGSR_2PCS_B")
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT",
                                SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("PrevHistoryOutput", 7)
                        .uniformSamplerTexture("MotionDepthClipAlphaBuffer", 8)
                        .uniformSamplerTexture("YCoCgColor", 9)
                        .uniformStorageTexture("SceneColorOutput", 0)
                        .uniformStorageTexture("HistoryOutput", 1)
                        .build());
        upscaleShader.compile();
        upscalePipeline = (ComputePipeline) GlComputePipeline.builder()
                .shader(upscaleShader)
                .build(RenderSystems.opengl().device());
        sgsrPipeline = new RenderGrape();
        PrevLumaHistory = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(RenderHandlerManager.getRenderWidth())
                .height(RenderHandlerManager.getRenderHeight())
                .format(TextureFormat.R32UI)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        YCoCgColor = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(RenderHandlerManager.getRenderWidth())
                .height(RenderHandlerManager.getRenderHeight())
                .format(TextureFormat.R32UI)
                .usages(TextureUsages.create().storage().sampler())
                .build());

        MotionDepthClipAlphaBuffer = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(RenderHandlerManager.getRenderWidth())
                .height(RenderHandlerManager.getRenderHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        PrevHistoryOutput = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(RenderHandlerManager.getScreenWidth())
                .height(RenderHandlerManager.getScreenHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        HistoryOutput = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(RenderHandlerManager.getScreenWidth())
                .height(RenderHandlerManager.getScreenHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        sgsrPipeline.add("convert",
                GrapeJobBuilders.compute(convertPipeline)
                        .resource("InputColor",
                                GrapeJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(sgsr.getInputResourceSet().colorTexture())))
                        .resource("InputDepth",
                                GrapeJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(sgsr.getInputResourceSet().depthTexture())))
                        .resource("InputVelocity",
                                GrapeJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(sgsr.getInputResourceSet().motionVectorsTexture())))
                        .resource("MotionDepthClipAlphaBuffer",
                                GrapeJobResource.StorageTexture.create(
                                        MotionDepthClipAlphaBuffer,
                                        GrapeResourceAccess.Write))
                        .resource("YCoCgColor",
                                GrapeJobResource.StorageTexture.create(
                                        YCoCgColor,
                                        GrapeResourceAccess.Write))
                        .resource("Params",
                                GrapeJobResource.UniformBuffer.create(
                                        sgsr.getParams()))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build());

        sgsrPipeline.add("upscale",
                GrapeJobBuilders.compute(upscalePipeline)
                        .resource("PrevHistoryOutput",
                                GrapeJobResource.SamplerTexture.create(
                                        TextureSupplier.of(() -> PrevHistoryOutput)))
                        .resource("MotionDepthClipAlphaBuffer",
                                GrapeJobResource.SamplerTexture.create(
                                        MotionDepthClipAlphaBuffer))
                        .resource("YCoCgColor",
                                GrapeJobResource.SamplerTexture.create(
                                        YCoCgColor))
                        .resource("SceneColorOutput",
                                GrapeJobResource.StorageTexture.create(
                                        FrameBufferTextureAdapter.ofColor(sgsr.getOutputFrameBuffer()),
                                        GrapeResourceAccess.Write))
                        .resource("HistoryOutput",
                                GrapeJobResource.StorageTexture.create(
                                        TextureSupplier.of(() -> HistoryOutput),
                                        GrapeResourceAccess.Write))
                        .resource("Params",
                                GrapeJobResource.UniformBuffer.create(
                                        sgsr.getParams()))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build());
    }

    private void swapHistoryOutput() {
        ITexture tempA = PrevHistoryOutput;
        PrevHistoryOutput = HistoryOutput;
        HistoryOutput = tempA;
    }

    @Override
    public void destroy() {
        HistoryOutput.destroy();
        PrevHistoryOutput.destroy();
        convertShader.destroy();
        upscaleShader.destroy();
        MotionDepthClipAlphaBuffer.destroy();
        YCoCgColor.destroy();

    }

    @Override
    public void resize(int width, int height) {
        HistoryOutput.resize(width, height);
        PrevHistoryOutput.resize(width, height);
        MotionDepthClipAlphaBuffer.resize(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());
        YCoCgColor.resize(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight());
    }
}
