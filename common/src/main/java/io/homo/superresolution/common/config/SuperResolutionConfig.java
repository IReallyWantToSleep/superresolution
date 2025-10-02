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

package io.homo.superresolution.common.config;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.config.*;
import io.homo.superresolution.api.config.values.list.StringListValue;
import io.homo.superresolution.api.config.values.single.BooleanValue;
import io.homo.superresolution.api.config.values.single.EnumValue;
import io.homo.superresolution.api.config.values.single.FloatValue;
import io.homo.superresolution.api.config.values.single.StringValue;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.enums.InternalTextureFormat;
import io.homo.superresolution.common.config.special.SpecialConfigs;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.SRShaderCompatConfig;
import io.homo.superresolution.common.minecraft.handler.ShaderCompatHandler;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.core.graphics.GpuVendor;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SuperResolutionConfig {
    public static final ModConfigSpec SPEC;
    public static final SpecialConfigs SPECIAL;
    public static final BooleanValue ENABLE_UPSCALE;
    public static final FloatValue UPSCALE_RATIO;
    public static final StringValue UPSCALE_ALGO;
    public static final FloatValue SHARPNESS;
    public static final EnumValue<CaptureMode> CAPTURE_MODE;
    public static final BooleanValue DEBUG_DUMP_SHADER;
    public static final BooleanValue SKIP_INIT_VULKAN;
    public static final BooleanValue ENABLE_RENDER_DOC;
    public static final BooleanValue ENABLE_IMGUI;
    public static final BooleanValue GENERATE_MOTION_VECTORS;
    public static final BooleanValue PAUSE_GAME_ON_GUI;
    public static final StringListValue INJECT_POST_CHAIN_BLACKLIST;
    public static final BooleanValue ENABLE_COMPAT_SHADER_COMPILER;
    public static final BooleanValue ENABLE_DATASET_GENERATOR;
    public static final StringValue DATASET_PATH;
    public static final BooleanValue ENABLE_DETAILED_PROFILING;
    public static final BooleanValue ENABLE_DEBUG;
    public static final BooleanValue DISABLE_UPSCALE_ON_VANILLA;
    public static final BooleanValue FORCE_DISABLE_SHADER_COMPAT;
    public static final EnumValue<InternalTextureFormat> INTERNAL_TEXTURE_FORMAT;
    public static final OSType CURRENT_OS_TYPE = new OS().type;
    public static final Runnable resolutionChangeCallback;

    static {
        ModConfigSpecBuilder builder = new ModConfigSpecBuilder();

        Supplier<String> defaultAlgoSupplier = () -> getDefaultAlgorithm().codeName;

        ENABLE_UPSCALE = builder.defineBoolean(
                "enable_upscale",
                () -> true,
                "Enable super-resolution upscaling"
        );

        UPSCALE_RATIO = builder.defineFloat(
                "upscale_ratio",
                () -> 1.7f,
                "Upscale ratio factor",
                value -> value >= 0.5f && value <= 4.0f
        );

        UPSCALE_ALGO = builder.defineString(
                "upscale_algo",
                defaultAlgoSupplier,
                "Algorithm used for upscaling",
                value -> {
                    if (value == null) return false;
                    AlgorithmDescription<?> algo = AlgorithmRegistry.getDescriptionByID(value);
                    return algo != null;
                }
        );

        SHARPNESS = builder.defineFloat(
                "sharpness",
                () -> 0.55f,
                "Sharpness adjustment factor",
                value -> value >= 0.0f && value <= 1.0f
        );

        CAPTURE_MODE = builder.defineEnum(
                "capture_mode",
                CaptureMode.class,
                () -> CaptureMode.A,
                "Screen capture mode"
        );

        PAUSE_GAME_ON_GUI = builder.defineBoolean(
                "pause_game_on_gui",
                () -> false,
                "Pause game when GUI is open"
        );

        INJECT_POST_CHAIN_BLACKLIST = builder.defineStringList(
                "inject_post_chain_blacklist",
                ArrayList::new,
                "List of post-processing chains to skip injection",
                value -> value != null && !value.isEmpty()
        );

        DEBUG_DUMP_SHADER = builder.defineBoolean(
                "debug/debug_dump_shader",
                () -> false,
                "Dump shaders for debugging purposes"
        );

        SKIP_INIT_VULKAN = builder.defineBoolean(
                "debug/skip_init_vulkan",
                () -> !(CURRENT_OS_TYPE == OSType.ANDROID || CURRENT_OS_TYPE == OSType.MACOS),
                "Skip Vulkan initialization (auto-set based on OS)"
        );

        ENABLE_RENDER_DOC = builder.defineBoolean(
                "debug/enable_render_doc",
                () -> (CURRENT_OS_TYPE == OSType.WINDOWS || CURRENT_OS_TYPE == OSType.LINUX) && Platform.currentPlatform.isDevelopmentEnvironment(),
                "Enable RenderDoc integration (auto-disabled on incompatible OS)"
        );

        ENABLE_IMGUI = builder.defineBoolean(
                "debug/enable_imgui",
                () -> (CURRENT_OS_TYPE == OSType.WINDOWS || CURRENT_OS_TYPE == OSType.LINUX) && Platform.currentPlatform.isDevelopmentEnvironment(),
                "Enable ImGui debug interface (auto-disabled on incompatible OS)"
        );

        ENABLE_DEBUG = builder.defineBoolean(
                "debug/enable_debug",
                () -> false,
                "Enable debug mode"
        );

        GENERATE_MOTION_VECTORS = builder.defineBoolean(
                "experiment/generate_motion_vectors",
                () -> false,
                "Generate motion vectors for advanced effects"
        );

        ENABLE_COMPAT_SHADER_COMPILER = builder.defineBoolean(
                "compat_shader_compiler",
                () -> {
                    try {
                        if (GL.getCapabilities() == null) return false;
                    } catch (Exception e) {
                        return false;
                    }
                    return RenderSystem.isOnRenderThread() ? (
                            GraphicsCapabilities.detectGpuVendor() == GpuVendor.Intel ||
                                    !GraphicsCapabilities.hasGLExtension("GL_ARB_gl_spirv") ||
                                    (GraphicsCapabilities.getGLVersion()[0] >= 4 && GraphicsCapabilities.getGLVersion()[1] < 2)
                    ) : false;
                },
                "This option enables the use of a compatibility shader compiler for compiling shaders when set to true."
        );

        ENABLE_DATASET_GENERATOR = builder.defineBoolean(
                "dataset/enable_dataset_generator",
                () -> false,
                ""
        );
        DATASET_PATH = builder.defineString(
                "dataset/dataset_path",
                () -> "msrDataset",
                ""
        );
        ENABLE_DETAILED_PROFILING = builder.defineBoolean(
                "debug/enable_detailed_profiling",
                () -> false,
                "Enable more detailed performance profiling for advanced analysis."
        );
        FORCE_DISABLE_SHADER_COMPAT = builder.defineBoolean(
                "force_disable_shader_compat",
                () -> false,
                "Force disable shader pack compatibility mode."
        );
        DISABLE_UPSCALE_ON_VANILLA = builder.defineBoolean(
                "disable_upscale_on_vanilla",
                () -> false,
                "Disable Super Resolution when using vanilla rendering."
        );

        INTERNAL_TEXTURE_FORMAT = builder.defineEnum(
                "internal_texture_format",
                InternalTextureFormat.class,
                () -> InternalTextureFormat.R11B11G10F,
                "The precision of the internal texture format affects video memory consumption: higher precision results in greater consumption, while lower precision leads to smaller consumption. Note: Excessively low precision may cause noticeable color banding in the image."
        );

        SPECIAL = new SpecialConfigs(builder);
        Path configPath = Platform.currentPlatform
                .getGameFolder()
                .resolve("config")
                .resolve("super_resolution.toml");
        builder.configPath(configPath);
        SPEC = builder.build();
        resolutionChangeCallback = () -> {
            RenderHandlerManager.resize();
            SuperResolution.getInstance().resize(
                    RenderHandlerManager.getScreenWidth(),
                    RenderHandlerManager.getScreenHeight()
            );
            Minecraft.getInstance().gameRenderer.resize(
                    RenderHandlerManager.getScreenWidth(),
                    RenderHandlerManager.getScreenHeight()
            );
        };
    }

    public static AlgorithmDescription<?> getDefaultAlgorithm() {
        try {
            GL.getCapabilities();
        } catch (Exception e) {
            return AlgorithmDescriptions.SGSR1;
        }
        for (AlgorithmDescription<?> algorithmDescription : AlgorithmRegistry.getAlgorithmMap().values()) {
            if (algorithmDescription.requirement.check().support()) {
                return algorithmDescription;
            }
        }

        SuperResolution.LOGGER.info("你的硬件不支持所有算法????"); //最逆天的一集
        return AlgorithmDescriptions.NONE;
    }

    public static float getRenderScaleFactor() {
        return ENABLE_UPSCALE.get() ? 1 / UPSCALE_RATIO.get() : 1;
    }

    public static AlgorithmDescription<?> getUpscaleAlgorithm() {
        String algoName = UPSCALE_ALGO.get();
        AlgorithmDescription<?> algo = AlgorithmRegistry.getDescriptionByID(algoName);

        if (algo == null) {
            algo = getDefaultAlgorithm();
            UPSCALE_ALGO.set(algo.codeName);
        }

        if (!algo.requirement.check().support() && !Platform.currentPlatform.isDevelopmentEnvironment()) {
            SuperResolution.LOGGER.warn("算法 {} 不支持，回退到默认算法", algo.displayName);
            AlgorithmDescription<?> defaultAlgo = getDefaultAlgorithm();
            setUpscaleAlgorithm(defaultAlgo);
            return defaultAlgo;
        }

        return algo;
    }

    public static synchronized void setUpscaleAlgorithm(AlgorithmDescription<?> newAlgo) {
        if (newAlgo == null) {
            newAlgo = getDefaultAlgorithm();
        }

        String algoName = UPSCALE_ALGO.get();
        AlgorithmDescription<?> currentAlgo = AlgorithmRegistry.getDescriptionByID(algoName);

        if (currentAlgo == newAlgo) return;

        AbstractAlgorithm oldAlgorithmInstance = SuperResolution.currentAlgorithm;
        AlgorithmDescription<?> oldDescription = SuperResolution.algorithmDescription;

        try {
            UPSCALE_ALGO.set(newAlgo.codeName);
            SuperResolution.algorithmDescription = newAlgo;

            if (!SuperResolution.createAlgorithm()) {
                throw new RuntimeException("创建算法失败");
            }

            if (oldAlgorithmInstance != null) {
                try {
                    oldAlgorithmInstance.destroy();
                } catch (Exception e) {
                    SuperResolution.LOGGER.error("销毁旧算法时出错", e);
                }
            }

        } catch (Exception e) {
            SuperResolution.LOGGER.error("切换到算法 {} 失败，尝试回滚", newAlgo.displayName, e);

            UPSCALE_ALGO.set(oldDescription != null ? oldDescription.codeName : AlgorithmDescriptions.NONE.codeName);
            SuperResolution.algorithmDescription = oldDescription;
            SuperResolution.currentAlgorithm = oldAlgorithmInstance;

            if (oldAlgorithmInstance == null && oldDescription != null) {
                try {
                    if (!SuperResolution.createAlgorithm()) {
                        fallbackToNone();
                    }
                } catch (Exception ex) {
                    fallbackToNone();
                }
            }
        }
    }

    private static void fallbackToNone() {
        SuperResolution.LOGGER.error("所有回滚尝试失败，使用NONE算法");
        UPSCALE_ALGO.set(AlgorithmDescriptions.NONE.codeName);
        SuperResolution.algorithmDescription = AlgorithmDescriptions.NONE;
        SuperResolution.createAlgorithm();
    }

    public static boolean isEnableUpscaleOriginal() {
        return ENABLE_UPSCALE.get();
    }

    public static boolean isEnableUpscale() {
        if (SuperResolutionConfig.isDisableUpscaleOnVanilla())
            return isEnableUpscaleOriginal() && ShaderCompatHandler.irisApiIsShaderPackInUse();
        return isEnableUpscaleOriginal();
    }

    public static void setEnableUpscale(boolean value) {
        boolean resolutionChanged = isEnableUpscale() != value;
        ENABLE_UPSCALE.set(value);
        if (resolutionChanged) resolutionChangeCallback.run();
    }

    public static float getSharpness() {
        return SHARPNESS.get();
    }

    public static void setSharpness(float value) {
        SHARPNESS.set(value);
    }

    public static CaptureMode getCaptureMode() {
        return CAPTURE_MODE.get();
    }

    public static void setCaptureMode(CaptureMode value) {
        CAPTURE_MODE.set(value);
    }

    public static float getUpscaleRatio() {
        return UPSCALE_RATIO.get();
    }

    public static void setUpscaleRatio(float value) {
        boolean resolutionChanged = getUpscaleRatio() != value;
        UPSCALE_RATIO.set(value);
        if (resolutionChanged) resolutionChangeCallback.run();
    }

    public static boolean isDebugDumpShader() {
        return DEBUG_DUMP_SHADER.get();
    }

    public static void setDebugDumpShader(boolean value) {
        DEBUG_DUMP_SHADER.set(value);
    }

    public static boolean isSkipInitVulkan() {
        return SKIP_INIT_VULKAN.get();
    }

    public static void setSkipInitVulkan(boolean value) {
        SKIP_INIT_VULKAN.set(value);
    }

    public static boolean isEnableRenderDoc() {
        return ENABLE_RENDER_DOC.get();
    }

    public static void setEnableRenderDoc(boolean value) {
        ENABLE_RENDER_DOC.set(value);
    }

    public static boolean isEnableImgui() {
        return ENABLE_IMGUI.get();
    }

    public static void setEnableImgui(boolean value) {
        ENABLE_IMGUI.set(value);
    }

    public static boolean isGenerateMotionVectors() {
        return GENERATE_MOTION_VECTORS.get();
    }

    public static void setGenerateMotionVectors(boolean value) {
        GENERATE_MOTION_VECTORS.set(value);
    }

    public static boolean isPauseGameOnGui() {
        return PAUSE_GAME_ON_GUI.get();
    }

    public static void setPauseGameOnGui(boolean value) {
        PAUSE_GAME_ON_GUI.set(value);
    }

    public static List<String> getInjectPostChainBlackList() {
        return INJECT_POST_CHAIN_BLACKLIST.get();
    }

    public static void setInjectPostChainBlackList(List<String> value) {
        INJECT_POST_CHAIN_BLACKLIST.set(value);
    }

    public static void setEnableCompatShaderCompiler(boolean value) {
        ENABLE_COMPAT_SHADER_COMPILER.set(value);
    }

    public static boolean isEnableCompatShaderCompiler() {
        return ENABLE_COMPAT_SHADER_COMPILER.get() || ENABLE_COMPAT_SHADER_COMPILER.getDefault();
    }

    public static void setEnableDatasetGenerator(boolean value) {
        ENABLE_DATASET_GENERATOR.set(value);
    }

    public static boolean isEnableDatasetGenerator() {
        return ENABLE_DATASET_GENERATOR.get();
    }

    public static boolean isEnableDetailedProfiling() {
        return ENABLE_DETAILED_PROFILING.get();
    }

    public static void setEnableDetailedProfiling(boolean value) {
        ENABLE_DETAILED_PROFILING.set(value);
    }

    public static boolean isEnableDebug() {
        return ENABLE_DEBUG.get();
    }

    public static void setEnableDebug(boolean value) {
        ENABLE_DEBUG.set(value);
    }

    public static boolean isForceDisableShaderCompat() {
        return FORCE_DISABLE_SHADER_COMPAT.get();
    }

    public static void setForceDisableShaderCompat(boolean value) {
        FORCE_DISABLE_SHADER_COMPAT.set(value);
    }

    public static boolean isDisableUpscaleOnVanilla() {
        return DISABLE_UPSCALE_ON_VANILLA.get();
    }

    public static void setDisableUpscaleOnVanilla(boolean value) {
        DISABLE_UPSCALE_ON_VANILLA.set(value);
    }

    public static void setInternalTextureFormat(InternalTextureFormat format) {
        INTERNAL_TEXTURE_FORMAT.set(format);
    }

    public static String getInternalTextureFormatGlslFormatQualifier() {
        return switch (getInternalTextureFormat()) {
            case RGBA8 -> "rgba8";
            case RGBA16F -> "rgba16f";
            case RGBA16 -> "rgba16";
            case R11G11B10F -> "r11f_g11f_b10f";
            default -> "r11f_g11f_b10f";
        };
    }

    public static TextureFormat getInternalTextureFormat() {
        //user settings > shaderPack > default
        if (INTERNAL_TEXTURE_FORMAT.get() == InternalTextureFormat.AUTO) {
            Optional<SRShaderCompatConfig.WorldConfig> currentLevelCompatConfig = ShaderCompatHandler.getCurrentLevelCompatConfig();
            if (currentLevelCompatConfig.isPresent()) {
                if (currentLevelCompatConfig.get().enabled) {
                    return currentLevelCompatConfig.get().upscale_config.getSrInternalTextureFormat();
                }
            }
            return TextureFormat.R11G11B10F;
        }
        return INTERNAL_TEXTURE_FORMAT.get().format();
    }

    public static float getMinUpscaleRatio() {
        if (ShaderCompatHandler.isShaderPackCompatSuperResolution()) return 1.0f;
        return 0.5f;
        /*
        int maxSize = 16384;
        if (Minecraft.getInstance().getWindow() == null) return 0.1f;
        double maxWidth = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenWidth());
        double maxHeight = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenHeight());
        return (float) Math.max(maxWidth, maxHeight);
        */
    }
}