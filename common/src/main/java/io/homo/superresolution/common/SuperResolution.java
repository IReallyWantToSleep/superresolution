package io.homo.superresolution.common;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.ConfigFile;
import io.homo.superresolution.common.debug.imgui.ImguiMain;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.mixin.core.WindowMixin;
import io.homo.superresolution.common.platform.EnvType;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.render.GlVkInteropManager;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.Gl;
import io.homo.superresolution.common.render.impl.framebuffer.MinecraftRenderTarget;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.AlgorithmType;
import io.homo.superresolution.common.upscale.none.None;
import io.homo.superresolution.common.upscale.utils.NativeLibManager;
import io.homo.superresolution.common.upscale.utils.Requirement;
import io.homo.superresolution.common.utils.MessageBox;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public final class SuperResolution implements Resizable, Destroyable {
    public static final String MOD_ID = "super_resolution";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final Logger LOGGER_CPP = LoggerFactory.getLogger("SuperResolution-CPP");
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Requirement commonRequirement = Requirement.nothing().glMajorVersion(4).glMinorVersion(3);
    public static AbstractAlgorithm currentAlgorithm;
    public static None defaultAlgorithm = None.create();
    public static boolean isInit;
    public static boolean isPreInit;
    public static boolean gameIsLoad = false;
    public static float frameTimeDelta = 16.6f;
    public static RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
    public static AlgorithmType algorithmType;
    public static GlVkInteropManager interopManager;
    public static int framebufferWidth = 0;
    public static int framebufferHeight = 0;
    public static int cachedWidth;
    public static int cachedHeight;
    private static SuperResolution instance;

    public SuperResolution() {
    }

    public static void preInit() {
        if (Platform.currentPlatform.getEnv() == EnvType.SERVER)
            throw new RuntimeException("SuperResolution不支持安装在服务器上！");
        if (Config.isSkipLoadNativeLib()) {
            LOGGER.warn("配置已禁用加载依赖库，将不再加载本地依赖库");
        } else if (Platform.currentPlatform.getOS().type == OSType.ANDROID) {
            LOGGER.warn("检测到在移动设备上运行，已跳过加载依赖库");
        } else if (Platform.currentPlatform.getOS().type == OSType.MACOS) {
            LOGGER.warn("检测到在MacOS上运行，已跳过加载依赖库");
        } else {
            if (!NativeLibManager.check(minecraft.gameDirectory.getAbsolutePath())) {
                NativeLibManager.extract(minecraft.gameDirectory.getAbsolutePath());
            }
            NativeLibManager.load(minecraft.gameDirectory.getAbsolutePath());
        }
        interopManager = new GlVkInteropManager();
        initVulkan();
        isPreInit = true;
    }

    public static int getMinecraftWidth() {
        return Math.max(Minecraft.getInstance().getWindow().getScreenWidth(), 1);
    }

    public static int getMinecraftHeight() {
        return Math.max(Minecraft.getInstance().getWindow().getScreenHeight(), 1);
    }

    public static SuperResolution getInstance() {
        return instance;
    }

    public static void setFrameTimeDelta(float value) {
        frameTimeDelta = value;
    }

    public static void check() {
        if (!commonRequirement.checkGlVersion()) {
            MessageBox.createError(
                    Component.translatable("superresolution.common_requirement.not_support.version").getString().formatted(
                            commonRequirement.getGlMajorVersion(),
                            commonRequirement.getGlMinorVersion(),
                            Gl.getVersion()[0],
                            Gl.getVersion()[0]),
                    Component.translatable("superresolution.common_requirement.not_support.msg").getString()
            );
            Minecraft.getInstance().destroy();
        }

        if (!commonRequirement.checkExtension()) {
            StringBuilder extensionStringBuilder = new StringBuilder();
            for (String name : commonRequirement.getMissingExtension()) {
                extensionStringBuilder.append(name).append("\n");
            }
            MessageBox.createError(Component.translatable("superresolution.common_requirement.not_support.extension").getString()
                            .formatted(extensionStringBuilder.toString()),
                    Component.translatable("superresolution.common_requirement.not_support.msg").getString()
            );
            Minecraft.getInstance().destroy();
        }
    }

    public static void initRendering() {
        if (!isPreInit) return;
        RenderSystem.assertOnRenderThread();
        MinecraftRenderHandle.init();
        AlgorithmManager.init();
        algorithmType = Config.getUpscaleAlgo();
    }

    public static boolean createAlgo() {
        if (!isPreInit) return false;
        defaultAlgorithm.init();
        try {
            currentAlgorithm = AlgorithmManager.getAlgorithm(algorithmType);
            SuperResolution.LOGGER.info("初始化算法 {}", algorithmType.toString());
            return true;
        } catch (Exception e) {
            SuperResolution.LOGGER.info("初始化算法 {} 时失败 错误 {}", algorithmType.toString(), e.getMessage());
        }
        return false;
    }

    public static void initVulkan() {
        interopManager.init();
    }

    public static void callAlgo(Consumer<AbstractAlgorithm> fn) {
        if (currentAlgorithm != null) {
            fn.accept(currentAlgorithm);
        }
    }

    public static AbstractAlgorithm getCurrentAlgorithm() {
        if (Config.isEnableUpscale() && currentAlgorithm != null) {
            return currentAlgorithm;
        }
        return defaultAlgorithm;
    }

    public static boolean isPojavLauncher() {
        return System.getenv("POJAV_RENDERER") != null;
    }

    public void init() {
        if (isInit)
            return;
        RenderSystem.assertOnRenderThread();
        instance = this;
        if (Platform.currentPlatform.isDevelopmentEnvironment() && Config.isEnableImgui()) new ImguiMain();
        mainTarget = Minecraft.getInstance().getMainRenderTarget();
        isInit = true;
        this.resize(SuperResolution.getMinecraftWidth(), SuperResolution.getMinecraftHeight());
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        cachedWidth = getMinecraftWidth();
        cachedHeight = getMinecraftHeight();
        ((MinecraftRenderTarget) MinecraftRenderHandle.getRenderTarget()).enableStencil();
        if (currentAlgorithm != null)
            currentAlgorithm.resize(getMinecraftWidth(), getMinecraftHeight());
        AlgorithmManager.resize(getMinecraftWidth(), getMinecraftHeight());
    }

    public void destroy() {
        RenderSystem.assertOnRenderThread();
        interopManager.destroy();
        if (currentAlgorithm != null)
            currentAlgorithm.destroy();
        AlgorithmManager.destroy();
    }
}
