package io.homo.superresolution.config;

import io.homo.superresolution.upscale.AlgorithmType;

public class ConfigData {
    public static final ConfigData defaultConfig = new ConfigData();
    public float upscaleRatio = 1.7f;
    public String upscaleAlgo = algoEnumToString(AlgorithmType.FSR1);
    public float sharpness = 0.55f;
    public boolean enableUpscale = true;

    public static String algoEnumToString(AlgorithmType e) {
        return switch (e) {
            case FSR1 -> "fsr1";
            case FSR2 -> "fsr2";
            case NONE -> "none";
        };
    }

    public static AlgorithmType stringToAlgoEnum(String e) {
        return switch (e) {
            case "fsr1" -> AlgorithmType.FSR1;
            case "fsr2" -> AlgorithmType.FSR2;
            case "none" -> AlgorithmType.NONE;
            default -> AlgorithmType.FSR1;
        };
    }

    public void setUpscaleRatio(float upscaleRatio) {
        this.upscaleRatio = (float) Math.clamp(upscaleRatio, Config.getMinUpscaleRatio(), 4.0);
    }

    public void setUpscaleAlgo(String upscaleAlgo) {
        this.upscaleAlgo = algoEnumToString(stringToAlgoEnum(upscaleAlgo));
    }

    public void setSharpness(float sharpness) {
        this.sharpness = (float) Math.clamp(sharpness, 0.0, 2.0);
    }

    public void setEnableUpscale(boolean enableUpscale) {
        this.enableUpscale = enableUpscale;
    }
}
