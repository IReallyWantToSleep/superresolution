package io.homo.superresolution.common.config;

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.special.SpecialConfigs;
import io.homo.superresolution.common.config.enums.CaptureMode;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;

public class ConfigData {
    private static final OSType CURRENT_OS_TYPE = new OS().type;
    private static final boolean compatMode = CURRENT_OS_TYPE == OSType.MACOS || CURRENT_OS_TYPE == OSType.ANDROID;
    private SpecialConfigs special = new SpecialConfigs();
    private boolean enableUpscale = true;
    private float upscaleRatio = 1.7f;
    private AlgorithmDescription<?> upscaleAlgo = AlgorithmDescriptions.SGSR1;
    private float sharpness = 0.55f;
    private CaptureMode captureMode = CaptureMode.A;
    private boolean debugDumpShader = false;
    private boolean skipInitVulkan;
    private boolean enableRenderDoc;
    private boolean enableImgui;
    private boolean generateMotionVectors;
    private boolean pauseGameOnGui;

    public ConfigData() {
        skipInitVulkan = compatMode;
        enableRenderDoc = !compatMode;
        enableImgui = !compatMode;
    }

    public static AlgorithmDescription<?> getDefaultAlgorithm() {
        AlgorithmDescription<?> desc = null;
        for (AlgorithmDescription<?> algorithmDescription : AlgorithmRegistry.getAlgorithmMap().values()) {
            if (algorithmDescription.requirement.check().support()) {
                desc = algorithmDescription;
                break;
            }
        }
        if (desc == null) {
            SuperResolution.LOGGER.info("你的硬件不支持所有算法????"); //最逆天的一集
            return AlgorithmDescriptions.NONE;
        }
        return desc;
    }

    public boolean isPauseGameOnGui() {
        return pauseGameOnGui;
    }

    public ConfigData setPauseGameOnGui(boolean pauseGameOnGui) {
        this.pauseGameOnGui = pauseGameOnGui;
        return this;
    }

    public boolean isGenerateMotionVectors() {
        return generateMotionVectors;
    }

    public ConfigData setGenerateMotionVectors(boolean generateMotionVectors) {
        this.generateMotionVectors = generateMotionVectors;
        return this;
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
        if (this.upscaleAlgo == null) {
            upscaleAlgo = getDefaultAlgorithm();
        }
        return this.upscaleAlgo;
    }

    public void setUpscaleAlgo(AlgorithmDescription<?> newAlgo) {
        if (newAlgo == null) {
            newAlgo = getDefaultAlgorithm();
        }
        if (!newAlgo.requirement.check().support() && !Platform.currentPlatform.isDevelopmentEnvironment()) {
            SuperResolution.LOGGER.warn("算法 {} 不支持，回退到默认算法", newAlgo.displayName);
            newAlgo = getDefaultAlgorithm();
        }
        if (this.upscaleAlgo == newAlgo) return;
        AlgorithmDescription<?> previousAlgo = this.upscaleAlgo;
        this.upscaleAlgo = newAlgo;


        SuperResolution.algorithmDescription = this.upscaleAlgo;
        if (SuperResolution.currentAlgorithm != null) {
            SuperResolution.currentAlgorithm.destroy();
        }
        if (!SuperResolution.createAlgo()) {
            this.upscaleAlgo = previousAlgo;
            SuperResolution.algorithmDescription = this.upscaleAlgo;
            if (!SuperResolution.createAlgo()) {
                SuperResolution.LOGGER.error("在初始化算法 {} 时失败后在回退到算法 {} 时又发生异常", upscaleAlgo.displayName, previousAlgo.displayName);
                throw new RuntimeException();
            } else {
                SuperResolution.LOGGER.error("初始化算法 {} 失败，已回退到算法 {}", upscaleAlgo.displayName, previousAlgo.displayName);
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
