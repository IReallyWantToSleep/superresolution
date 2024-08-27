package io.homo.superresolution;

import io.homo.superresolution.config.Config;
import io.homo.superresolution.fsr2.FSR2;
import io.homo.superresolution.fsr2.nativelib.FSR2LibManager;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SuperResolution implements CanResize, CanDestroy {
    public static final String MOD_ID = "super_resolution";
    public static ResolutionControl resolutioncontrol;
    private static SuperResolution instance;
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution");
    public static final Config config= new Config();
    public static FSR2 FSR;
    public static boolean isInit;
    private static final Minecraft minecraft = Minecraft.getInstance();
    public static boolean gameIsLoad = false;
    public SuperResolution(){}
    public void init() {
        instance = this;
        resolutioncontrol = new ResolutionControl(minecraft);
        resolutioncontrol.init();
        if (FSR2LibManager.exists(minecraft.gameDirectory.getAbsolutePath())){
            LOGGER.info("FSR2 library exists.");
        }else {
            LOGGER.info("FSR2 library does not exist.");
            FSR2LibManager.extract(minecraft.gameDirectory.getAbsolutePath());
        }
        FSR2LibManager.load();
        FSR = new FSR2();
        this.resize(minecraft.getWindow().getWidth(),minecraft.getWindow().getHeight());
        LOGGER.info("Loading FSR2 library completed.");
        LOGGER.info("Initialization completed.");
        isInit = true;
    }
    public static SuperResolution getInstance() {
        return instance;
    }
    public void resize(int width,int height){
        if (minecraft.level != null){
            FSR.resize(width, height);
        }
    }

    public static int getMinecraftWidth(){
        return minecraft.getWindow().getWidth();
    }
    public static int getMinecraftHeight(){
        return minecraft.getWindow().getHeight();
    }
    public void destroy(){
        FSR.destroy();
    }
}

