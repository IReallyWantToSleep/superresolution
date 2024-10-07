package io.homo.superresolution.config;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.upscale.AlgorithmManager;

public class Config {
    private static Config instance;
    public Config(){
        instance = this;
    }
    public static Config getInstance() {
        return instance;
    }

    private static float upscaleRatio = 1.7f;
    private static AlgorithmManager.AlgorithmType upscaleAlgo = AlgorithmManager.AlgorithmType.FSR1;
    private static float renderScaleFactor = 1 / upscaleRatio;
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

    public static AlgorithmManager.AlgorithmType getUpscaleAlgo() {
        return upscaleAlgo;
    }

    public static void setUpscaleAlgo(AlgorithmManager.AlgorithmType upscaleAlgo) {
        Config.upscaleAlgo = upscaleAlgo;
        SuperResolution.currentAlgorithm.destroy();
        SuperResolution.algorithmType = Config.upscaleAlgo;
        SuperResolution.getInstance().initAlgo();
    }
}
