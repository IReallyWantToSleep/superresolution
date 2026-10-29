/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.api.InputResourceSet;
import org.joml.Matrix4f;
import org.joml.Vector2f;

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

        float preExposure,

        InputResourceSet resources
) {
}
