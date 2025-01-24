package io.homo.superresolution;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.platform.Platform;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.config.ConfigFile;
import io.homo.superresolution.debug.imgui.ImguiMain;
import io.homo.superresolution.impl.Destroyable;
import io.homo.superresolution.impl.Resizable;
import io.homo.superresolution.render.GlVkInteropManager;
import io.homo.superresolution.render.MinecraftRenderingStates;
import io.homo.superresolution.render.gl.Gl;
import io.homo.superresolution.upscale.AbstractAlgorithm;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.AlgorithmType;
import io.homo.superresolution.upscale.none.None;
import io.homo.superresolution.upscale.utils.NativeLibManager;
import io.homo.superresolution.upscale.utils.Requirement;
import io.homo.superresolution.utils.MessageBox;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SuperResolution implements Resizable, Destroyable {
    public static final String MOD_ID = "super_resolution";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final Logger LOGGER_CPP = LoggerFactory.getLogger("SuperResolution-CPP");
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final Requirement commonRequirement = Requirement.nothing().majorVersion(4).minorVersion(3);
    public static AbstractAlgorithm currentAlgorithm;
    public static None defaultAlgorithm = None.create();
    public static boolean isInit;
    public static boolean gameIsLoad = false;
    public static float frameTimeDelta = 16.6f;
    public static MainTarget mainTarget = (MainTarget) Minecraft.getInstance().getMainRenderTarget();
    public static boolean isRenderingWorld = false;
    public static AlgorithmType algorithmType = Config.getUpscaleAlgo();
    public static GlVkInteropManager interopManager;

    private static SuperResolution instance;

    public SuperResolution() {
    }

    public static void preInit() {

        if (!NativeLibManager.check(minecraft.gameDirectory.getAbsolutePath())) {
            NativeLibManager.extract(minecraft.gameDirectory.getAbsolutePath());
        }
        NativeLibManager.load(minecraft.gameDirectory.getAbsolutePath());
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
        if (!commonRequirement.checkVersion()) {
            MessageBox.createError(
                    "OpenGL版本不符合要求\n要求版本:%s.%s\n实际版本:%s.%s".formatted(
                            commonRequirement.getMajorVersion(),
                            commonRequirement.getMinorVersion(),
                            Gl.getVersion()[0],
                            Gl.getVersion()[0]),
                    "SuperResolution 错误");
            Minecraft.getInstance().destroy();
        }

        if (!commonRequirement.checkExtension()) {
            StringBuilder extensionStringBuilder = new StringBuilder();
            for (String name : commonRequirement.getMissingExtension()) {
                extensionStringBuilder.append(name).append("\n");
            }
            MessageBox.createError("缺少必要的OpenGL扩展\n缺少的扩展:%s"
                            .formatted(extensionStringBuilder.toString()),
                    "SuperResolution 错误"
            );
            Minecraft.getInstance().destroy();
        }
    }

    public static void initRendering() {
        if (!gameIsLoad) return;
        RenderSystem.assertOnRenderThread();
        MinecraftRenderingStates.init();
    }

    public static void createAlgo(){
        currentAlgorithm = AlgorithmManager.getAlgorithm(algorithmType);
        defaultAlgorithm.init();
    }

    public static void initVulkan() {
        interopManager.init();
    }

    public void init() {
        if (isInit)
            return;
        RenderSystem.assertOnRenderThread();
        Config.fromData(ConfigFile.read());
        instance = this;
        if (!ConfigFile.exists()) ConfigFile.write();
        interopManager = new GlVkInteropManager();
        //initVulkan();
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
