package io.homo.superresolution.thirdparty.asr2;

public class Asr2ContextFlags {
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

    public Asr2ContextFlags enableHighDynamicRange(boolean value) {
        this.enableHighDynamicRange = value;
        return this;
    }

    public boolean isEnableDisplayResolutionMotionVectors() {
        return enableDisplayResolutionMotionVectors;
    }

    public Asr2ContextFlags enableDisplayResolutionMotionVectors(boolean value) {
        this.enableDisplayResolutionMotionVectors = value;
        return this;
    }

    public boolean isEnableMotionVectorsJitterCancellation() {
        return enableMotionVectorsJitterCancellation;
    }

    public Asr2ContextFlags enableMotionVectorsJitterCancellation(boolean value) {
        this.enableMotionVectorsJitterCancellation = value;
        return this;
    }

    public boolean isEnableDepthInverted() {
        return enableDepthInverted;
    }

    public Asr2ContextFlags enableDepthInverted(boolean value) {
        this.enableDepthInverted = value;
        return this;
    }

    public boolean isEnableDepthInfinite() {
        return enableDepthInfinite;
    }

    public Asr2ContextFlags enableDepthInfinite(boolean value) {
        this.enableDepthInfinite = value;
        return this;
    }

    public boolean isEnableAutoExposure() {
        return enableAutoExposure;
    }

    public Asr2ContextFlags enableAutoExposure(boolean value) {
        this.enableAutoExposure = value;
        return this;
    }

    public boolean isEnableDynamicResolution() {
        return enableDynamicResolution;
    }

    public Asr2ContextFlags enableDynamicResolution(boolean value) {
        this.enableDynamicResolution = value;
        return this;
    }

    public boolean isEnableTexture1dUsage() {
        return enableTexture1dUsage;
    }

    public Asr2ContextFlags enableTexture1dUsage(boolean value) {
        this.enableTexture1dUsage = value;
        return this;
    }

    public boolean isEnableDebugChecking() {
        return enableDebugChecking;
    }

    public Asr2ContextFlags enableDebugChecking(boolean value) {
        this.enableDebugChecking = value;
        return this;
    }

    public boolean isAllowNullDeviceAndCommandList() {
        return allowNullDeviceAndCommandList;
    }

    public Asr2ContextFlags allowNullDeviceAndCommandList(boolean value) {
        this.allowNullDeviceAndCommandList = value;
        return this;
    }
}