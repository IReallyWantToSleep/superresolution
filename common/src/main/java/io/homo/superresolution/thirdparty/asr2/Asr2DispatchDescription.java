package io.homo.superresolution.thirdparty.asr2;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.math.Vector2f;

public class Asr2DispatchDescription {
    public ITexture color;
    public ITexture depth;
    public ITexture motionVectors;
    public ITexture exposure;
    public ITexture reactive;
    public ITexture transparencyAndComposition;
    public ITexture output;
    public Vector2f jitterOffset;
    public Vector2f motionVectorScale;
    public Vector2f renderSize;
    public boolean enableSharpening;
    public float sharpness;
    public float frameTimeDelta;
    public float preExposure;
    public boolean reset;
    public float cameraNear;
    public float cameraFar;
    public float cameraFovAngleVertical;
    public float viewSpaceToMetersFactor;
    public boolean deviceDepthNegativeOneToOne;

    public static Asr2DispatchDescription create() {
        return new Asr2DispatchDescription();
    }

    public ITexture color() {
        return color;
    }

    public Asr2DispatchDescription setColor(ITexture color) {
        this.color = color;
        return this;
    }

    public ITexture depth() {
        return depth;
    }

    public Asr2DispatchDescription setDepth(ITexture depth) {
        this.depth = depth;
        return this;
    }

    public ITexture motionVectors() {
        return motionVectors;
    }

    public Asr2DispatchDescription setMotionVectors(ITexture motionVectors) {
        this.motionVectors = motionVectors;
        return this;
    }

    public ITexture exposure() {
        return exposure;
    }

    public Asr2DispatchDescription setExposure(ITexture exposure) {
        this.exposure = exposure;
        return this;
    }

    public ITexture reactive() {
        return reactive;
    }

    public Asr2DispatchDescription setReactive(ITexture reactive) {
        this.reactive = reactive;
        return this;
    }

    public ITexture transparencyAndComposition() {
        return transparencyAndComposition;
    }

    public Asr2DispatchDescription setTransparencyAndComposition(ITexture transparencyAndComposition) {
        this.transparencyAndComposition = transparencyAndComposition;
        return this;
    }

    public ITexture output() {
        return output;
    }

    public Asr2DispatchDescription setOutput(ITexture output) {
        this.output = output;
        return this;
    }

    public Vector2f jitterOffset() {
        return jitterOffset;
    }

    public Asr2DispatchDescription setJitterOffset(Vector2f jitterOffset) {
        this.jitterOffset = jitterOffset;
        return this;
    }

    public Vector2f motionVectorScale() {
        return motionVectorScale;
    }

    public Asr2DispatchDescription setMotionVectorScale(Vector2f motionVectorScale) {
        this.motionVectorScale = motionVectorScale;
        return this;
    }

    public Vector2f renderSize() {
        return renderSize;
    }

    public Asr2DispatchDescription setRenderSize(Vector2f renderSize) {
        this.renderSize = renderSize;
        return this;
    }

    public boolean enableSharpening() {
        return enableSharpening;
    }

    public Asr2DispatchDescription setEnableSharpening(boolean enableSharpening) {
        this.enableSharpening = enableSharpening;
        return this;
    }

    public float sharpness() {
        return sharpness;
    }

    public Asr2DispatchDescription setSharpness(float sharpness) {
        this.sharpness = sharpness;
        return this;
    }

    public float frameTimeDelta() {
        return frameTimeDelta;
    }

    public Asr2DispatchDescription setFrameTimeDelta(float frameTimeDelta) {
        this.frameTimeDelta = frameTimeDelta;
        return this;
    }

    public float preExposure() {
        return preExposure;
    }

    public Asr2DispatchDescription setPreExposure(float preExposure) {
        this.preExposure = preExposure;
        return this;
    }

    public boolean reset() {
        return reset;
    }

    public Asr2DispatchDescription setReset(boolean reset) {
        this.reset = reset;
        return this;
    }

    public float cameraNear() {
        return cameraNear;
    }

    public Asr2DispatchDescription setCameraNear(float cameraNear) {
        this.cameraNear = cameraNear;
        return this;
    }

    public float cameraFar() {
        return cameraFar;
    }

    public Asr2DispatchDescription setCameraFar(float cameraFar) {
        this.cameraFar = cameraFar;
        return this;
    }

    public float cameraFovAngleVertical() {
        return cameraFovAngleVertical;
    }

    public Asr2DispatchDescription setCameraFovAngleVertical(float cameraFovAngleVertical) {
        this.cameraFovAngleVertical = cameraFovAngleVertical;
        return this;
    }

    public float viewSpaceToMetersFactor() {
        return viewSpaceToMetersFactor;
    }

    public Asr2DispatchDescription setViewSpaceToMetersFactor(float viewSpaceToMetersFactor) {
        this.viewSpaceToMetersFactor = viewSpaceToMetersFactor;
        return this;
    }

    public boolean deviceDepthNegativeOneToOne() {
        return deviceDepthNegativeOneToOne;
    }

    public Asr2DispatchDescription setDeviceDepthNegativeOneToOne(boolean deviceDepthNegativeOneToOne) {
        this.deviceDepthNegativeOneToOne = deviceDepthNegativeOneToOne;
        return this;
    }
}
