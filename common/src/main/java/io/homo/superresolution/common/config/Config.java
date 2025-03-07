package io.homo.superresolution.common.config;

import com.google.gson.GsonBuilder;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.config.enums.SgsrVariant;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmType;
import net.minecraft.client.Minecraft;

public class Config {
    private static ConfigData instance;
    private static Runnable resolutionChangeCallback;

    static {
        setResolutionChangeCallback(() -> {
            MinecraftRenderHandle.resize();
            SuperResolution.getInstance().resize(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );
            Minecraft.getInstance().levelRenderer.resize(
                    MinecraftRenderHandle.getScreenWidth(),
                    MinecraftRenderHandle.getScreenHeight()
            );
        });
    }

    public static ConfigData getInstance() {
        return instance;
    }

    public static void setInstance(ConfigData instance) {
        Config.instance = instance;
    }

    public static void registerTypeAdapter(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(
                AlgorithmType.class,
                new EnumSerializer.Builder<AlgorithmType>()
                        .addMapping("fsr1", AlgorithmType.FSR1)
                        .addMapping("nis", AlgorithmType.NIS)
                        .addMapping("fsr2", AlgorithmType.FSR2)
                        .addMapping("sgsr", AlgorithmType.SGSR)
                        .addMapping("none", AlgorithmType.NONE)
                        .setDefault(AlgorithmType.FSR1)
                        .build()
        );

        gsonBuilder.registerTypeAdapter(
                CaptureMode.class,
                new EnumSerializer.Builder<CaptureMode>()
                        .addMapping("a", CaptureMode.A)
                        .addMapping("b", CaptureMode.B)
                        .addMapping("c", CaptureMode.C)
                        .setDefault(CaptureMode.A)
                        .build()
        );

        gsonBuilder.registerTypeAdapter(
                SgsrVariant.class,
                new EnumSerializer.Builder<SgsrVariant>()
                        .addMapping("CS_2", SgsrVariant.CS_2)
                        .addMapping("CS_3", SgsrVariant.CS_3)
                        .addMapping("FS_2", SgsrVariant.FS_2)
                        .setDefault(SgsrVariant.CS_2)
                        .build()
        );
    }

    public static CaptureMode getCaptureMode() {
        return instance.getCaptureMode();
    }

    public static void setCaptureMode(CaptureMode captureMode) {
        instance.setCaptureMode(captureMode);
    }

    public static float getRenderScaleFactor() {
        return instance.getRenderScaleFactor();
    }

    public static float getUpscaleRatio() {
        return instance.getUpscaleRatio();
    }

    public static void setUpscaleRatio(float value) {
        instance.setUpscaleRatio(value);
        if (getUpscaleRatio() != value) runResolutionChangeCallback();
    }

    public static AlgorithmType getUpscaleAlgo() {
        return instance.getUpscaleAlgo();
    }

    public static void setUpscaleAlgo(AlgorithmType upscaleAlgo) {
        instance.setUpscaleAlgo(upscaleAlgo);
    }

    public static float getSharpness() {
        return instance.getSharpness();
    }

    public static void setSharpness(float sharpness) {
        instance.setSharpness(sharpness);
    }

    public static double getMinUpscaleRatio() {
        int maxSize = 16384;
        if (Minecraft.getInstance().getWindow() == null) return 0.1;
        double maxWidth = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenWidth());
        double maxHeight = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenHeight());
        return Math.max(maxWidth, maxHeight);
    }

    public static boolean isEnableUpscale() {
        return instance.isEnableUpscale();
    }

    public static void setEnableUpscale(boolean enableUpscale) {
        instance.setEnableUpscale(enableUpscale);
    }

    public static SpecialConfigs getSpecial() {
        return instance.getSpecial();
    }

    public static void setSpecial(SpecialConfigs special) {
        instance.setSpecial(special);
    }

    public static void setResolutionChangeCallback(Runnable callback) {
        resolutionChangeCallback = callback;
    }

    private static void runResolutionChangeCallback() {
        if (resolutionChangeCallback != null) {
            resolutionChangeCallback.run();
        }
    }
}
