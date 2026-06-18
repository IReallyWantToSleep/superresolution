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
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import org.joml.Vector3i;

public class Sgsr3PassCompute extends AbstractSgsrVariant {
    private GlShaderProgram activateShader;
    private GlShaderProgram convertShader;
    private GlShaderProgram upscaleShader;
    private ComputePipeline activatePipeline;
    private ComputePipeline convertPipeline;
    private ComputePipeline upscalePipeline;
    private ITexture PrevLumaHistory;
    private ITexture LumaHistory;
    private ITexture YCoCgColor;

    private ITexture MotionDepthClipAlphaBuffer;
    private ITexture MotionDepthAlphaBuffer;
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
        swapLumaHistory();
        Vector3i wg = getWorkGroupSize();
        convertPipeline.descriptorSet().samplerTexture("InputOpaqueColor", sgsr.getInputResourceSet().colorTexture());
        convertPipeline.descriptorSet().samplerTexture("InputColor", sgsr.getInputResourceSet().colorTexture());
        convertPipeline.descriptorSet().samplerTexture("InputDepth", sgsr.getInputResourceSet().depthTexture());
        convertPipeline.descriptorSet().samplerTexture("InputVelocity", sgsr.getInputResourceSet().motionVectorsTexture());
        if (sgsr.getInputResourceSet().exposureTexture() != null) {
            convertPipeline.descriptorSet().samplerTexture("InputExposure", sgsr.getInputResourceSet().exposureTexture());
        }
        convertPipeline.descriptorSet().storageImage("YCoCgColor", YCoCgColor);
        convertPipeline.descriptorSet().storageImage("MotionDepthAlphaBuffer", MotionDepthAlphaBuffer);
        convertPipeline.descriptorSet().uniformBuffer("Params", sgsr.getParams());
        convertPipeline.descriptorSet().update();

        activatePipeline.descriptorSet().samplerTexture("PrevLumaHistory", PrevLumaHistory);
        activatePipeline.descriptorSet().samplerTexture("MotionDepthAlphaBuffer", MotionDepthAlphaBuffer);
        activatePipeline.descriptorSet().samplerTexture("YCoCgColor", YCoCgColor);
        activatePipeline.descriptorSet().storageImage("MotionDepthClipAlphaBuffer", MotionDepthClipAlphaBuffer);
        activatePipeline.descriptorSet().storageImage("LumaHistory", LumaHistory);
        activatePipeline.descriptorSet().uniformBuffer("Params", sgsr.getParams());
        activatePipeline.descriptorSet().update();

        upscalePipeline.descriptorSet().samplerTexture("PrevHistoryOutput", PrevHistoryOutput);
        upscalePipeline.descriptorSet().samplerTexture("MotionDepthClipAlphaBuffer", MotionDepthClipAlphaBuffer);
        upscalePipeline.descriptorSet().samplerTexture("YCoCgColor", YCoCgColor);
        upscalePipeline.descriptorSet().storageImage("HistoryOutput", HistoryOutput);
        upscalePipeline.descriptorSet().storageImage("SceneColorOutput", FrameBufferTextureAdapter.ofColor(sgsr.getOutputFrameBuffer()));
        upscalePipeline.descriptorSet().uniformBuffer("Params", sgsr.getParams());
        upscalePipeline.descriptorSet().update();
        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        commandBuffer.begin();
        commandBuffer.writeToBuffer(sgsr.getParams(), 0, sgsr.paramsData());
        commandBuffer.bindPipeline(convertPipeline);
        commandBuffer.dispatch(wg.x, wg.y, wg.z);
        commandBuffer.bindPipeline(activatePipeline);
        commandBuffer.dispatch(wg.x, wg.y, wg.z);
        commandBuffer.bindPipeline(upscalePipeline);
        commandBuffer.dispatch(wg.x, wg.y, wg.z);

        commandBuffer.end();
        RenderSystems.current().device().submitCommandBuffer(commandBuffer);
    }

    @Override
    public void init(Sgsr2 sgsr) {
        this.parentSgsr = sgsr;
        activateShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.Compute, "/shader/sgsr/3pass_cs/sgsr2_activate.comp.glsl",
                                true))
                        .name("SGSR_3PCS_A")
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT",
                                SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("PrevLumaHistory", 1)
                        .uniformSamplerTexture("MotionDepthAlphaBuffer", 2)
                        .uniformSamplerTexture("YCoCgColor", 3)
                        .uniformStorageTexture("MotionDepthClipAlphaBuffer", 4)
                        .uniformStorageTexture("LumaHistory", 5)
                        .build());
        activateShader.compile();
        activatePipeline = GlComputePipeline.builder()
                .shader(activateShader)
                .build(RenderSystems.opengl().device());

        convertShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.Compute, "/shader/sgsr/3pass_cs/sgsr2_convert.comp.glsl",
                                true))
                        .name("SGSR_3PCS_B")
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT",
                                SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("InputOpaqueColor", 1)
                        .uniformSamplerTexture("InputColor", 2)
                        .uniformSamplerTexture("InputDepth", 3)
                        .uniformSamplerTexture("InputVelocity", 4)
                        .uniformSamplerTexture("InputExposure", 7)
                        .uniformStorageTexture("YCoCgColor", 5)
                        .uniformStorageTexture("MotionDepthAlphaBuffer", 6)
                        .build());
        convertShader.compile();
        convertPipeline = GlComputePipeline.builder()
                .shader(convertShader)
                .build(RenderSystems.opengl().device());

        upscaleShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.Compute, "/shader/sgsr/3pass_cs/sgsr2_upscale.comp.glsl",
                                true))
                        .name("SGSR_3PCS_C")
                        .addDefine("SR_INTERNAL_TEXTURE_FORMAT",
                                SuperResolutionConfig.getInternalTextureFormatGlslFormatQualifier())
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("PrevHistoryOutput", 1)
                        .uniformSamplerTexture("MotionDepthClipAlphaBuffer", 2)
                        .uniformSamplerTexture("YCoCgColor", 3)
                        .uniformStorageTexture("HistoryOutput", 4)
                        .uniformStorageTexture("SceneColorOutput", 5)
                        .build());
        upscaleShader.compile();
        upscalePipeline = GlComputePipeline.builder()
                .shader(upscaleShader)
                .build(RenderSystems.opengl().device());

        PrevLumaHistory = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(RenderHandlerManager.getRenderWidth())
                .height(RenderHandlerManager.getRenderHeight())
                .format(TextureFormat.R32UI)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        LumaHistory = RenderSystems.current().device().createTexture(TextureDescription.create()
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
        MotionDepthAlphaBuffer = RenderSystems.current().device().createTexture(TextureDescription.create()
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
    }

    private void swapHistoryOutput() {
        ITexture tempA = PrevHistoryOutput;
        PrevHistoryOutput = HistoryOutput;
        HistoryOutput = tempA;
    }

    private void swapLumaHistory() {
        ITexture tempA = PrevLumaHistory;
        PrevLumaHistory = LumaHistory;
        LumaHistory = tempA;
    }

    @Override
    public void destroy() {
        HistoryOutput.destroy();
        PrevHistoryOutput.destroy();
        activateShader.destroy();
        convertShader.destroy();
        upscaleShader.destroy();
        MotionDepthClipAlphaBuffer.destroy();
        MotionDepthAlphaBuffer.destroy();
        PrevLumaHistory.destroy();
        LumaHistory.destroy();
        YCoCgColor.destroy();
    }
}
