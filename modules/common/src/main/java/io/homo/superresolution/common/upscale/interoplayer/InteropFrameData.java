package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.common.upscale.DispatchResource;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public record InteropFrameData(
        int renderWidth,
        int renderHeight,
        Vector2f renderSize,
        int screenWidth,
        int screenHeight,
        Vector2f screenSize,
        int frameCount,
        float frameTimeDelta,
        float verticalFov,
        float horizontalFov,
        float cameraNear,
        float cameraFar,
        Vector2f jitterOffset,
        int jitterSequenceLength,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        Matrix4f modelViewProjectionMatrix,
        Matrix4f viewMatrix,
        Matrix4f lastModelViewMatrix,
        Matrix4f lastProjectionMatrix,
        Matrix4f lastModelViewProjectionMatrix,
        Matrix4f lastViewMatrix,
        float preExposure
) {
    public static InteropFrameData from(DispatchResource resource) {
        return new InteropFrameData(
                resource.renderWidth(),
                resource.renderHeight(),
                new Vector2f(resource.renderSize()),
                resource.screenWidth(),
                resource.screenHeight(),
                new Vector2f(resource.screenSize()),
                resource.frameCount(),
                resource.frameTimeDelta(),
                resource.verticalFov(),
                resource.horizontalFov(),
                resource.cameraNear(),
                resource.cameraFar(),
                new Vector2f(resource.jitterOffset()),
                resource.jitterSequenceLength(),
                new Matrix4f(resource.modelViewMatrix()),
                new Matrix4f(resource.projectionMatrix()),
                new Matrix4f(resource.modelViewProjectionMatrix()),
                new Matrix4f(resource.viewMatrix()),
                new Matrix4f(resource.lastModelViewMatrix()),
                new Matrix4f(resource.lastProjectionMatrix()),
                new Matrix4f(resource.lastModelViewProjectionMatrix()),
                new Matrix4f(resource.lastViewMatrix()),
                resource.preExposure()
        );
    }
}
