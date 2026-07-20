/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.common.framegeneration.constants;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class MinecraftCameraState {
    public static float fov;
    #if MC_VER >= MC_26_1 && MC_VER < MC_26_2
    public static float getFov() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getFov();
    }

    public static Vector3d getPosition() {
        return new Vector3d(
                Minecraft.getInstance().gameRenderer.getMainCamera().position().x,
                Minecraft.getInstance().gameRenderer.getMainCamera().position().y,
                Minecraft.getInstance().gameRenderer.getMainCamera().position().z
        );
    }

    public static Vector3f getLookVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector());
    }

    public static Vector3f getUpVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().upVector());
    }

    public static Vector3f getLeftVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().leftVector());
    }

    public static Matrix4f getViewRotationMatrix() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var rotation = camera.rotation();
        Quaternionf inverseRotation = rotation.conjugate(new Quaternionf());
        return new Matrix4f().rotation(inverseRotation);
    }
    #elif MC_VER >= MC_1_21_11 && MC_VER < MC_26_1
    public static float getFov() {
        return fov;
    }

    public static Vector3d getPosition() {
        return new Vector3d(
                Minecraft.getInstance().gameRenderer.getMainCamera().position().x,
                Minecraft.getInstance().gameRenderer.getMainCamera().position().y,
                Minecraft.getInstance().gameRenderer.getMainCamera().position().z
        );
    }

    public static Vector3f getLookVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().forwardVector());
    }

    public static Vector3f getUpVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().upVector());
    }

    public static Vector3f getLeftVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().leftVector());
    }

    public static Matrix4f getViewRotationMatrix() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var rotation = camera.rotation();
        Quaternionf inverseRotation = rotation.conjugate(new Quaternionf());
        return new Matrix4f().rotation(inverseRotation);
    }
    #elif MC_VER >= MC_1_21 && MC_VER < MC_1_21_2
    public static float getFov() {
        return fov;
    }

    public static Vector3d getPosition() {
        return new Vector3d(
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z
        );
    }

    public static Vector3f getLookVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector());
    }

    public static Vector3f getUpVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getUpVector());
    }

    public static Vector3f getLeftVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getLeftVector());
    }

    public static Matrix4f getViewRotationMatrix() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var rotation = camera.rotation();
        Quaternionf inverseRotation = rotation.conjugate(new Quaternionf());
        return new Matrix4f().rotation(inverseRotation);
    }
    #else
    public static float getFov() {
        return fov;
    }

    public static Vector3d getPosition() {
        return new Vector3d(
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y,
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z
        );
    }

    public static Vector3f getLookVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector());
    }

    public static Vector3f getUpVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getUpVector());
    }

    public static Vector3f getLeftVector() {
        return new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getLeftVector());
    }

    public static Matrix4f getViewRotationMatrix() {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var rotation = camera.rotation();
        Quaternionf inverseRotation = rotation.conjugate(new Quaternionf());
        return new Matrix4f().rotation(inverseRotation);
    }
    #endif
}
