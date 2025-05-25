package io.homo.superresolution.common;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import io.homo.superresolution.api.event.AlgorithmResizeEvent;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.debug.imgui.ImguiMain;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Resizable;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.platform.*;
import io.homo.superresolution.core.interop.GlVkInteropManager;
import io.homo.superresolution.core.GraphicsCapabilities;
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
import java.util.function.Consumer;

public final class SuperResolution implements Resizable, Destroyable {
    public static final String MOD_ID = "super_resolution";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final Logger LOGGER_CPP = LoggerFactory.getLogger("SuperResolution-CPP");
    private static final Requirement commonRequirement = Requirement.nothing()
            .glMajorVersion(4).glMinorVersion(3)
            .requiredGlExtension("GL_ARB_gl_spirv")
            .requiredGlExtension("GL_ARB_direct_state_access");
    public static AbstractAlgorithm currentAlgorithm;
    public static None defaultAlgorithm = new None();
    public static boolean isInit;
    public static boolean isPreInit;
    public static boolean gameIsLoad = false;
    public static float frameTimeDelta = 16.6f;
    public static AlgorithmDescription<?> algorithmDescription;
    public static GlVkInteropManager interopManager;
    public static int framebufferWidth = 0;
    public static int framebufferHeight = 0;
    public static int cachedWidth;
    public static int cachedHeight;
    private static Minecraft minecraft = Minecraft.getInstance();
    private static SuperResolution instance;
    public final KeyMapping OPENGUI_KEYMAPPING = new KeyMapping(
            "key.super_resolution.open_config",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F6,
            "Super Resolution"
    );

    public SuperResolution() {
        if (minecraft == null) minecraft = Minecraft.getInstance();
        KeyMappingRegistry.register(OPENGUI_KEYMAPPING);
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (OPENGUI_KEYMAPPING.consumeClick()) {
                minecraft.setScreen(
                        ConfigScreenBuilder.create().buildConfigScreen(minecraft.screen)
                );
            }
        });
    }

    public static void preInit() {
        if (minecraft == null) minecraft = Minecraft.getInstance();
        File gameDir = Platform.currentPlatform.getGameFolder().toFile();
        if (Platform.currentPlatform.getEnv() == EnvType.SERVER)
            throw new RuntimeException("SuperResolution不支持安装在服务器上！");
        if (!NativeLibManager.check(gameDir.getAbsolutePath())) {
            NativeLibManager.extract(gameDir.getAbsolutePath());
        }
        NativeLibManager.load(gameDir.getAbsolutePath());
        GlslangShaderCompiler.init();
        if (new OS().type == OSType.WINDOWS) {
            interopManager = new GlVkInteropManager();
            initVulkan();
        }

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
            Minecraft.getInstance().destroy();
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
            Minecraft.getInstance().destroy();
        }
    }

    public static void initRendering() {
        try (GlState ignored = new GlState()) {
            if (minecraft == null) minecraft = Minecraft.getInstance();
            if (!isPreInit) return;
            MinecraftRenderHandle.init();
            AlgorithmManager.init();
            algorithmDescription = Config.getUpscaleAlgo();
        }
    }

    public static boolean createAlgo() {
        try (GlState ignored = new GlState()) {
            if (minecraft == null) minecraft = Minecraft.getInstance();
            if (!isPreInit) return false;
            defaultAlgorithm.init();
            algorithmDescription = Config.getUpscaleAlgo();
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

    public void init() {
        if (minecraft == null) minecraft = Minecraft.getInstance();

        if (isInit)
            return;
        instance = this;
        if (Platform.currentPlatform.isDevelopmentEnvironment() && Config.isEnableImgui()) new ImguiMain();

        isInit = true;
        this.resize(SuperResolution.getMinecraftWidth(), SuperResolution.getMinecraftHeight());
    }

    public void resize(int width, int height) {
        cachedWidth = getMinecraftWidth();
        cachedHeight = getMinecraftHeight();
        if (currentAlgorithm != null) {
            AlgorithmResizeEvent.EVENT.invoker().onAlgorithmResize(
                    currentAlgorithm,
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight(),
                    MinecraftRenderHandle.getRenderWidth(),
                    MinecraftRenderHandle.getRenderHeight()
            );
            currentAlgorithm.resize(getMinecraftWidth(), getMinecraftHeight());
        }
        AlgorithmManager.resize(getMinecraftWidth(), getMinecraftHeight());
    }

    public void destroy() {
        if (interopManager != null) {
            interopManager.destroy();
        }
        if (currentAlgorithm != null)
            currentAlgorithm.destroy();
        AlgorithmManager.destroy();
    }
}
