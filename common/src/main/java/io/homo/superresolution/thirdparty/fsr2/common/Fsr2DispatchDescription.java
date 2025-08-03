package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.math.Vector2f;

public class Fsr2DispatchDescription {
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

    public ICommandBuffer commandBuffer() {
        return commandBuffer;
    }

    public Fsr2DispatchDescription setCommandBuffer(ICommandBuffer commandBuffer) {
        this.commandBuffer = commandBuffer;
        return this;
    }

    public ICommandBuffer commandBuffer;

    public static Fsr2DispatchDescription create() {
        return new Fsr2DispatchDescription();
    }

    public ITexture color() {
        return color;
    }

    public Fsr2DispatchDescription setColor(ITexture color) {
        this.color = color;
        return this;
    }

    public ITexture depth() {
        return depth;
    }

    public Fsr2DispatchDescription setDepth(ITexture depth) {
        this.depth = depth;
        return this;
    }

    public ITexture motionVectors() {
        return motionVectors;
    }

    public Fsr2DispatchDescription setMotionVectors(ITexture motionVectors) {
        this.motionVectors = motionVectors;
        return this;
    }

    public ITexture exposure() {
        return exposure;
    }

    public Fsr2DispatchDescription setExposure(ITexture exposure) {
        this.exposure = exposure;
        return this;
    }

    public ITexture reactive() {
        return reactive;
    }

    public Fsr2DispatchDescription setReactive(ITexture reactive) {
        this.reactive = reactive;
        return this;
    }

    public ITexture transparencyAndComposition() {
        return transparencyAndComposition;
    }

    public Fsr2DispatchDescription setTransparencyAndComposition(ITexture transparencyAndComposition) {
        this.transparencyAndComposition = transparencyAndComposition;
        return this;
    }

    public ITexture output() {
        return output;
    }

    public Fsr2DispatchDescription setOutput(ITexture output) {
        this.output = output;
        return this;
    }

    public Vector2f jitterOffset() {
        return jitterOffset;
    }

    public Fsr2DispatchDescription setJitterOffset(Vector2f jitterOffset) {
        this.jitterOffset = jitterOffset;
        return this;
    }

    public Vector2f motionVectorScale() {
        return motionVectorScale;
    }

    public Fsr2DispatchDescription setMotionVectorScale(Vector2f motionVectorScale) {
        this.motionVectorScale = motionVectorScale;
        return this;
    }

    public Vector2f renderSize() {
        return renderSize;
    }

    public Fsr2DispatchDescription setRenderSize(Vector2f renderSize) {
        this.renderSize = renderSize;
        return this;
    }

    public boolean enableSharpening() {
        return enableSharpening;
    }

    public Fsr2DispatchDescription setEnableSharpening(boolean enableSharpening) {
        this.enableSharpening = enableSharpening;
        return this;
    }

    public float sharpness() {
        return sharpness;
    }

    public Fsr2DispatchDescription setSharpness(float sharpness) {
        this.sharpness = sharpness;
        return this;
    }

    public float frameTimeDelta() {
        return frameTimeDelta;
    }

    public Fsr2DispatchDescription setFrameTimeDelta(float frameTimeDelta) {
        this.frameTimeDelta = frameTimeDelta;
        return this;
    }

    public float preExposure() {
        return preExposure;
    }

    public Fsr2DispatchDescription setPreExposure(float preExposure) {
        this.preExposure = preExposure;
        return this;
    }

    public boolean reset() {
        return reset;
    }

    public Fsr2DispatchDescription setReset(boolean reset) {
        this.reset = reset;
        return this;
    }

    public float cameraNear() {
        return cameraNear;
    }

    public Fsr2DispatchDescription setCameraNear(float cameraNear) {
        this.cameraNear = cameraNear;
        return this;
    }

    public float cameraFar() {
        return cameraFar;
    }

    public Fsr2DispatchDescription setCameraFar(float cameraFar) {
        this.cameraFar = cameraFar;
        return this;
    }

    public float cameraFovAngleVertical() {
        return cameraFovAngleVertical;
    }

    public Fsr2DispatchDescription setCameraFovAngleVertical(float cameraFovAngleVertical) {
        this.cameraFovAngleVertical = cameraFovAngleVertical;
        return this;
    }

    public float viewSpaceToMetersFactor() {
        return viewSpaceToMetersFactor;
    }

    public Fsr2DispatchDescription setViewSpaceToMetersFactor(float viewSpaceToMetersFactor) {
        this.viewSpaceToMetersFactor = viewSpaceToMetersFactor;
        return this;
    }

    public boolean deviceDepthNegativeOneToOne() {
        return deviceDepthNegativeOneToOne;
    }

    public Fsr2DispatchDescription setDeviceDepthNegativeOneToOne(boolean deviceDepthNegativeOneToOne) {
        this.deviceDepthNegativeOneToOne = deviceDepthNegativeOneToOne;
        return this;
    }
}
