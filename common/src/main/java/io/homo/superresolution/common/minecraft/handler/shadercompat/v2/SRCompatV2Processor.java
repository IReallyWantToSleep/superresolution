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

package io.homo.superresolution.common.minecraft.handler.shadercompat.v2;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.utils.FileReadHelper;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.DLSSRenderPreset;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.MacroRegistrar;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRCompatProcessor;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
import io.homo.superresolution.common.minecraft.handler.shadercompat.UniformRegistrar;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.shadercompat.IrisShaderCompatUpscaleDispatcher;
import io.homo.superresolution.shadercompat.IrisShaderCompatUtils;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.*;

public class SRCompatV2Processor implements SRCompatProcessor {
    private static final Map<String, ComputePipeline> mvPreprocessPipelineCache = new HashMap<>();

    @Override
    public int version() {
        return 2;
    }

    @Override
    public boolean needsPreProcessColor(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        return false;
    }

    @Override
    public boolean needsPreProcessDepth(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        return false;
    }

    @Override
    public boolean needsPreProcessMotionVectors(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        if (
                description.equals(AlgorithmDescriptions.FSR) ||
                        description.equals(AlgorithmDescriptions.DLSS) ||
                        description.equals(AlgorithmDescriptions.XESS)
        ) {
            return false;
        }else {
            return IrisShaderCompatUtils.getCurrentConfig()
                    .map(p -> p.upscale.customs != null && p.upscale.customs.motionVectorPreprocessingFunction != null)
                    .orElse(false);
        }

    }

    @Override
    public boolean needsPreProcessExposure(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        return false;
    }

    @Override
    public boolean needsAdaptJitter(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        return true;
    }

    @Override
    public boolean needsAdaptPreExposure(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        return false;
    }

    @Override
    public Vector2f adaptJitterForAlgorithm(Vector2f rawJitter, AbstractAlgorithm algorithm, SRShaderCompatData config, AlgorithmDescription<?> description) {
        return rawJitter;
    }

    @Override
    public Vector2f adaptJitterForShaderpack(Vector2f rawJitter, AbstractAlgorithm algorithm, SRShaderCompatData config, AlgorithmDescription<?> description) {
        if (
                description.equals(AlgorithmDescriptions.FSR) ||
                        description.equals(AlgorithmDescriptions.DLSS) ||
                        description.equals(AlgorithmDescriptions.XESS)
        ) {
            return rawJitter.mul(1, -1);
        }else {
            return rawJitter;
        }
    }

    @Override
    public void preProcessColor(ITexture input, ITexture output, ICommandBuffer commandBuffer,
                                SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
    }

    @Override
    public void preProcessDepth(ITexture input, ITexture output, ICommandBuffer commandBuffer,
                                SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
    }

    @Override
    public void preProcessMotionVectors(ITexture input, ITexture output, ICommandBuffer commandBuffer,
                                        SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        String function = IrisShaderCompatUtils.getCurrentConfig()
                .map(p -> p.upscale.customs != null ? p.upscale.customs.motionVectorPreprocessingFunction : null)
                .orElse(null);
        if (function == null) return;

        ComputePipeline pipeline = getOrCreateMVPreprocessPipeline(function);
        pipeline.descriptorSet().samplerTexture("inputMotionVectors", input);
        pipeline.descriptorSet().storageImage("outputMotionVectors", output);
        pipeline.descriptorSet().update();
        commandBuffer.bindPipeline(pipeline);
        commandBuffer.dispatch(
                (input.getWidth() + 15) / 16,
                (input.getHeight() + 15) / 16,
                1
        );
    }

    private static ComputePipeline getOrCreateMVPreprocessPipeline(String function) {
        ComputePipeline cached = mvPreprocessPipelineCache.get(function);
        if (cached != null) return cached;

        ArrayList<String> sourceLines = FileReadHelper.readText("/shader/interop/motion_vector_preprocess.comp.glsl");
        String templateSource = String.join("\n", sourceLines);
        String modifiedSource = templateSource.replace(
                "// MOTION_VECTOR_PREPROCESSING_FUNCTION_PLACEHOLDER",
                function);

        ShaderSource shaderSource = new ShaderSource(ShaderType.Compute, modifiedSource, false);
        ShaderDescription shaderDesc = ShaderDescription.create()
                .compute(shaderSource)
                .name("mv_preprocess")
                .addDefine("MOTION_VECTOR_PREPROCESSING_FUNCTION_INJECTED", "1")
                .uniformSamplerTexture("inputMotionVectors", 0)
                .uniformStorageTexture("outputMotionVectors", 1)
                .build();

        IShaderProgram shader = RenderSystems.current().device().createShaderProgram(shaderDesc);
        shader.compile();
        ComputePipeline pipeline = ComputePipeline.builder()
                .shader(shader)
                .build(RenderSystems.current().device());
        mvPreprocessPipelineCache.put(function, pipeline);
        return pipeline;
    }

    @Override
    public void preProcessExposure(ITexture input, ITexture output, ICommandBuffer commandBuffer,
                                    SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
    }

    @Override
    public float adaptPreExposureForAlgorithm(float rawExposure, AbstractAlgorithm algorithm, SRShaderCompatData config, AlgorithmDescription<?> description) {
        return rawExposure;
    }


    @Override
    public void registerMacros(MacroRegistrar r, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        // region V1
        r.registerMacro("SR_INSTALLED", "1");

        Map<AlgorithmDescription<?>, Integer> idMap = new HashMap<>();
        List<AlgorithmDescription<?>> algorithms = new ArrayList<>(AlgorithmRegistry.getAlgorithmMap().values());

        AlgorithmRegistry.getAlgorithmMap().values().forEach((desc) -> {
            int id = algorithms.indexOf(desc) + 0x546F0;
            idMap.put(desc, id);
            r.registerMacro("SR_ALGO_" + desc.codeName.toUpperCase(), Integer.toString(id));
        });

        Arrays.stream(DLSSRenderPreset.values()).toList().forEach((preset) -> {
            r.registerMacro("SR_ALGO_DLSS_RENDERPRESET_" + preset.toString(), Integer.toString(preset.getCode()));
        });

        if (SuperResolutionConfig.isEnableUpscaleOriginal()) {
            AlgorithmDescription<?> selectedAlgorithm = description != null
                    ? description
                    : SuperResolutionConfig.getUpscaleAlgorithm();
            r.registerMacro("SR_ENABLE", "1");
            r.registerMacro("SR_DISABLE", "0");
            r.registerMacro("SR_ALGO_SUPPORTS_JITTER",
                    AlgorithmManager.supportsJitter(selectedAlgorithm) ? "1" : "0");
            r.registerMacro("SR_USING_ALGO", Integer.toString(
                    idMap.get(selectedAlgorithm)));
            r.registerMacro("SR_SHOULD_APPLY_SCALE", "1");
            r.registerMacro("SR_SHOULD_APPLY_JITTER", "1");
            r.registerMacro("SR_SCALED_WIDTH",
                    Integer.toString(SuperResolutionAPI.getRenderWidth()));
            r.registerMacro("SR_SCALED_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getRenderHeight()));
            r.registerMacro("SR_SCREEN_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            r.registerMacro("SR_SCREEN_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            r.registerMacro("SR_UPSCALE_RATIO",
                    Float.toString(SuperResolutionConfig.getUpscaleRatio()));
            r.registerMacro("SR_RENDER_SCALE_FACTOR",
                    Float.toString(SuperResolutionConfig.getRenderScaleFactor()));

            r.registerMacro("SR_JITTER_SEQUENCE_LENGTH",
                    Integer.toString(AlgorithmManager.getConfiguredJitterSequenceLength()));
            r.registerMacro("SR_ALGO_DLSS_RENDERPRESET",
                    selectedAlgorithm.equals(AlgorithmDescriptions.DLSS) ?
                            Integer.toString(SuperResolutionConfig.SPECIAL.DLSS.RENDER_PRESET.get().getCode()) :
                            "0");
        } else {
            r.registerMacro("SR_ENABLE", "0");
            r.registerMacro("SR_DISABLE", "1");
            r.registerMacro("SR_ALGO_SUPPORTS_JITTER", "0");
            r.registerMacro("SR_USING_ALGO", "0");
            r.registerMacro("SR_SHOULD_APPLY_SCALE", "0");
            r.registerMacro("SR_SHOULD_APPLY_JITTER", "0");
            r.registerMacro("SR_SCALED_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            r.registerMacro("SR_SCALED_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            r.registerMacro("SR_SCREEN_WIDTH",
                    Integer.toString(SuperResolutionAPI.getScreenWidth()));
            r.registerMacro("SR_SCREEN_HEIGHT",
                    Integer.toString(SuperResolutionAPI.getScreenHeight()));
            r.registerMacro("SR_UPSCALE_RATIO",
                    Float.toString(1.0f));
            r.registerMacro("SR_RENDER_SCALE_FACTOR",
                    Float.toString(1.0f));
            r.registerMacro("SR_JITTER_SEQUENCE_LENGTH", "0");
        }
        // endregion

        // region V2
        r.registerMacro("SR_CONFIG_SCHEMA_VERSION","2");

        if (SuperResolutionConfig.isEnableUpscaleOriginal()) {
            r.registerMacro("SR_UPSCALE_RATIO_HALF",
                    Float.toString(SuperResolutionConfig.getUpscaleRatio() * 0.5F));
            r.registerMacro("SR_RENDER_SCALE_FACTOR_HALF",
                    Float.toString(SuperResolutionConfig.getRenderScaleFactor() * 0.5F));
        } else {
            r.registerMacro("SR_UPSCALE_RATIO_HALF",
                    Float.toString(0.5F));
            r.registerMacro("SR_RENDER_SCALE_FACTOR_HALF",
                    Float.toString(0.5F));
        }
        // endregion
    }

    @Override
    public void registerUniforms(UniformRegistrar r, SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description) {
        // region V1
        r.uniform1f("SRRenderScale",
                () -> SuperResolutionConfig.isEnableUpscaleOriginal() ? SuperResolutionConfig.getRenderScaleFactor() : 1);
        r.uniform1f("SRRatio",
                () -> SuperResolutionConfig.isEnableUpscaleOriginal() ? SuperResolutionConfig.getUpscaleRatio() : 1);
        r.uniform1f("SRRenderScaleLog2",
                () -> SuperResolutionConfig.isEnableUpscaleOriginal()
                        ? (float) (Math.log((double) SuperResolutionAPI.getRenderWidth() /
                        SuperResolutionAPI.getScreenWidth()) / Math.log(2))
                        : 0);
        r.uniform1i("SRFrameCount", RenderHandlerManager::getFrameCount);
        r.uniform2f("SRScaledViewportSize",
                () -> new Vector2f(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()));
        r.uniform2f("SROriginalViewportSize",
                () -> new Vector2f(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()));
        r.uniform2i("SRScaledViewportSizeI",
                () -> new Vector2i(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()));
        r.uniform2i("SROriginalViewportSizeI",
                () -> new Vector2i(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()));

        Optional<SRShaderCompatData.WorldProfile> currentConfig = IrisShaderCompatUtils.getCurrentConfig();
        if (!(currentConfig.isEmpty() || currentConfig.get().jitter.source == SRShaderCompatData.JitterConfig.JitterSource.SHADERPACK)) {
            r.uniform2f("SRJitterOffset",
                    () -> {
                        if (algorithm == null || !algorithm.isSupportJitter()) {
                            return new Vector2f(0);
                        }
                        Vector2f rawJitter = IrisShaderCompatUpscaleDispatcher.getJitterOffset();
                        return adaptJitterForShaderpack(rawJitter, algorithm, config, description);
                    });
            r.uniform2f("SRPreviousJitterOffset",
                    () -> {
                        if (algorithm == null
                                || !algorithm.isSupportJitter()
                                || IrisShaderCompatUtils.getCurrentConfig().isEmpty()
                                || IrisShaderCompatUtils.getCurrentConfig().get().jitter.source != SRShaderCompatData.JitterConfig.JitterSource.MOD) {
                            return new Vector2f(0);
                        }
                        return new Vector2f(AlgorithmManager.getPreviousJitterOffset());
                    });
        }
        // endregion
    }
}
