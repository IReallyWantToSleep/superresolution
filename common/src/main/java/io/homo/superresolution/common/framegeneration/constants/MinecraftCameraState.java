/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.framegeneration.constants;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class MinecraftCameraState {
    public static float fov;

    private MinecraftCameraState() {
    }

    public static FGConstantsBuilder.CameraFrame capture(
            int frameIndex,
            Matrix4fc projection,
            float cameraNear,
            float cameraFar,
            float aspectRatio
    ) {
        #if MC_VER == MC_26_2
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        #else
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        #endif
        Quaternionf inverseRotation = camera.rotation().conjugate(new Quaternionf());
        Matrix4f viewRotation = new Matrix4f().rotation(inverseRotation);

        #if MC_VER >= MC_26_1 && MC_VER < MC_26_2
        Vector3d position = new Vector3d(camera.position().x, camera.position().y, camera.position().z);
        Vector3f forward = new Vector3f(camera.forwardVector());
        Vector3f up = new Vector3f(camera.upVector());
        Vector3f right = new Vector3f(camera.leftVector()).negate();
        float capturedFov = camera.getFov();
        #elif MC_VER >= MC_1_21_11 && MC_VER < MC_26_1
        Vector3d position = new Vector3d(camera.position().x, camera.position().y, camera.position().z);
        Vector3f forward = new Vector3f(camera.forwardVector());
        Vector3f up = new Vector3f(camera.upVector());
        Vector3f right = new Vector3f(camera.leftVector()).negate();
        float capturedFov = fov;
        #elif MC_VER == MC_26_2
        Vector3d position = new Vector3d(camera.position().x, camera.position().y, camera.position().z);
        Vector3f forward = new Vector3f(camera.forwardVector());
        Vector3f up = new Vector3f(camera.upVector());
        Vector3f right = new Vector3f(camera.leftVector()).negate();
        float capturedFov = fov;
        #else
        Vector3d position = new Vector3d(
                camera.getPosition().x,
                camera.getPosition().y,
                camera.getPosition().z
        );
        Vector3f forward = new Vector3f(camera.getLookVector());
        Vector3f up = new Vector3f(camera.getUpVector());
        Vector3f right = new Vector3f(camera.getLeftVector()).negate();
        float capturedFov = fov;
        #endif

        return new FGConstantsBuilder.CameraFrame(
                frameIndex,
                projection,
                viewRotation,
                position,
                up,
                right,
                forward,
                cameraNear,
                cameraFar,
                (float) Math.toRadians(capturedFov),
                aspectRatio
        );
    }
}
