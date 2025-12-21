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

package io.homo.superresolution.shadercompat;

import io.homo.irisapi.IrisAPI;
import io.homo.irisapi.MacroRegistrationEvent;
import io.homo.irisapi.UniformRegistrationEvent;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.helpers.StringPair;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShaderCompatEventHandler {

    public static void registerEventListeners() {
        IrisAPI.EVENT_BUS.addListener(ShaderCompatEventHandler::onMacroRegistration);
        IrisAPI.EVENT_BUS.addListener(ShaderCompatEventHandler::onUniformRegistration);
    }

    private static void onMacroRegistration(MacroRegistrationEvent event) {
        if (SuperResolutionConfig.isForceDisableShaderCompat()) {
            return;
        }

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
            event.registerMacro("SR_SUPPORTS_JITTER", SuperResolution.getCurrentAlgorithm().isSupportJitter() ? "1" : "0");
            event.registerMacro("SR_USING_ALGO", Integer.toString(
                    idMap.get(SuperResolutionConfig.getUpscaleAlgorithm())
            ));
        } else {
            event.registerMacro("SR_ENABLE", "0");
            event.registerMacro("SR_DISABLE", "1");
            event.registerMacro("SR_SUPPORTS_JITTER", "0");
            event.registerMacro("SR_USING_ALGO", "0");
        }
    }

    private static void onUniformRegistration(UniformRegistrationEvent event) {
        if (SuperResolutionConfig.isForceDisableShaderCompat()) {
            return;
        }

        var uniforms = event.getUniforms();

        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRenderScale",
                () -> SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getRenderScaleFactor() : 1
        );

        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRatio",
                () -> SuperResolutionConfig.isEnableUpscale() ? SuperResolutionConfig.getUpscaleRatio() : 1
        );

        //注：这代码改之前因为没讲到对数那一章，误把ln当log2
        //ln(inputWidth/upscaledWidth) / ln(2) = log2(inputWidth/upscaledWidth)
        //欸嘿嘿嘿嘿换底公式真好玩
        uniforms.uniform1f(
                UniformUpdateFrequency.PER_FRAME,
                "SRRenderScaleLog2",
                () -> SuperResolutionConfig.isEnableUpscale() ? (Math.log((
                        (double) SuperResolutionAPI.getRenderWidth() /
                                SuperResolutionAPI.getScreenWidth()
                )) / Math.log(2)) : 0
        );

        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SRScaledViewportSize",
                () -> new Vector2f(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight())
        );

        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SROriginalViewportSize",
                () -> new Vector2f(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight())
        );

        uniforms.uniform2i(
                UniformUpdateFrequency.PER_FRAME,
                "SRScaledViewportSizeI",
                () -> new Vector2i(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight())
        );

        uniforms.uniform2i(
                UniformUpdateFrequency.PER_FRAME,
                "SROriginalViewportSizeI",
                () -> new Vector2i(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight())
        );

        uniforms.uniform2f(
                UniformUpdateFrequency.PER_FRAME,
                "SRJitterOffset",
                () -> {
                    if (!SuperResolution.getCurrentAlgorithm().isSupportJitter()) return new Vector2f(0);
                    Vector2f jitterOffset = AlgorithmManager.getJitterOffset();
                    return new Vector2f(jitterOffset.x, jitterOffset.y);
                }
        );
    }
}
