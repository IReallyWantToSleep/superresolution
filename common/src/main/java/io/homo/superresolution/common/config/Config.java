package io.homo.superresolution.common.config;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.upscale.AlgorithmType;
import net.minecraft.client.Minecraft;

public class Config {
    private static boolean enableUpscale = true;
    private static float upscaleRatio = 1.7f;
    private static AlgorithmType upscaleAlgo = AlgorithmType.FSR1;
    private static float renderScaleFactor = 1 / upscaleRatio;
    private static float sharpness = 0.2f;

    public static float getRenderScaleFactor() {
        return isEnableUpscale() ? renderScaleFactor : 1;
    }

    public static float getUpscaleRatio() {
        return upscaleRatio;
    }

    public static void setUpscaleRatio(float value) {
        upscaleRatio = value;
        renderScaleFactor = 1 / upscaleRatio;
    }

    public static AlgorithmType getUpscaleAlgo() {
        return upscaleAlgo;
    }

    public static void setUpscaleAlgo(AlgorithmType upscaleAlgo) {
        Config.upscaleAlgo = upscaleAlgo;
        SuperResolution.algorithmType = Config.upscaleAlgo;
        if (SuperResolution.currentAlgorithm != null) SuperResolution.currentAlgorithm.destroy();
        SuperResolution.createAlgo();
    }

    public static float getSharpness() {
        return sharpness;
    }

    public static void setSharpness(float sharpness) {
        Config.sharpness = sharpness;
    }

    public static double getMinUpscaleRatio() {
        int maxSize = 16384;
        double maxWidth = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenWidth());
        double maxHeight = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenHeight());
        return Math.max(maxWidth, maxHeight);
    }

    public static ConfigData buildData() {
        ConfigData data = new ConfigData();
        data.sharpness = sharpness;
        data.upscaleAlgo = ConfigData.algoEnumToString(getUpscaleAlgo());
        data.upscaleRatio = upscaleRatio;
        data.enableUpscale = isEnableUpscale();
        return data;
    }

    public static void fromData(ConfigData data) {
        sharpness = data.sharpness;
        upscaleAlgo = ConfigData.stringToAlgoEnum(data.upscaleAlgo);
        upscaleRatio = data.upscaleRatio;
        setEnableUpscale(data.enableUpscale);
    }

    public static boolean isEnableUpscale() {
        return enableUpscale;
    }

    public static void setEnableUpscale(boolean enableUpscale) {
        Config.enableUpscale = enableUpscale;
    }
}
