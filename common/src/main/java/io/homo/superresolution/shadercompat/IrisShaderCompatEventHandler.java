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
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
import io.homo.superresolution.common.minecraft.handler.shadercompat.UniformRegistrar;
import io.homo.superresolution.common.minecraft.handler.shadercompat.v2.SRCompatV2Processor;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRCompatProcessor;
import io.homo.superresolution.shadercompat.mixin.core.CompositeRendererAccessor;
import io.homo.superresolution.shadercompat.mixin.core.RenderTargetsAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class IrisShaderCompatEventHandler {
    public static boolean failedToDispatchUpscale = false;
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
            SuperResolution.LOGGER.error("执行超分时发生错误", throwable);
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
            if ((event.getCompositeRenderer()).getRenderTargets() != null) {
                if (
                        !(
                                (RenderTargetsAccessor) (
                                        (
                                                event.getCompositeRenderer()
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
            if (!failedToDispatchUpscale){
                SuperResolution.LOGGER.error("执行超分时发生错误", throwable);
                SuperResolution.LOGGER.error("下次错误将不会再打印，直到重载光影");
            }
            failedToDispatchUpscale = true;
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

        SRShaderCompatData config = IrisShaderCompatUtils.getCurrentShaderPackConfig().orElse(null);
        if (config == null) return;
        AbstractAlgorithm algorithm = SuperResolutionAPI.getCurrentAlgorithm();
        AlgorithmDescription<?> description = SuperResolution.algorithmDescription;
        SRCompatProcessor processor = config.getProcessor();
        processor.registerMacros(event::registerMacro, algorithm, description);
    }

    private static void onUniformRegistration(UniformRegistrationEvent event) {
        if (SuperResolutionConfig.isForceDisableShaderCompat()) {
            return;
        }
        SRShaderCompatData config = IrisShaderCompatUtils.getCurrentShaderPackConfig().orElse(null);
        if (config == null) return;

        AbstractAlgorithm algorithm = SuperResolutionAPI.getCurrentAlgorithm();
        AlgorithmDescription<?> description = SuperResolution.algorithmDescription;
        config.getProcessor().registerUniforms(adaptUniforms(event.getUniforms()), config, algorithm, description);
    }

    private static UniformRegistrar adaptUniforms(UniformHolder holder) {
        return new UniformRegistrar() {
            @Override
            public void uniform1f(String name, java.util.function.Supplier<Float> value) {
                holder.uniform1f(UniformUpdateFrequency.PER_FRAME, name, value::get);
            }

            @Override
            public void uniform2f(String name, java.util.function.Supplier<Vector2f> value) {
                holder.uniform2f(UniformUpdateFrequency.PER_FRAME, name, value);
            }

            @Override
            public void uniform1i(String name, java.util.function.Supplier<Integer> value) {
                holder.uniform1i(UniformUpdateFrequency.PER_FRAME, name, value::get);
            }

            @Override
            public void uniform2i(String name, java.util.function.Supplier<Vector2i> value) {
                holder.uniform2i(UniformUpdateFrequency.PER_FRAME, name, value);
            }
        };
    }
}
