package io.homo.superresolution.fsr2;

import java.util.HashMap;
import java.util.Map;

public class Fsr2ContextFlags {
    private boolean enableHighDynamicRange = false;
    private boolean enableDisplayResolutionMotionVectors = false;
    private boolean enableMotionVectorsJitterCancellation = false;
    private boolean enableDepthInverted = false;
    private boolean enableDepthInfinite = false;
    private boolean enableAutoExposure = false;
    private boolean enableDynamicResolution = false;
    private boolean enableTexture1dUsage = false;
    private boolean enableDebugChecking = false;
    private boolean allowNullDeviceAndCommandList = false;

    public boolean isEnableHighDynamicRange() {
        return enableHighDynamicRange;
    }

    public Fsr2ContextFlags enableHighDynamicRange(boolean value) {
        this.enableHighDynamicRange = value;
        return this;
    }

    public boolean isEnableDisplayResolutionMotionVectors() {
        return enableDisplayResolutionMotionVectors;
    }

    public Fsr2ContextFlags enableDisplayResolutionMotionVectors(boolean value) {
        this.enableDisplayResolutionMotionVectors = value;
        return this;
    }

    public boolean isEnableMotionVectorsJitterCancellation() {
        return enableMotionVectorsJitterCancellation;
    }

    public Fsr2ContextFlags enableMotionVectorsJitterCancellation(boolean value) {
        this.enableMotionVectorsJitterCancellation = value;
        return this;
    }

    public boolean isEnableDepthInverted() {
        return enableDepthInverted;
    }

    public Fsr2ContextFlags enableDepthInverted(boolean value) {
        this.enableDepthInverted = value;
        return this;
    }

    public boolean isEnableDepthInfinite() {
        return enableDepthInfinite;
    }

    public Fsr2ContextFlags enableDepthInfinite(boolean value) {
        this.enableDepthInfinite = value;
        return this;
    }

    public boolean isEnableAutoExposure() {
        return enableAutoExposure;
    }

    public Fsr2ContextFlags enableAutoExposure(boolean value) {
        this.enableAutoExposure = value;
        return this;
    }

    public boolean isEnableDynamicResolution() {
        return enableDynamicResolution;
    }

    public Fsr2ContextFlags enableDynamicResolution(boolean value) {
        this.enableDynamicResolution = value;
        return this;
    }

    public boolean isEnableTexture1dUsage() {
        return enableTexture1dUsage;
    }

    public Fsr2ContextFlags enableTexture1dUsage(boolean value) {
        this.enableTexture1dUsage = value;
        return this;
    }

    public boolean isEnableDebugChecking() {
        return enableDebugChecking;
    }

    public Fsr2ContextFlags enableDebugChecking(boolean value) {
        this.enableDebugChecking = value;
        return this;
    }

    public boolean isAllowNullDeviceAndCommandList() {
        return allowNullDeviceAndCommandList;
    }

    public Fsr2ContextFlags allowNullDeviceAndCommandList(boolean value) {
        this.allowNullDeviceAndCommandList = value;
        return this;
    }

    private String bool(boolean b) {
        return String.valueOf(b ? 1 : 0);
    }

    public Map<String, String> getShaderDefines() {
        Map<String, String> defines = new HashMap<>();
        defines.put("FFX_FSR2_OPTION_REPROJECT_USE_LANCZOS_TYPE", bool(false));
        defines.put("FFX_FSR2_OPTION_HDR_COLOR_INPUT", bool(isEnableHighDynamicRange()));
        defines.put("FFX_FSR2_OPTION_LOW_RESOLUTION_MOTION_VECTORS", bool(!isEnableDisplayResolutionMotionVectors()));
        defines.put("FFX_FSR2_OPTION_JITTERED_MOTION_VECTORS", bool(isEnableMotionVectorsJitterCancellation()));
        defines.put("FFX_FSR2_OPTION_INVERTED_DEPTH", bool(isEnableDepthInverted()));
        defines.put("FFX_FSR2_OPTION_APPLY_SHARPENING", bool(false));
        defines.put("FFX_FSR2_OPTION_UPSAMPLE_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_ACCUMULATE_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_REPROJECT_SAMPLERS_USE_DATA_HALF", "1");
        defines.put("FFX_FSR2_OPTION_POSTPROCESSLOCKSTATUS_SAMPLERS_USE_DATA_HALF", "0");
        defines.put("FFX_FSR2_OPTION_UPSAMPLE_USE_LANCZOS_TYPE", "2");
        defines.put("FFX_GLSL", bool(true));
        defines.put("FFX_GPU", bool(true));
        return defines;
    }
}