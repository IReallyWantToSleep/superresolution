package io.homo.superresolution.common.config;

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.special.SpecialConfigs;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;

public class ConfigData {
    private SpecialConfigs special = new SpecialConfigs();
    private boolean enableUpscale = true;
    private float upscaleRatio = 1.7f;
    private AlgorithmDescription<?> upscaleAlgo = AlgorithmDescriptions.FSR1;
    private float sharpness = 0.55f;
    private CaptureMode captureMode = CaptureMode.A;
    private boolean debugDumpShader = false;
    private boolean skipLoadNativeLib;
    private boolean skipInitVulkan;
    private boolean enableRenderDoc;
    private boolean enableImgui;

    public ConfigData() {
        boolean compatMode = new OS().type == OSType.MACOS || new OS().type == OSType.ANDROID;
        skipLoadNativeLib = compatMode;
        skipInitVulkan = compatMode;
        enableRenderDoc = !compatMode;
        enableImgui = !compatMode;

    }

    public boolean isEnableRenderDoc() {
        return enableRenderDoc;
    }

    public void setEnableRenderDoc(boolean enableRenderDoc) {
        this.enableRenderDoc = enableRenderDoc;
    }

    public boolean isEnableImgui() {
        return enableImgui;
    }

    public void setEnableImgui(boolean enableImgui) {
        this.enableImgui = enableImgui;
    }

    public boolean isSkipLoadNativeLib() {
        return skipLoadNativeLib;
    }

    public void setSkipLoadNativeLib(boolean skipLoadNativeLib) {
        this.skipLoadNativeLib = skipLoadNativeLib;
    }

    public boolean isSkipInitVulkan() {
        return skipInitVulkan;
    }

    public void setSkipInitVulkan(boolean skipInitVulkan) {
        this.skipInitVulkan = skipInitVulkan;
    }

    public boolean isDebugDumpShader() {
        return debugDumpShader;
    }

    public void setDebugDumpShader(boolean debugDumpShader) {
        this.debugDumpShader = debugDumpShader;
    }

    public CaptureMode getCaptureMode() {
        return this.captureMode;
    }

    public void setCaptureMode(CaptureMode captureMode) {
        this.captureMode = captureMode;
    }

    public float getRenderScaleFactor() {
        return isEnableUpscale() ? 1 / getUpscaleRatio() : 1;
    }

    public float getUpscaleRatio() {
        return this.upscaleRatio;
    }

    public void setUpscaleRatio(float value) {
        this.upscaleRatio = value;
    }

    public AlgorithmDescription<?> getUpscaleAlgo() {
        return this.upscaleAlgo;
    }

    public void setUpscaleAlgo(AlgorithmDescription<?> upscaleAlgo) {
        if (this.upscaleAlgo == upscaleAlgo) return;
        AlgorithmDescription<?> lastUpscaleAlgo = this.upscaleAlgo;
        this.upscaleAlgo = upscaleAlgo;
        SuperResolution.algorithmDescription = this.upscaleAlgo;
        if (SuperResolution.currentAlgorithm != null) {
            SuperResolution.currentAlgorithm.destroy();
        }
        if (!SuperResolution.createAlgo()) {
            this.upscaleAlgo = lastUpscaleAlgo;
            SuperResolution.algorithmDescription = this.upscaleAlgo;
            if (!SuperResolution.createAlgo()) {
                SuperResolution.LOGGER.error("在初始化算法 {} 时失败后在回退到算法 {} 时又发生异常", upscaleAlgo.toString(), lastUpscaleAlgo.toString());
                throw new RuntimeException();
            } else {
                SuperResolution.LOGGER.error("初始化算法 {} 失败，已回退到算法 {}", upscaleAlgo.toString(), lastUpscaleAlgo.toString());
            }
        }

    }

    public float getSharpness() {
        return this.sharpness;
    }

    public void setSharpness(float sharpness) {
        this.sharpness = sharpness;
    }

    public boolean isEnableUpscale() {
        return this.enableUpscale;
    }

    public void setEnableUpscale(boolean enableUpscale) {
        this.enableUpscale = enableUpscale;
    }

    public SpecialConfigs getSpecial() {
        return special;
    }

    public void setSpecial(SpecialConfigs special) {
        this.special = special;
    }
}
