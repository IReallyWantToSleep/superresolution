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
    private static CaptureMode captureMode = CaptureMode.A;

    public static CaptureMode getCaptureMode() {
        return captureMode;
    }

    public static void setCaptureMode(CaptureMode captureMode) {
        Config.captureMode = captureMode;
    }

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
        if (Config.upscaleAlgo == upscaleAlgo) return;
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
        if (Minecraft.getInstance().getWindow() == null) return 0.1;
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
        data.captureMode = getCaptureMode();

        return data;
    }

    public static void fromData(ConfigData data) {
        sharpness = data.sharpness;
        upscaleAlgo = ConfigData.stringToAlgoEnum(data.upscaleAlgo);
        setUpscaleRatio(data.upscaleRatio);
        setEnableUpscale(data.enableUpscale);
        setCaptureMode(data.captureMode);
    }

    public static boolean isEnableUpscale() {
        return enableUpscale;
    }

    public static void setEnableUpscale(boolean enableUpscale) {
        Config.enableUpscale = enableUpscale;
    }
}
