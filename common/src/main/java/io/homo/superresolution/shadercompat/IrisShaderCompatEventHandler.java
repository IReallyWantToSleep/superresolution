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

import io.homo.irisapi.IrisAPI;
import io.homo.irisapi.IrisCompositePassRenderingEvent;
import io.homo.irisapi.MacroRegistrationEvent;
import io.homo.irisapi.UniformRegistrationEvent;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import io.homo.superresolution.shadercompat.mixin.core.RenderTargetsAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IrisShaderCompatEventHandler {
    public static void registerEventListeners() {
        IrisAPI.EVENT_BUS.addListener(IrisShaderCompatEventHandler::onMacroRegistration);
        IrisAPI.EVENT_BUS.addListener(IrisShaderCompatEventHandler::onUniformRegistration);
        IrisAPI.EVENT_BUS.addListener(IrisShaderCompatEventHandler::onCompositeRendererRenderAfter);
        IrisAPI.EVENT_BUS.addListener(IrisShaderCompatEventHandler::onCompositeRendererRenderBefore);
    }

    private static void onCompositeRendererRenderAfter(IrisCompositePassRenderingEvent.AfterPassRender event) {
        if (!IrisShaderCompatUtils.shouldApplySuperResolutionChanges()) {
            return;
        }
        if (event.getCompositeRenderer() == null) {
            return;
        }
        if (IrisShaderCompatUtils.getCurrentConfig().isEmpty()) {
            return;
        }
        SRShaderCompatData.WorldProfile config = IrisShaderCompatUtils.getCurrentConfig().get();
        if (!config.enabled || !config.upscale.enabled) {
            return;
        }
        if (Iris.getPipelineManager().getPipeline().isEmpty()) {
            return;
        }
        if (config.upscale.trigger.order != SRShaderCompatData.PipelineTrigger.Order.AFTER) {
            return;
        }
        String targetPassName = config.upscale.trigger.passName;
        String currentPassName = event.getPassName();
        if (!targetPassName.equals(currentPassName)) {
            return;
        }

        try {
            //检查renderTargets是不是null以及是否被销毁，否则1.21.5+会报Tried to use destroyed RenderTargets
            if (((CompositeRendererAccessor) event.getCompositeRenderer()).getRenderTargets() != null) {
                if (
                        !(
                                (RenderTargetsAccessor) (
                                        (
                                                (CompositeRendererAccessor) event.getCompositeRenderer()
                                        )
                                                .getRenderTargets()
                                )
                        ).isDestroyed()) {
                    if (Iris.getPipelineManager().getPipeline().isPresent()) {
                        IrisShaderCompatUpscaleDispatcher.dispatchUpscale(event.getCompositeRenderer(), event.getCompositePass());
                    }
                }
            }
        } catch (Throwable throwable) {
            SuperResolution.LOGGER.error("执行超分时发生错误");
            throwable.printStackTrace();
        }
    }

    private static void onCompositeRendererRenderBefore(IrisCompositePassRenderingEvent.BeforePassRender event) {
        if (!IrisShaderCompatUtils.shouldApplySuperResolutionChanges()) {
            return;
        }
        if (event.getCompositeRenderer() == null) {
            return;
        }
        if (IrisShaderCompatUtils.getCurrentConfig().isEmpty()) {
            return;
        }
        SRShaderCompatData.WorldProfile config = IrisShaderCompatUtils.getCurrentConfig().get();
        if (!config.enabled || !config.upscale.enabled) {
            return;
        }
        if (Iris.getPipelineManager().getPipeline().isEmpty()) {
            return;
        }
        if (config.upscale.trigger.order != SRShaderCompatData.PipelineTrigger.Order.BEFORE) {
            return;
        }
        String targetPassName = config.upscale.trigger.passName;
        String currentPassName = event.getPassName();
        if (!targetPassName.equals(currentPassName)) {
            return;
        }

        try {
            //检查renderTargets是不是null以及是否被销毁，否则1.21.5+会报Tried to use destroyed RenderTargets
            if (((CompositeRendererAccessor) event.getCompositeRenderer()).getRenderTargets() != null) {
                if (
                        !(
                                (RenderTargetsAccessor) (
                                        (
                                                (CompositeRendererAccessor) event.getCompositeRenderer()
                                        )
                                                .getRenderTargets()
                                )
                        ).isDestroyed()) {
                    if (Iris.getPipelineManager().getPipeline().isPresent()) {
                        IrisShaderCompatUpscaleDispatcher.dispatchUpscale(event.getCompositeRenderer(), event.getCompositePass());
                    }
                }
            }
        } catch (Throwable throwable) {
            SuperResolution.LOGGER.error("执行超分时发生错误");
            throwable.printStackTrace();
        }
        setupUniforms();
    }

    private static void setupUniforms() {

    }

    private static void onMacroRegistration(MacroRegistrationEvent event) {
        if (SuperResolutionConfig.isForceDisableShaderCompat()) {
            return;
        }

        RenderHandlerManager.frameCount = 0;

        event.registerMacro("SR_INSTALLED", "1");

        Map<AlgorithmDescription<?>, Integer> idMap = new HashMap<>();
        List<AlgorithmDescription<?>> algorithms = new ArrayList<>(AlgorithmRegistry.getAlgorithmMap().values());

        AlgorithmRegistry.getAlgorithmMap().values().forEach((desc) -> {
            int id = algorithms.indexOf(desc) + 0x546F0;
            idMap.put(desc, id);
            event.registerMacro("SR_ALGO_" + desc.codeName.toUpperCase(), Integer.toString(id));
        });

        if (SuperResolutionConfig.isEnableUpscale()) {
            event.registerMacro("SR_ENABLE", "1");
            event.registerMacro("SR_DISABLE", "0");
            event.registerMacro("SR_ALGO_SUPPORTS_JITTER",
                    SuperResolution.getCurrentAlgorithm().isSupportJitter() ? "1" : "0");
            event.registerMacro("SR_USING_ALGO", Integer.toString(
                    idMap.get(SuperResolutionConfig.getUpscaleAlgorithm())));
            event.registerMacro("SR_SHOULD_APPLY_SCALE", "1");
            event.registerMacro("SR_SHOULD_APPLY_JITTER", "1");
            event.registerMacro("SR_SCALED_WIDTH",
                    Integer.toString(SuperResolutionAPI.getRenderWidth()));
            event.registerMacro("SR_SCALED_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getRenderHeight()));

            event.registerMacro("SR_SCREEN_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            event.registerMacro("SR_SCREEN_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            event.registerMacro("SR_UPSCALE_RATIO",
                    Float.toString(SuperResolutionConfig.getUpscaleRatio()));
            event.registerMacro("SR_RENDER_SCALE_FACTOR",
                    Float.toString(SuperResolutionConfig.getRenderScaleFactor()));
            event.registerMacro("SR_JITTER_SEQUENCE_LENGTH",
                    Integer.toString(AlgorithmManager.getJitterSequenceLength()));
        } else {
            event.registerMacro("SR_ENABLE", "0");
            event.registerMacro("SR_DISABLE", "1");
            event.registerMacro("SR_ALGO_SUPPORTS_JITTER", "0");
            event.registerMacro("SR_USING_ALGO", "0");
            event.registerMacro("SR_SHOULD_APPLY_SCALE", "0");
            event.registerMacro("SR_SHOULD_APPLY_JITTER", "0");
            event.registerMacro("SR_SCALED_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            event.registerMacro("SR_SCALED_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            event.registerMacro("SR_SCREEN_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            event.registerMacro("SR_SCREEN_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            event.registerMacro("SR_UPSCALE_RATIO",
                    Float.toString(1.0f));
            event.registerMacro("SR_RENDER_SCALE_FACTOR",
                    Float.toString(1.0f));
            event.registerMacro("SR_JITTER_SEQUENCE_LENGTH", "0");
        }
    }

    private static void onUniformRegistration(UniformRegistrationEvent event) {
        if (SuperResolutionConfig.isForceDisableShaderCompat()) {
            return;
        }
        if (IrisShaderCompatUtils.getCurrentShaderPackConfig().isEmpty())return;

        UniformHolder uniforms = event.getUniforms();

        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRenderScale",
                () -> SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getRenderScaleFactor() : 1);

        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRatio",
                () -> SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getUpscaleRatio() : 1);

        // 注：这代码改之前因为没讲到对数那一章，误把ln当log2
        // ln(inputWidth/upscaledWidth) / ln(2) = log2(inputWidth/upscaledWidth)
        // 欸嘿嘿嘿嘿换底公式真好玩
        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRenderScaleLog2",
                () -> SuperResolutionConfig.isEnableUpscale()
                        ? (Math.log(((double) SuperResolutionAPI.getRenderWidth() /
                        SuperResolutionAPI.getScreenWidth())) / Math.log(2))
                        : 0);
        uniforms.uniform1i(
                UniformUpdateFrequency.PER_FRAME,
                "SRFrameCount",
                RenderHandlerManager::getFrameCount);

        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SRScaledViewportSize",
                () -> new Vector2f(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()));

        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SROriginalViewportSize",
                () -> new Vector2f(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()));

        uniforms.uniform2i(
                UniformUpdateFrequency.PER_FRAME,
                "SRScaledViewportSizeI",
                () -> new Vector2i(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()));

        uniforms.uniform2i(
                UniformUpdateFrequency.PER_FRAME,
                "SROriginalViewportSizeI",
                () -> new Vector2i(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()));
        if (IrisShaderCompatUtils.getCurrentConfig().isEmpty()) {
            return;
        }
        if (IrisShaderCompatUtils.getCurrentConfig().get().jitter.source == SRShaderCompatData.JitterConfig.JitterSource.SHADERPACK) {
            return;
        }

        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SRJitterOffset",
                () -> {
                    if (SuperResolutionAPI.getCurrentAlgorithm() == null || !SuperResolution.getCurrentAlgorithm().isSupportJitter()) {
                        return new Vector2f(0);
                    }
                    Vector2f jitter = IrisShaderCompatUpscaleDispatcher.getJitterOffset();
                    return new Vector2f(
                            jitter
                    );
                });
        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SRPreviousJitterOffset",
                () -> {
                    if (
                            SuperResolutionAPI.getCurrentAlgorithm() == null || //得有算法
                                    !SuperResolution.getCurrentAlgorithm().isSupportJitter() || //算法得支持抖动
                                    IrisShaderCompatUtils.getCurrentConfig().isEmpty() || //得有配置
                                    IrisShaderCompatUtils.getCurrentConfig().get().jitter.source != SRShaderCompatData.JitterConfig.JitterSource.MOD //配置的抖动源得是MOD
                    ) {
                        return new Vector2f(0);
                    }
                    //我懒，所以只对MOD源的抖动提供上一个帧的偏移，反正SHADERPACK源的抖动它自己管，MOD源的抖动才需要我们提供上一个帧的偏移
                    return new Vector2f(
                            AlgorithmManager.getPreviousJitterOffset()
                    );
                });
    }
}
