package io.homo.superresolution.srapi;

import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.math.Vector2i;

public class SRDispatchUpscaleDesc {
    long commandList;

    SRTextureResource color;
    SRTextureResource depth;
    SRTextureResource motionVectors;
    SRTextureResource exposure;
    SRTextureResource reactive;
    SRTextureResource transparencyAndComposition;
    SRTextureResource output;

    Vector2f jitterOffset;
    Vector2f motionVectorScale;
    Vector2i renderSize;
    Vector2i upscaleSize;

    float frameTimeDelta;
    boolean enableSharpening;
    float sharpness;
    float preExposure;

    float cameraNear;
    float cameraFar;
    float cameraFovAngleVertical;
    float viewSpaceToMetersFactor;

    boolean reset;
    int flags;

    public long getCommandList() {
        return commandList;
    }

    public SRDispatchUpscaleDesc setCommandList(long commandList) {
        this.commandList = commandList;
        return this;
    }

    public SRTextureResource getReactive() {
        return reactive;
    }

    public SRDispatchUpscaleDesc setReactive(SRTextureResource reactive) {
        this.reactive = reactive;
        return this;
    }

    public SRTextureResource getColor() {
        return color;
    }

    public SRDispatchUpscaleDesc setColor(SRTextureResource color) {
        this.color = color;
        return this;
    }

    public SRTextureResource getDepth() {
        return depth;
    }

    public SRDispatchUpscaleDesc setDepth(SRTextureResource depth) {
        this.depth = depth;
        return this;
    }

    public SRTextureResource getMotionVectors() {
        return motionVectors;
    }

    public SRDispatchUpscaleDesc setMotionVectors(SRTextureResource motionVectors) {
        this.motionVectors = motionVectors;
        return this;
    }

    public SRTextureResource getExposure() {
        return exposure;
    }

    public SRDispatchUpscaleDesc setExposure(SRTextureResource exposure) {
        this.exposure = exposure;
        return this;
    }

    public SRTextureResource getTransparencyAndComposition() {
        return transparencyAndComposition;
    }

    public SRDispatchUpscaleDesc setTransparencyAndComposition(SRTextureResource transparencyAndComposition) {
        this.transparencyAndComposition = transparencyAndComposition;
        return this;
    }

    public SRTextureResource getOutput() {
        return output;
    }

    public SRDispatchUpscaleDesc setOutput(SRTextureResource output) {
        this.output = output;
        return this;
    }

    public Vector2f getJitterOffset() {
        return jitterOffset;
    }

    public SRDispatchUpscaleDesc setJitterOffset(Vector2f jitterOffset) {
        this.jitterOffset = jitterOffset;
        return this;
    }

    public Vector2f getMotionVectorScale() {
        return motionVectorScale;
    }

    public SRDispatchUpscaleDesc setMotionVectorScale(Vector2f motionVectorScale) {
        this.motionVectorScale = motionVectorScale;
        return this;
    }

    public Vector2i getRenderSize() {
        return renderSize;
    }

    public SRDispatchUpscaleDesc setRenderSize(Vector2i renderSize) {
        this.renderSize = renderSize;
        return this;
    }

    public Vector2i getUpscaleSize() {
        return upscaleSize;
    }

    public SRDispatchUpscaleDesc setUpscaleSize(Vector2i upscaleSize) {
        this.upscaleSize = upscaleSize;
        return this;
    }

    public float getFrameTimeDelta() {
        return frameTimeDelta;
    }

    public SRDispatchUpscaleDesc setFrameTimeDelta(float frameTimeDelta) {
        this.frameTimeDelta = frameTimeDelta;
        return this;
    }

    public boolean isEnableSharpening() {
        return enableSharpening;
    }

    public SRDispatchUpscaleDesc setEnableSharpening(boolean enableSharpening) {
        this.enableSharpening = enableSharpening;
        return this;
    }

    public float getSharpness() {
        return sharpness;
    }

    public SRDispatchUpscaleDesc setSharpness(float sharpness) {
        this.sharpness = sharpness;
        return this;
    }

    public float getPreExposure() {
        return preExposure;
    }

    public SRDispatchUpscaleDesc setPreExposure(float preExposure) {
        this.preExposure = preExposure;
        return this;
    }

    public float getCameraNear() {
        return cameraNear;
    }

    public SRDispatchUpscaleDesc setCameraNear(float cameraNear) {
        this.cameraNear = cameraNear;
        return this;
    }

    public float getCameraFar() {
        return cameraFar;
    }

    public SRDispatchUpscaleDesc setCameraFar(float cameraFar) {
        this.cameraFar = cameraFar;
        return this;
    }

    public float getCameraFovAngleVertical() {
        return cameraFovAngleVertical;
    }

    public SRDispatchUpscaleDesc setCameraFovAngleVertical(float cameraFovAngleVertical) {
        this.cameraFovAngleVertical = cameraFovAngleVertical;
        return this;
    }

    public float getViewSpaceToMetersFactor() {
        return viewSpaceToMetersFactor;
    }

    public SRDispatchUpscaleDesc setViewSpaceToMetersFactor(float viewSpaceToMetersFactor) {
        this.viewSpaceToMetersFactor = viewSpaceToMetersFactor;
        return this;
    }

    public boolean isReset() {
        return reset;
    }

    public SRDispatchUpscaleDesc setReset(boolean reset) {
        this.reset = reset;
        return this;
    }

    public int getFlags() {
        return flags;
    }

    public SRDispatchUpscaleDesc setFlags(int flags) {
        this.flags = flags;
        return this;
    }
}
