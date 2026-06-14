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

package io.homo.superresolution.shadercompat;


import io.homo.irisapi.ICompositeRendererAccessor;
import io.homo.irisapi.IrisAPI;
import io.homo.irisapi.NamedCompositePass;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.AlgorithmDispatchEvent;
import io.homo.superresolution.api.event.AlgorithmDispatchFinishEvent;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRCompatProcessor;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatTextureInfo;
import io.homo.superresolution.common.minecraft.handler.shadercompat.v2.SRCompatV2Processor;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.InteropResourcesConverter;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.utils.GlTextureCopier;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.minecraft.client.Minecraft;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL41;

import java.util.HashMap;
import java.util.Map;

import static io.homo.superresolution.common.upscale.AlgorithmManager.param;

/**
 * 狗屎一坨，以至于我不得不写注释
 */
public class IrisShaderCompatUpscaleDispatcher {
    public static Map<String, Object> debugInfo = new HashMap<>();

    public static ShaderCompatTextureInfo colorTexture;
    public static ShaderCompatTextureInfo depthTexture;
    public static ShaderCompatTextureInfo motionVectorsTexture;
    public static ShaderCompatTextureInfo exposureTexture;

    private static SRShaderCompatData.InputTexture lastColorConfig;
    private static SRShaderCompatData.InputTexture lastDepthConfig;
    private static SRShaderCompatData.InputTexture lastMotionConfig;
    private static SRShaderCompatData.InputTexture lastExposureConfig;
    private static SRShaderCompatData.OutputTexture lastOutputConfig;


    private static ICompositeRendererAccessor cachedCompositeRenderer;
    private static NamedCompositePass cachedNamedCompositePass;
    private static final Map<String, ITexture> cachedOutputTargetTextures = new HashMap<>();

    private static float resolvePreExposure(SRShaderCompatData.SourceConfig config) {
        if (config == null) {
            return 1.0f;
        }
        if (config.source == SRShaderCompatData.SourceConfig.SourceType.CONST) {
            return ((Number) config.value).floatValue();
        }
        IrisShaderPipelineContext ctx = new IrisShaderPipelineContext(IrisAPI.getIrisRenderingPipeline());
        Object val = config.source == SRShaderCompatData.SourceConfig.SourceType.UNIFORM
                ? ctx.getCustomUniformValue((String) config.value)
                : ctx.getCustomVariableValue((String) config.value);
        if (val instanceof Number n) {
            return n.floatValue();
        }
        return 1.0f;
    }

    public static Vector2f getJitterOffset() {
        if (IrisShaderCompatUtils.getCurrentConfig().isEmpty()) {
            return new Vector2f(0, 0);
        }
        SRShaderCompatData.WorldProfile profile = IrisShaderCompatUtils.getCurrentConfig().get();
        if (profile.jitter.source == SRShaderCompatData.JitterConfig.JitterSource.MOD) {
            Vector2f jitter = AlgorithmManager.getJitterOffset();
            return jitter;
        } else {
            Vector2f jitter = profile.jitter.sourceConfig.getJitterOffset(
                    new IrisShaderPipelineContext(IrisAPI.getIrisRenderingPipeline())
            );
            if (jitter == null) {
                return new Vector2f(0, 0);
            }
            return jitter;
        }
    }

    public static int getJitterSequenceLength() {
        if (IrisShaderCompatUtils.getCurrentConfig().isEmpty()) {
            return 0;
        }
        SRShaderCompatData.WorldProfile profile = IrisShaderCompatUtils.getCurrentConfig().get();
        if (profile.jitter.source == SRShaderCompatData.JitterConfig.JitterSource.MOD) {
            return AlgorithmManager.getJitterSequenceLength();
        } else {
            return profile.jitter.sourceConfig.getJitterSequenceLength(
                    new IrisShaderPipelineContext(IrisAPI.getIrisRenderingPipeline())
            );
        }
    }

    public static DispatchResource getDispatchResource(ICompositeRendererAccessor compositeRenderer,
                                                       float preExposure,
                                                       ITexture resolvedExposureTexture,
                                                       Vector2f adaptedJitter,
                                                       boolean colorPreProcessed,
                                                       boolean depthPreProcessed,
                                                       boolean motionVectorsPreProcessed) {
        ITexture motionVectorsInput = null;
        if (motionVectorsTexture != null && lastMotionConfig != null && lastMotionConfig.enabled && motionVectorsTexture.getSourceTexture() != null) {
            motionVectorsInput = motionVectorsTexture.getAlgorithmTexture(motionVectorsPreProcessed);
        }
        if (motionVectorsInput == null && AlgorithmManager.getMotionVectorsFrameBuffer() != null) {
            motionVectorsInput = AlgorithmManager.getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color);
        }
        return new DispatchResource(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight(),
                new Vector2f(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()),

                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight(),
                new Vector2f(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()),

                RenderHandlerManager.getFrameCount(),
                PerformanceTracker.getLastResultCPU("Frame"),
                (float) param.verticalFov,
                (float) Math.tan(param.verticalFov / 2.0) * RenderHandlerManager.getRenderWidth() / RenderHandlerManager.getRenderHeight(),
                MinecraftUtils.getCameraNear(),
                MinecraftUtils.getCameraFar(),
                adaptedJitter,
                getJitterSequenceLength(),
                param.currentModelViewMatrix,
                param.currentProjectionMatrix,
                param.currentModelViewProjectionMatrix,
                param.currentViewMatrix,

                param.lastModelViewMatrix,
                param.lastProjectionMatrix,
                param.lastModelViewProjectionMatrix,
                param.lastViewMatrix,

                preExposure,

                new InputResourceSet(
                        colorTexture.getAlgorithmTexture(colorPreProcessed),
                        depthTexture.getAlgorithmTexture(depthPreProcessed),
                        motionVectorsInput,
                        resolvedExposureTexture
                )

        );
    }

    private static boolean configEquals(SRShaderCompatData.OutputTexture c1,
                                        SRShaderCompatData.OutputTexture c2) {
        if (c1 == c2) {
            return true;
        }
        if (c1 == null || c2 == null) {
            return false;
        }

        return c1.enabled == c2.enabled &&
                c1.targetNames.equals(c2.targetNames) &&
                c1.region.equals(c2.region);
    }


    private static boolean configEquals(SRShaderCompatData.InputTexture c1,
                                        SRShaderCompatData.InputTexture c2) {
        if (c1 == c2) {
            return true;
        }
        if (c1 == null || c2 == null) {
            return false;
        }

        return c1.enabled == c2.enabled &&
                c1.sourceName.equals(c2.sourceName) &&
                c1.region.equals(c2.region);
    }

    public static void reset(){
        if (colorTexture != null) {
            colorTexture.destroy();
        }
        if (depthTexture != null) {
            depthTexture.destroy();
        }
        if (motionVectorsTexture != null) {
            motionVectorsTexture.destroy();
        }
        if (exposureTexture != null) {
            exposureTexture.destroy();
        }
        colorTexture = null;
        depthTexture = null;
        motionVectorsTexture = null;
        exposureTexture = null;

        lastColorConfig = null;
        lastDepthConfig = null;
        lastMotionConfig = null;
        lastExposureConfig = null;
        lastOutputConfig = null;

        cachedCompositeRenderer = null;
        cachedNamedCompositePass = null;
        cachedOutputTargetTextures.clear();
        IrisShaderCompatEventHandler.failedToDispatchUpscale = false;
        RenderHandlerManager.frameCount = 0;
        InteropResourcesConverter.destroy();
        SRCompatV2Processor.destroyPipelineCache();
    }

    public static void dispatchUpscale(ICompositeRendererAccessor compositeRenderer, NamedCompositePass pass) {
        if (!SuperResolutionConfig.isEnableUpscaleOriginal()) {
            return;
        }
        if (IrisShaderCompatUtils.getCurrentConfig().isEmpty()) {
            return;
        }

        PerformanceTracker.push("Upscale");

        SRShaderCompatData.UpscaleConfig currentConfig = IrisShaderCompatUtils.getCurrentConfig().get().upscale;
        SRShaderCompatData shaderCompatData = IrisShaderCompatUtils.getCurrentShaderPackConfig().orElse(null);
        SRCompatProcessor processor = shaderCompatData != null ? shaderCompatData.getProcessor() : null;
        AbstractAlgorithm algorithm = SuperResolution.getCurrentAlgorithm();
        AlgorithmDescription<?> description = SuperResolution.algorithmDescription;
        boolean needUpdate = false;
        boolean needsPreProcessColor = processor != null && processor.needsPreProcessColor(shaderCompatData, algorithm, description);
        boolean needsPreProcessDepth = processor != null && processor.needsPreProcessDepth(shaderCompatData, algorithm, description);
        boolean needsPreProcessMotionVectors = processor != null && processor.needsPreProcessMotionVectors(shaderCompatData, algorithm, description);
        boolean needsPreProcessExposure = processor != null && processor.needsPreProcessExposure(shaderCompatData, algorithm, description);

        if (compositeRenderer != cachedCompositeRenderer && !compositeRenderer.isSameInstance(cachedCompositeRenderer)) {
            cachedCompositeRenderer = compositeRenderer;
            cachedOutputTargetTextures.clear();
            needUpdate = true;
        }

        if (!pass.equals(cachedNamedCompositePass)) {
            cachedNamedCompositePass = pass;
            cachedOutputTargetTextures.clear();
            needUpdate = true;
        }

        /*
        检查+初始化超分输入配置
        createForInput使用getIrisTexture方法会从Iris拿到纹理然后从纹理ID创建超分自己的ITexture对象
        updateTexture内部会把Iris纹理复制到内部纹理，便于读写（其实只有读）
         */
        SRShaderCompatData.InputTexture colorConfig;
        SRShaderCompatData.InputTexture depthConfig;
        SRShaderCompatData.InputTexture motionConfig;
        SRShaderCompatData.InputTexture exposureConfig;
        SRShaderCompatData.OutputTexture outputConfig;
        {
            colorConfig = currentConfig.inputTextures.get("color");
            depthConfig = currentConfig.inputTextures.get("depth");
            motionConfig = currentConfig.inputTextures.get("motion_vectors");
            exposureConfig = currentConfig.inputTextures.get("exposure");
            outputConfig = currentConfig.outputTextures.get("upscaled_color");
            if (!configEquals(outputConfig, lastOutputConfig)) {
                cachedOutputTargetTextures.clear();
                lastOutputConfig = outputConfig;
            }

            if ((colorTexture == null || !configEquals(colorConfig, lastColorConfig) || needUpdate) && colorConfig != null && colorConfig.enabled) {
                if (colorTexture != null) {
                    colorTexture.destroy();
                }
                colorTexture = IrisTextureConfigResolver.createForInput(compositeRenderer, colorConfig, pass);
                lastColorConfig = colorConfig;
            }
            if ((depthTexture == null || !configEquals(depthConfig, lastDepthConfig) || needUpdate) && depthConfig != null && depthConfig.enabled) {
                if (depthTexture != null) {
                    depthTexture.destroy();
                }
                depthTexture = IrisTextureConfigResolver.createForInput(compositeRenderer, depthConfig, pass);
                lastDepthConfig = depthConfig;
            }
            if ((motionVectorsTexture == null || !configEquals(motionConfig, lastMotionConfig) || needUpdate)) {
                if (motionVectorsTexture != null) {
                    motionVectorsTexture.destroy();
                }
                motionVectorsTexture = IrisTextureConfigResolver.createForInput(compositeRenderer, motionConfig, pass);
                lastMotionConfig = motionConfig;
            }

            if ((exposureTexture == null || !configEquals(exposureConfig, lastExposureConfig) || needUpdate) && exposureConfig != null && exposureConfig.enabled) {
                if (exposureTexture != null) {
                    exposureTexture.destroy();
                }
                exposureTexture = IrisTextureConfigResolver.createForInput(compositeRenderer, exposureConfig, pass);
                lastExposureConfig = exposureConfig;
            }

            GlDebug.pushGroup(64108436, "SRUpscale-CopyInput");
            colorTexture.updateTexture();
            depthTexture.updateTexture();
            if (motionVectorsTexture != null) {
                motionVectorsTexture.updateTexture();
            }
            if (exposureTexture != null) {
                exposureTexture.updateTexture();
            }
            GlDebug.popGroup();
        }

        if (processor != null) {
            ICommandBuffer cb = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
            try {
                cb.begin();
                if (needsPreProcessColor) {
                    ITexture input = colorTexture.getPreProcessInputTexture();
                    ITexture output = colorTexture.getPreProcessOutputTexture();
                    processor.preProcessColor(input, output, cb, shaderCompatData, algorithm, description);
                }
                if (needsPreProcessDepth && depthTexture != null) {
                    ITexture input = depthTexture.getPreProcessInputTexture();
                    ITexture output = depthTexture.getPreProcessOutputTexture();
                    processor.preProcessDepth(input, output, cb, shaderCompatData, algorithm, description);
                }
                if (needsPreProcessMotionVectors && motionVectorsTexture != null) {
                    ITexture input = motionVectorsTexture.getPreProcessInputTexture();
                    ITexture output = motionVectorsTexture.getPreProcessOutputTexture();
                    processor.preProcessMotionVectors(input, output, cb, shaderCompatData, algorithm, description);
                }
                if (needsPreProcessExposure && exposureTexture != null) {
                    ITexture input = exposureTexture.getPreProcessInputTexture();
                    ITexture output = exposureTexture.getPreProcessOutputTexture();
                    processor.preProcessExposure(input, output, cb, shaderCompatData, algorithm, description);
                }
                cb.end();
                RenderSystems.current().device().submitCommandBuffer(cb);
                cb.waitForFence();
            } finally {
                cb.destroy();
            }

        }

        float preExposure = resolvePreExposure(currentConfig.preExposure);
        if (processor != null && processor.needsAdaptPreExposure(shaderCompatData, algorithm, description)) {
            preExposure = processor.adaptPreExposureForAlgorithm(preExposure, algorithm, shaderCompatData, description);
        }

        ITexture rawExposureTexture = (
                exposureConfig != null &&
                        exposureConfig.enabled &&
                        exposureTexture != null &&
                        exposureTexture.getSourceTexture() != null
        ) ? exposureTexture.getAlgorithmTexture(needsPreProcessExposure) : null;

        /*
        升采样阶段开始
         */
        {
            if (RenderHandlerManager.needCaptureUpscale) {
                if (RenderDoc.renderdoc != null) {
                    RenderDoc.renderdoc.StartFrameCapture.call(null, null);
                }
            }
        }
        GlDebug.pushGroup(64108436, "SR Upscale");
        AlgorithmManager.update();
        // MotionVectorsGenerator 已被弃用
        Vector2f rawJitter = getJitterOffset();
        Vector2f adaptedJitter = (processor != null && processor.needsAdaptJitter(shaderCompatData, algorithm, description))
                ? processor.adaptJitterForAlgorithm(rawJitter, algorithm, shaderCompatData, description)
                : rawJitter;
        DispatchResource dispatchResource = getDispatchResource(
                compositeRenderer,
                preExposure,
                rawExposureTexture,
                adaptedJitter,
                needsPreProcessColor,
                needsPreProcessDepth,
                needsPreProcessMotionVectors
        );
        if (SuperResolution.currentAlgorithm != null) {
            SuperResolutionAPI.EVENT_BUS.post(
                    new AlgorithmDispatchEvent(
                            SuperResolution.currentAlgorithm,
                            dispatchResource
                    )
            );
        }
        try (GlState ignored_ = new GlState()) {
            SuperResolution.getCurrentAlgorithm().dispatch(dispatchResource);
        }
        if (SuperResolution.currentAlgorithm != null) {
            SuperResolutionAPI.EVENT_BUS.post(
                    new AlgorithmDispatchFinishEvent(
                            SuperResolution.currentAlgorithm,
                            SuperResolution.currentAlgorithm.getOutputFrameBuffer()
                    )
            );
        }
        GlDebug.popGroup();
        /*
        升采样阶段结束
         */
        GlDebug.pushGroup(64108436, "SRUpscale-CopyResult");
        IFrameBuffer outFbo = SuperResolution.getCurrentAlgorithm().getOutputFrameBuffer();
        if (currentConfig.outputTextures.get("upscaled_color").enabled) {
            for (String targetName : currentConfig.outputTextures.get("upscaled_color").targetNames) {
                ITexture targetTexture = cachedOutputTargetTextures.computeIfAbsent(targetName,
                        name -> IrisTextureResolver.getIrisTexture(
                                compositeRenderer,
                                name,
                                pass,
                                false
                        ));
                ITexture sourceTexture = outFbo.getTexture(FrameBufferAttachmentType.Color);
                if (targetTexture != null && sourceTexture != null) {
                    if (
                            targetTexture.getTextureFormat() != sourceTexture.getTextureFormat()
                    ) {
                        GlTextureCopier.copy(
                                CopyOperation.create()
                                        .src(outFbo.getTexture(FrameBufferAttachmentType.Color))
                                        .dst(targetTexture)
                                        .fromTo(CopyOperation.TextureChannel.A, CopyOperation.TextureChannel.A)
                                        .fromTo(CopyOperation.TextureChannel.R, CopyOperation.TextureChannel.R)
                                        .fromTo(CopyOperation.TextureChannel.G, CopyOperation.TextureChannel.G)
                                        .fromTo(CopyOperation.TextureChannel.B, CopyOperation.TextureChannel.B)
                        );
                    } else {
                        Gl.DSA.copyImageSubData(
                                (int) sourceTexture.handle(),
                                GL41.GL_TEXTURE_2D,
                                0,
                                0,
                                0,
                                0,
                                (int) targetTexture.handle(),
                                GL41.GL_TEXTURE_2D,
                                0,
                                outputConfig.region.getX(),
                                outputConfig.region.getY(),
                                0,
                                outputConfig.region.resolve(RenderHandlerManager.getRenderSize(), RenderHandlerManager.getScreenSize())[2],
                                outputConfig.region.resolve(RenderHandlerManager.getRenderSize(), RenderHandlerManager.getScreenSize())[3],
                                1
                        );
                    }
                }
            }
        }
        GlDebug.popGroup();
        {
            if (RenderHandlerManager.needCaptureUpscale) {
                if (RenderDoc.renderdoc != null) {
                    RenderHandlerManager.needCaptureUpscale = false;
                    RenderDoc.renderdoc.EndFrameCapture.call(null, null);
                }
            }
        }
        PerformanceTracker.pop("Upscale");
    }
}
