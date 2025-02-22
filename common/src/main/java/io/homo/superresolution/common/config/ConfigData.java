package io.homo.superresolution.common.config;

import io.homo.superresolution.common.upscale.AlgorithmType;

public class ConfigData {
    public static final ConfigData defaultConfig = new ConfigData();
    public float upscaleRatio = 1.7f;
    public String upscaleAlgo = algoEnumToString(AlgorithmType.FSR1);
    public float sharpness = 0.55f;
    public boolean enableUpscale = true;
    public CaptureMode captureMode = CaptureMode.A;

    public static String algoEnumToString(AlgorithmType e) {
        return switch (e) {
            case FSR1 -> "fsr1";
            case FSR2 -> "fsr2";
            case NIS -> "nis";
            case NONE -> "none";
        };
    }

    public static AlgorithmType stringToAlgoEnum(String e) {
        return switch (e) {
            case "fsr1" -> AlgorithmType.FSR1;
            case "nis" -> AlgorithmType.NIS;
            case "fsr2" -> AlgorithmType.FSR2;
            case "none" -> AlgorithmType.NONE;
            default -> AlgorithmType.FSR1;
        };
    }

    public void setCaptureMode(CaptureMode captureMode) {
        this.captureMode = captureMode;
    }

    public void setUpscaleRatio(float upscaleRatio) {
        this.upscaleRatio = (float) Math.min(Math.max(upscaleRatio, Config.getMinUpscaleRatio()), 4.0);
    }

    public void setUpscaleAlgo(String upscaleAlgo) {
        this.upscaleAlgo = algoEnumToString(stringToAlgoEnum(upscaleAlgo));
    }

    public void setSharpness(float sharpness) {
        this.sharpness = (float) Math.min(Math.max(sharpness, 0.0), 2.0);
    }

    public void setEnableUpscale(boolean enableUpscale) {
        this.enableUpscale = enableUpscale;
    }
}
