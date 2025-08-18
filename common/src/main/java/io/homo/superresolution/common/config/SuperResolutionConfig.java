package io.homo.superresolution.common.config;

import com.mojang.blaze3d.systems.RenderSystem;
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
import io.homo.superresolution.common.config.special.SpecialConfigs;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.core.graphics.GpuVendor;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
                value -> value > -0.1f && value <= 4.0f
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
                () -> CURRENT_OS_TYPE == OSType.WINDOWS && Platform.currentPlatform.isDevelopmentEnvironment(),
                "Enable RenderDoc integration (auto-disabled on incompatible OS)"
        );

        ENABLE_IMGUI = builder.defineBoolean(
                "debug/enable_imgui",
                () -> CURRENT_OS_TYPE == OSType.WINDOWS && Platform.currentPlatform.isDevelopmentEnvironment(),
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
                            GraphicsCapabilities.detectGpuVendor() == GpuVendor.INTEL ||
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

        SPECIAL = new SpecialConfigs(builder);
        Path configPath = Platform.currentPlatform
                .getGameFolder()
                .resolve("config")
                .resolve("super_resolution.toml");
        builder.configPath(configPath);
        SPEC = builder.build();
        resolutionChangeCallback = () -> {
            MinecraftRenderHandle.resize();
            SuperResolution.getInstance().resize(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );
            Minecraft.getInstance().gameRenderer.resize(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
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

    public static void setUpscaleAlgorithm(AlgorithmDescription<?> newAlgo) {
        if (newAlgo == null) {
            newAlgo = getDefaultAlgorithm();
        }

        AlgorithmDescription<?> currentAlgo = getUpscaleAlgorithm();
        if (currentAlgo == newAlgo) return;

        UPSCALE_ALGO.set(newAlgo.codeName);

        SuperResolution.algorithmDescription = newAlgo;
        if (SuperResolution.currentAlgorithm != null) {
            SuperResolution.currentAlgorithm.destroy();
        }

        if (!SuperResolution.createAlgorithm()) {
            UPSCALE_ALGO.set(currentAlgo.codeName);
            SuperResolution.algorithmDescription = currentAlgo;

            if (!SuperResolution.createAlgorithm()) {
                SuperResolution.LOGGER.error(
                        "在初始化算法 {} 时失败后在回退到算法 {} 时又发生异常",
                        newAlgo.displayName,
                        currentAlgo.displayName
                );
                throw new RuntimeException("Algorithm initialization failed");
            } else {
                SuperResolution.LOGGER.error(
                        "初始化算法 {} 失败，已回退到算法 {}",
                        newAlgo.displayName,
                        currentAlgo.displayName
                );
            }
        }
    }

    public static boolean isEnableUpscaleOriginal() {
        return ENABLE_UPSCALE.get();
    }

    public static boolean isEnableUpscale() {
        if (SuperResolutionConfig.isDisableUpscaleOnVanilla())
            return isEnableUpscaleOriginal() && SuperResolution.irisApiIsShaderPackInUse();
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

    public static float getMinUpscaleRatio() {
        if (SuperResolution.isShaderPackCompatSuperResolution()) return 1.0f;
        int maxSize = 16384;
        if (Minecraft.getInstance().getWindow() == null) return 0.1f;
        double maxWidth = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenWidth());
        double maxHeight = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenHeight());
        return (float) Math.max(maxWidth, maxHeight);
    }
}