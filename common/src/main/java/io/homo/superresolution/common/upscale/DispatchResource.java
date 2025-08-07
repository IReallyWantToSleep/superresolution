package io.homo.superresolution.common.upscale;

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import org.joml.Matrix4f;

public record DispatchResource(
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

        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        Matrix4f modelViewProjectionMatrix,
        Matrix4f viewMatrix,

        Matrix4f lastModelViewMatrix,
        Matrix4f lastProjectionMatrix,
        Matrix4f lastModelViewProjectionMatrix,
        Matrix4f lastViewMatrix,
        InputResourceSet resources
) {
}
