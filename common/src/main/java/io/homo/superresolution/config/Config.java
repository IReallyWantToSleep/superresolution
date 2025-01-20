package io.homo.superresolution.config;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.render.gl.Gl;
import io.homo.superresolution.upscale.AlgorithmType;
import net.minecraft.client.Minecraft;


public class Config {
    private static float upscaleRatio = 1.7f;
    private static AlgorithmType upscaleAlgo = AlgorithmType.FSR1;
    private static float renderScaleFactor = 1 / upscaleRatio;
    private static float FSR1_sharpness = 0.2f;

    public static float getRenderScaleFactor() {
        return renderScaleFactor;
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
        SuperResolution.initAlgo();
    }

    public static float getSharpness() {
        return FSR1_sharpness;
    }

    public static void setSharpness(float FSR1_sharpness) {
        Config.FSR1_sharpness = FSR1_sharpness;
    }

    public static double getMinUpscaleRatio() {
        int maxSize = Gl.glGetMaxTextureSize();
        double maxWidth = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenWidth());
        double maxHeight = 1 / ((double) maxSize / Minecraft.getInstance().getWindow().getScreenHeight());
        return Math.max(maxWidth, maxHeight);
    }
}
