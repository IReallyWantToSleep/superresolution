package io.homo.superresolution.config;

public class Config {
    private static Config instance;
    public Config(){
        instance = this;
    }
    public static Config getInstance() {
        return instance;
    }

    //
    private static float fsr2Ratio = 1.7f;
    private static float renderScaleFactor = 1/fsr2Ratio;

    //
    public static float getRenderScaleFactor() {
        return renderScaleFactor;
    }

    public static float getFsr2Ratio() {
        return fsr2Ratio;
    }

    public static void setFsr2Ratio(float value) {
        fsr2Ratio = value;
        renderScaleFactor = 1/fsr2Ratio;
    }
}
