package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.impl.Vec2;
import io.homo.superresolution.common.render.impl.framebuffer.MotionVectorsFrameBuffer;
import org.joml.Matrix4f;

public record DispatchResource(
        int renderWidth,
        int renderHeight,
        int screenWidth,
        int screenHeight,
        int frameCount,
        float frameTimeDelta,
        float verticalFov,
        float horizontalFov,
        float cameraNear,
        float cameraFar,

        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        Matrix4f modelViewProjectionMatrix,
        Matrix4f viewMatrix,
        Matrix4f lastModelViewMatrix,
        Matrix4f lastProjectionMatrix,
        Matrix4f lastModelViewProjectionMatrix,
        Matrix4f lastViewMatrix,
        MotionVectorsFrameBuffer motionVectors,

        Vec2 renderSize,
        Vec2 screenSize


) {
}
