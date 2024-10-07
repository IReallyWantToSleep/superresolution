package io.homo.superresolution;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.debug.imgui.ImguiMain;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import io.homo.superresolution.upscale.AbstractAlgorithm;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.utils.NativeLibManager;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.client.Minecraft.ON_OSX;

public final class SuperResolution implements CanResize, CanDestroy {
    public static final String MOD_ID = "super_resolution";
    public static ResolutionControl resolutioncontrol;
    private static SuperResolution instance;
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final Logger LOGGER_CPP = LoggerFactory.getLogger("SuperResolution-CPP");

    public static final Config config= new Config();
    public static AbstractAlgorithm currentAlgorithm;
    public static boolean isInit;
    private static final Minecraft minecraft = Minecraft.getInstance();
    public static boolean gameIsLoad = false;
    public static float frameTimeDelta = 16.6f;
    public static float frameTimeDelta_fsr = 16.6f;
    public static MainTarget mainTarget = (MainTarget) Minecraft.getInstance().getMainRenderTarget();
    public static boolean isRenderingWorld = false;
    public static boolean notSupportFSR2 = false;
    public static AlgorithmManager.AlgorithmType algorithmType = AlgorithmManager.AlgorithmType.FSR2;
    public SuperResolution(){}
    public static void initFSR2Lib(){
        if (NativeLibManager.exists(minecraft.gameDirectory.getAbsolutePath())) {
            LOGGER.info("FSR2库存在无需提取");
        }else {
            LOGGER.info("FSR2库不存在，正在提取");
            NativeLibManager.extract(minecraft.gameDirectory.getAbsolutePath());
        }
        NativeLibManager.load();
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

    public void init() {
        RenderSystem.assertOnRenderThread();
        instance = this;
        Minecraft.getInstance().getMainRenderTarget().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        Minecraft.getInstance().getMainRenderTarget().clear(ON_OSX);
        resolutioncontrol = new ResolutionControl(minecraft);
        resolutioncontrol.init();
        new ImguiMain();
        LOGGER.info("imgui初始化完成");
        initAlgo();
        LOGGER.info("初始化完成");
        isInit = true;
        this.resize(SuperResolution.getMinecraftWidth(),SuperResolution.getMinecraftHeight());
    }

    public void initAlgo() {
        currentAlgorithm = AlgorithmManager.getAlgorithm(algorithmType);
    }

    public void resize(int width,int height){
        RenderSystem.assertOnRenderThread();
        if (!notSupportFSR2) currentAlgorithm.resize(width, height);
        AlgorithmManager.resize(width, height);
    }

    public void destroy(){
        RenderSystem.assertOnRenderThread();
        if (!notSupportFSR2) currentAlgorithm.destroy();
        ResolutionControl.getInstance().destroy();
        AlgorithmManager.destroy();
    }
    public static void setFrameTimeDelta(float value){
        frameTimeDelta = value;
    }
    public static void setFrameTimeDeltaFSR(float value){
        frameTimeDelta_fsr = value;
    }
}

