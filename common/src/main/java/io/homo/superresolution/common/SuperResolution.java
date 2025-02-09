package io.homo.superresolution.common;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.platform.Platform;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.ConfigFile;
import io.homo.superresolution.common.debug.imgui.ImguiMain;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.render.GlVkInteropManager;
import io.homo.superresolution.common.render.MinecraftRenderingStates;
import io.homo.superresolution.common.render.gl.Gl;
import io.homo.superresolution.common.render.renderdoc.RenderDoc;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.AlgorithmType;
import io.homo.superresolution.common.upscale.none.None;
import io.homo.superresolution.common.upscale.utils.NativeLibManager;
import io.homo.superresolution.common.upscale.utils.Requirement;
import io.homo.superresolution.common.utils.MessageBox;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SuperResolution implements Resizable, Destroyable {
    public static final String MOD_ID = "super_resolution";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final Logger LOGGER_CPP = LoggerFactory.getLogger("SuperResolution-CPP");
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Requirement commonRequirement = Requirement.nothing().glMajorVersion(4).glMinorVersion(3);
    public static AbstractAlgorithm currentAlgorithm;
    public static None defaultAlgorithm = None.create();
    public static boolean isInit;
    public static boolean gameIsLoad = false;
    public static float frameTimeDelta = 16.6f;
    public static MainTarget mainTarget = (MainTarget) Minecraft.getInstance().getMainRenderTarget();
    public static boolean isRenderingWorld = false;
    public static AlgorithmType algorithmType;
    public static GlVkInteropManager interopManager;
    private static SuperResolution instance;

    public SuperResolution() {
    }

    public static void preInit() {
        if (Platform.getEnv() == EnvType.SERVER) throw new RuntimeException("SuperResolution不支持安装在服务器上！");
        if (Platform.isDevelopmentEnvironment()) RenderDoc.init();
        if (!NativeLibManager.check(minecraft.gameDirectory.getAbsolutePath())) {
            NativeLibManager.extract(minecraft.gameDirectory.getAbsolutePath());
        }
        NativeLibManager.load(minecraft.gameDirectory.getAbsolutePath());
        interopManager = new GlVkInteropManager();
        initVulkan();
    }

    public static int getMinecraftWidth() {
        return minecraft.getWindow().getScreenWidth();
    }

    public static int getMinecraftHeight() {
        return minecraft.getWindow().getScreenHeight();
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
        RenderSystem.assertOnRenderThread();
        MinecraftRenderingStates.init();
        Config.fromData(ConfigFile.read());
        algorithmType = Config.getUpscaleAlgo();
    }

    public static void createAlgo() {
        currentAlgorithm = AlgorithmManager.getAlgorithm(algorithmType);
        defaultAlgorithm.init();
        SuperResolution.LOGGER.info("初始化算法 {}", algorithmType.toString());
    }

    public static void initVulkan() {
        interopManager.init();
    }

    public void init() {
        if (isInit)
            return;
        RenderSystem.assertOnRenderThread();
        instance = this;
        if (!ConfigFile.exists()) ConfigFile.write();
        if (Platform.isDevelopmentEnvironment()) new ImguiMain();
        mainTarget = (MainTarget) Minecraft.getInstance().getMainRenderTarget();
        isInit = true;
        this.resize(SuperResolution.getMinecraftWidth(), SuperResolution.getMinecraftHeight());
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        if (currentAlgorithm != null)
            currentAlgorithm.resize(width, height);
        AlgorithmManager.resize(width, height);
    }

    public void destroy() {
        RenderSystem.assertOnRenderThread();
        //interopManager.destroy();
        if (currentAlgorithm != null)
            currentAlgorithm.destroy();
        AlgorithmManager.destroy();
    }
}
