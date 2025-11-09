/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.perf.PerformanceRecoder;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import org.joml.Vector2f;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

public class AlgorithmManager {
    public static AlgorithmParam param = new AlgorithmParam();

    public static GlFrameBuffer getMotionVectorsFrameBuffer() {
        return (GlFrameBuffer) MotionVectorsGenerator.getMotionVectorsFrameBuffer();
    }

    public static void destroy() {

    }

    public static void resize(int width, int height) {
        MotionVectorsGenerator.resize();
    }

    public static boolean isSupportAlgorithm(AlgorithmDescription<?> type) {
        return type.getRequirement().check().support();
    }

    public static float extractVerticalFovDegrees(Matrix4f projectionMatrix) {
        float m11 = projectionMatrix.m11();
        float halfFovRad = (float) Math.atan(1.0f / m11);
        float fovDegrees = (float) Math.toDegrees(halfFovRad * 2.0f);
        return fovDegrees;
    }

    public static void setMatrixVanilla(Matrix4f proj, Matrix4f modelView) {
        setModelViewMatrix(modelView);
        setProjectionMatrix(proj);
        param.verticalFov = extractVerticalFovDegrees(proj);
        Matrix4f curViewProjectionMatrix = new Matrix4f(proj);
        curViewProjectionMatrix.mul(modelView);
        if (param.lastModelViewProjectionMatrix == null) {
            param.lastModelViewProjectionMatrix = curViewProjectionMatrix;
        } else {
            param.lastModelViewProjectionMatrix = param.currentModelViewProjectionMatrix;
        }
        param.currentModelViewProjectionMatrix = curViewProjectionMatrix;

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Matrix4f viewMatrix = new Matrix4f()
                .lookAt(
                        (float) camera.getPosition().x, (float) camera.getPosition().y, (float) camera.getPosition().z,
                        (float) (camera.getPosition().x + camera.getLookVector().x),
                        (float) (camera.getPosition().y + camera.getLookVector().y),
                        (float) (camera.getPosition().z + camera.getLookVector().z),
                        camera.getUpVector().x, camera.getUpVector().y, camera.getUpVector().z
                );
        if (param.lastViewMatrix == null) {
            param.lastViewMatrix = viewMatrix;
        } else {
            param.lastViewMatrix = param.currentViewMatrix;
        }
        param.currentViewMatrix = viewMatrix;

    }

    private static void setProjectionMatrix(Matrix4f cur) {
        if (param.lastProjectionMatrix == null) {
            param.lastProjectionMatrix = new Matrix4f(cur);
        } else {
            param.lastProjectionMatrix = param.currentProjectionMatrix;
        }
        param.currentProjectionMatrix = new Matrix4f(cur);
    }

    private static void setModelViewMatrix(Matrix4f cur) {
        if (param.lastModelViewMatrix == null) {
            param.lastModelViewMatrix = new Matrix4f(cur);
        } else {
            param.lastModelViewMatrix = param.currentModelViewMatrix;
        }
        param.currentModelViewMatrix = new Matrix4f(cur);
    }

    public static Vector2f getJitterOffset() {
        if (SuperResolutionAPI.getCurrentAlgorithm() != null) {
            Vector2f jitter = SuperResolutionAPI.getCurrentAlgorithm().getJitterOffset(
                    RenderHandlerManager.getFrameCount(),
                    new Vector2f(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()),
                    new Vector2f(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight())
            );
            return jitter;
        }
        return new Vector2f(0);
    }


    public static Matrix4f applyJitterOffset(Matrix4f proj, Vector2f jitter) {
        float jx_ndc = (2.0f * jitter.x) / RenderHandlerManager.getRenderWidth();
        float jy_ndc = (2.0f * jitter.y) / RenderHandlerManager.getRenderHeight();
        proj.m20(proj.m20() + jx_ndc);
        proj.m21(proj.m21() + jy_ndc);

        return proj;
    }


    public static DispatchResource getDispatchResource(
            ITexture color,
            ITexture depth,
            ITexture motionVectors
    ) {
        return new DispatchResource(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight(),
                new Vector2f(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()),

                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight(),
                new Vector2f(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()),

                RenderHandlerManager.getFrameCount(),
                PerformanceRecoder.getCpuFrameTimeMs(),
                (float) param.verticalFov,
                (float) Math.tan(param.verticalFov / 2.0) * RenderHandlerManager.getRenderWidth() / RenderHandlerManager.getRenderHeight(),
                0.05F,
                Minecraft.getInstance().gameRenderer.getDepthFar(),
                param.currentModelViewMatrix,
                param.currentProjectionMatrix,
                param.currentModelViewProjectionMatrix,
                param.currentViewMatrix,

                param.lastModelViewMatrix,
                param.lastProjectionMatrix,
                param.lastModelViewProjectionMatrix,
                param.lastViewMatrix,

                new InputResourceSet(
                        color,
                        depth,
                        motionVectors == null ?
                                getMotionVectorsFrameBuffer() == null ?
                                        null :
                                        getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color) :
                                motionVectors

                )
        );
    }

    public static void init() {
        MotionVectorsGenerator.init();
    }

    public static void update() {
    }

    public static class AlgorithmParam {
        public Matrix4f lastProjectionMatrix;
        public Matrix4f currentProjectionMatrix;
        public Matrix4f currentModelViewMatrix;
        public Matrix4f lastModelViewMatrix;
        public Matrix4f currentModelViewProjectionMatrix;
        public Matrix4f lastModelViewProjectionMatrix;
        public Matrix4f currentViewMatrix;
        public Matrix4f lastViewMatrix;

        public double verticalFov = 11.4514f;
    }
}
