/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.common.minecraft.handler.shadercompat;

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.DLSSRenderPreset;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.shadercompat.IrisShaderCompatUtils;

import java.util.*;

public class SRCompatBuiltinMacros {

    public static void addMacros(JsonMacroPreprocessor preprocessor) {
        // region V1
        preprocessor.addMacro("SR_INSTALLED", "1");

        Map<AlgorithmDescription<?>, Integer> idMap = new HashMap<>();
        List<AlgorithmDescription<?>> algorithms = new ArrayList<>(AlgorithmRegistry.getAlgorithmMap().values());

        AlgorithmRegistry.getAlgorithmMap().values().forEach((desc) -> {
            int id = algorithms.indexOf(desc) + 0x546F0;
            idMap.put(desc, id);
            preprocessor.addMacro("SR_ALGO_" + desc.codeName.toUpperCase(), Integer.toString(id));
        });

        Arrays.stream(DLSSRenderPreset.values()).toList().forEach((preset) -> {
            preprocessor.addMacro("SR_ALGO_DLSS_RENDERPRESET_" + preset.toString(), Integer.toString(preset.getCode()));
        });

        AlgorithmDescription<?> description = SuperResolution.algorithmDescription;
        AlgorithmDescription<?> selectedAlgorithm = description != null
                ? description
                : SuperResolutionConfig.getUpscaleAlgorithm();
        boolean frameGenOnly = SuperResolutionConfig.isEnableUpscaleOriginal()
                && IrisShaderCompatUtils.isFrameGenerationOnlySupported()
                && AlgorithmDescriptions.NONE.equals(selectedAlgorithm);

        if (SuperResolutionConfig.isEnableUpscaleOriginal()) {
            preprocessor.addMacro("SR_ENABLE", "1");
            preprocessor.addMacro("SR_DISABLE", "0");
            preprocessor.addMacro("SR_ALGO_SUPPORTS_JITTER",
                    !frameGenOnly && AlgorithmManager.supportsJitter(selectedAlgorithm) ? "1" : "0");
            preprocessor.addMacro("SR_USING_ALGO", Integer.toString(idMap.get(selectedAlgorithm)));
            preprocessor.addMacro("SR_SHOULD_APPLY_SCALE", frameGenOnly ? "0" : "1");
            preprocessor.addMacro("SR_SHOULD_APPLY_JITTER", frameGenOnly ? "0" : "1");
            preprocessor.addMacro("SR_SCALED_WIDTH",
                    Integer.toString(frameGenOnly ? SuperResolutionAPI.getScreenWidth() : SuperResolutionAPI.getRenderWidth()));
            preprocessor.addMacro("SR_SCALED_HEIGHT",
                    Integer.toString(frameGenOnly ? SuperResolutionAPI.getScreenHeight() : SuperResolutionAPI.getRenderHeight()));
            preprocessor.addMacro("SR_SCREEN_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            preprocessor.addMacro("SR_SCREEN_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            preprocessor.addMacro("SR_UPSCALE_RATIO",
                    Float.toString(frameGenOnly ? 1.0f : SuperResolutionConfig.getUpscaleRatio()));
            preprocessor.addMacro("SR_RENDER_SCALE_FACTOR",
                    Float.toString(frameGenOnly ? 1.0f : SuperResolutionConfig.getRenderScaleFactor()));
            preprocessor.addMacro("SR_JITTER_SEQUENCE_LENGTH",
                    frameGenOnly ? "0" : Integer.toString(AlgorithmManager.getConfiguredJitterSequenceLength()));
            preprocessor.addMacro("SR_ALGO_DLSS_RENDERPRESET",
                    selectedAlgorithm.equals(AlgorithmDescriptions.DLSS) ?
                            Integer.toString(SuperResolutionConfig.SPECIAL.DLSS.RENDER_PRESET.get().getCode()) :
                            "0");
        } else {
            preprocessor.addMacro("SR_ENABLE", "0");
            preprocessor.addMacro("SR_DISABLE", "1");
            preprocessor.addMacro("SR_ALGO_SUPPORTS_JITTER", "0");
            preprocessor.addMacro("SR_USING_ALGO", "0");
            preprocessor.addMacro("SR_SHOULD_APPLY_SCALE", "0");
            preprocessor.addMacro("SR_SHOULD_APPLY_JITTER", "0");
            preprocessor.addMacro("SR_SCALED_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            preprocessor.addMacro("SR_SCALED_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            preprocessor.addMacro("SR_SCREEN_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            preprocessor.addMacro("SR_SCREEN_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            preprocessor.addMacro("SR_UPSCALE_RATIO", Float.toString(1.0f));
            preprocessor.addMacro("SR_RENDER_SCALE_FACTOR", Float.toString(1.0f));
            preprocessor.addMacro("SR_JITTER_SEQUENCE_LENGTH", "0");
        }
        // endregion

        // region V2
        if (SuperResolutionConfig.isEnableUpscaleOriginal()) {
            float rsf = frameGenOnly ? 1.0f : SuperResolutionConfig.getRenderScaleFactor();
            float ratio = frameGenOnly ? 1.0f : SuperResolutionConfig.getUpscaleRatio();
            preprocessor.addMacro("SR_UPSCALE_RATIO_HALF",
                    Float.toString(ratio * 0.5F));
            preprocessor.addMacro("SR_RENDER_SCALE_FACTOR_HALF",
                    Float.toString(rsf * 0.5F));
        } else {
            preprocessor.addMacro("SR_UPSCALE_RATIO_HALF", Float.toString(0.5F));
            preprocessor.addMacro("SR_RENDER_SCALE_FACTOR_HALF", Float.toString(0.5F));
        }
        // endregion

        // region V3
        preprocessor.addMacro("SR_CONFIG_SCHEMA_VERSION", Integer.toString(SRCompatConfigParser.LATEST_CONFIG_VERSION));
        // endregion
    }
}
