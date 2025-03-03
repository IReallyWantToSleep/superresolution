package io.homo.superresolution.common.upscale;

import org.joml.Matrix4f;

public record DispatchResource(
        int renderWidth,
        int renderHeight,
        int screenWidth,
        int screenHeight,
        float frameTimeDelta,
        float verticalFov,
        float horizontalFov,
        float cameraNear,
        float cameraFar,

        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        Matrix4f modelViewProjectionMatrix,
        Matrix4f lastModelViewMatrix,
        Matrix4f lastProjectionMatrix,
        Matrix4f lastModelViewProjectionMatrix
) {
}
