package io.homo.superresolution.common;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import io.homo.superresolution.api.event.AlgorithmResizeEvent;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.dataset.DataSetGenerator;
import io.homo.superresolution.common.debug.imgui.ImguiMain;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.GpuVendor;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Resizable;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.platform.*;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.none.None;
import net.minecraft.client.KeyMapping;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.core.utils.MessageBox;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class SuperResolution implements Resizable, Destroyable {
    public static final String MOD_ID = "super_resolution";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final Logger LOGGER_CPP = LoggerFactory.getLogger("SuperResolution-CPP");
    private static final Requirement commonRequirement = Requirement.nothing()
            .glMajorVersion(4).glMinorVersion(1);
    public static AbstractAlgorithm currentAlgorithm;
    public static None defaultAlgorithm = new None();
    public static boolean isInit;
    public static boolean isPreInit;
    public static boolean gameIsLoad = false;
    public static AlgorithmDescription<?> algorithmDescription;
    public static int framebufferWidth = 0;
    public static int framebufferHeight = 0;
    public static int cachedWidth;
    public static int cachedHeight;
    public static Thread renderThread;
    private static Minecraft minecraft = Minecraft.getInstance();
    private static SuperResolution instance;
    public static final KeyMapping OPENGUI_KEYMAPPING = new KeyMapping(
            "key.super_resolution.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F6,
            "Super Resolution"
    );
    private static boolean registeredKeyMapping = false;

    public static void registerKeyMapping() {
        if (!registeredKeyMapping) {
            if (SuperResolutionConfig.isEnableDatasetGenerator()) DataSetGenerator.init();
            
            KeyMappingRegistry.register(OPENGUI_KEYMAPPING);
        }
        registeredKeyMapping = true;
    }

    public static void registerEvents() {

        ClientLifecycleEvent.CLIENT_SETUP.register(
                (minecraft) -> registerKeyMapping()
        );
        ClientLifecycleEvent.CLIENT_STARTED.register(
                (minecraft) -> {
                    instance = new SuperResolution();
                    SuperResolution.preInit();
                    SuperResolution.initRendering();
                    SuperResolution.createAlgorithm();
                    SuperResolution.getInstance().init();
                }
        );
        ClientLifecycleEvent.CLIENT_STOPPING.register(
                (minecraft) -> {
                    SuperResolution.getInstance().destroy();
                }
        );
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (SuperResolution.getInstance().OPENGUI_KEYMAPPING.consumeClick()) {
                minecraft.setScreen(
                        ConfigScreenBuilder.create().buildConfigScreen(minecraft.screen)
                );
            }
        });
    }

    public SuperResolution() {
        instance = this;
        if (minecraft == null) minecraft = Minecraft.getInstance();
    }

    public static void preInit() {
        if (minecraft == null) minecraft = Minecraft.getInstance();
        File gameDir = Platform.currentPlatform.getGameFolder().toFile();
        if (Platform.currentPlatform.getEnv() == EnvType.SERVER)
            throw new RuntimeException("SuperResolution不支持安装在服务器上！");
        NativeLibManager.extract(gameDir.getAbsolutePath());
        NativeLibManager.load(gameDir.getAbsolutePath());
        GlslangShaderCompiler.init();
        isPreInit = true;
    }

    public static SuperResolution getInstance() {
        return instance;
    }

    public static void check() {
        if (minecraft == null) minecraft = Minecraft.getInstance();

        if (!commonRequirement.check().glVersionMet()) {
            MessageBox.createError(
                    Component.translatable("superresolution.common_requirement.not_support.version").getString().formatted(
                            commonRequirement.getGlMajorVersion(),
                            commonRequirement.getGlMinorVersion(),
                            GraphicsCapabilities.getGLVersion()[0],
                            GraphicsCapabilities.getGLVersion()[1]),
                    Component.translatable("superresolution.common_requirement.not_support.msg").getString()
            );
            System.exit(1);
        }

        if (!commonRequirement.check().glExtensionsPresent()) {
            StringBuilder extensionStringBuilder = new StringBuilder();
            for (String name : commonRequirement.getMissingGlExtensions()) {
                extensionStringBuilder.append(name).append("\n");
            }
            MessageBox.createError(Component.translatable("superresolution.common_requirement.not_support.extension").getString()
                            .formatted(extensionStringBuilder.toString()),
                    Component.translatable("superresolution.common_requirement.not_support.msg").getString()
            );
            System.exit(1);
        }
    }

    public static void initRendering() {
        renderThread = Thread.currentThread();
        try (GlState ignored = new GlState()) {
            RenderSystems.init();

            if (minecraft == null) minecraft = Minecraft.getInstance();
            if (!isPreInit) return;
            if (SuperResolutionConfig.isEnableCompatShaderCompiler() == SuperResolutionConfig.ENABLE_COMPAT_SHADER_COMPILER.getDefault()) {
                if (GraphicsCapabilities.detectGpuVendor() == GpuVendor.INTEL) {
                    SuperResolutionConfig.setEnableCompatShaderCompiler(true);
                    SuperResolutionConfig.SPEC.save();
                }
            }

            LOGGER.info("显卡供应商 {}", GraphicsCapabilities.detectGpuVendor().name());
            LOGGER.info("OpenGL版本 {}", GraphicsCapabilities.getGLVersionString());


            MinecraftRenderHandle.init();
            AlgorithmManager.init();
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
        }
    }

    public static boolean createAlgorithm() {
        try (GlState ignored = new GlState()) {
            if (minecraft == null) minecraft = Minecraft.getInstance();
            if (!isPreInit) return false;
            defaultAlgorithm.init();
            algorithmDescription = SuperResolutionConfig.getUpscaleAlgorithm();
            try {
                currentAlgorithm = algorithmDescription.createNewInstance();
                SuperResolution.LOGGER.info("初始化算法 {}", algorithmDescription.getDisplayName());
                return true;
            } catch (Exception e) {
                SuperResolution.LOGGER.info("初始化算法 {} 时失败 错误:", algorithmDescription.getDisplayName());
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean recreateAlgorithm() {
        try (GlState ignored = new GlState()) {
            if (minecraft == null) minecraft = Minecraft.getInstance();
            if (!isPreInit) {
                return false;
            }

            if (currentAlgorithm != null) {
                currentAlgorithm.destroy();
            }

            try {
                currentAlgorithm = algorithmDescription.createNewInstance();
                currentAlgorithm.init();
                currentAlgorithm.resize(MinecraftWindow.getWindowWidth(), MinecraftWindow.getWindowHeight());
                AlgorithmResizeEvent.EVENT.invoker().onAlgorithmResize(
                        currentAlgorithm,
                        MinecraftRenderHandle.getScreenWidth(),
                        MinecraftRenderHandle.getScreenHeight(),
                        MinecraftRenderHandle.getRenderWidth(),
                        MinecraftRenderHandle.getRenderHeight()
                );

                return true;
            } catch (Exception e) {
                LOGGER.error("初始化算法 {} 时失败：", algorithmDescription.getDisplayName(), e);
            }
        }
        return false;
    }

    public static AbstractAlgorithm getCurrentAlgorithm() {
        if (SuperResolutionConfig.isEnableUpscale() && currentAlgorithm != null) {
            return currentAlgorithm;
        }
        return defaultAlgorithm;
    }

    public static void irisApiReloadShader() {
        try {
            Class<?> irisApiClazz = Class.forName("net.irisshaders.iris.Iris");
            irisApiClazz.getMethod("reload").invoke(null);
        } catch (Throwable ignored) {
        }
    }

    public static boolean irisApiIsShaderPackInUse() {
        try {
            Class<?> irisApiClazz = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object irisApiInstance = irisApiClazz.getMethod("getInstance").invoke(null);
            return (boolean) irisApiClazz.getMethod("isShaderPackInUse").invoke(irisApiInstance);
        } catch (Throwable ignored) {
        }
        return false;
    }

    public void init() {
        if (minecraft == null) minecraft = Minecraft.getInstance();

        if (isInit)
            return;
        if (Platform.currentPlatform.isDevelopmentEnvironment() && SuperResolutionConfig.isEnableImgui())
            new ImguiMain();

        isInit = true;
        this.resize(MinecraftWindow.getWindowWidth(), MinecraftWindow.getWindowHeight());
    }

    public void resize(int width, int height) {
        cachedWidth = MinecraftWindow.getWindowWidth();
        cachedHeight = MinecraftWindow.getWindowHeight();
        if (currentAlgorithm != null) {
            AlgorithmResizeEvent.EVENT.invoker().onAlgorithmResize(
                    currentAlgorithm,
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight(),
                    MinecraftRenderHandle.getRenderWidth(),
                    MinecraftRenderHandle.getRenderHeight()
            );
            currentAlgorithm.resize(MinecraftWindow.getWindowWidth(), MinecraftWindow.getWindowHeight());
        }
        AlgorithmManager.resize(MinecraftWindow.getWindowWidth(), MinecraftWindow.getWindowHeight());
    }

    public void destroy() {
        RenderSystems.destroy();
        if (currentAlgorithm != null)
            currentAlgorithm.destroy();
        AlgorithmManager.destroy();
    }

    public static boolean isShaderPackCompatSuperResolution() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.IrisShaderPipelineHandle");
            return (Boolean) irisApiClazz.getMethod("shouldApplySuperResolutionChanges").invoke(null);
        } catch (Throwable e) {
            return false;
        }
    }


    public static boolean isShaderPackCompatSuperResolutionJitter() {
        try {
            Class<?> irisApiClazz = Class.forName("io.homo.superresolution.shadercompat.IrisShaderPipelineHandle");
            return (Boolean) irisApiClazz.getMethod("shouldApplySuperResolutionChangesJitter").invoke(null);
        } catch (Throwable e) {
            return false;
        }
    }
}
