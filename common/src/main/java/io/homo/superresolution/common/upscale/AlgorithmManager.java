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
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import io.homo.superresolution.common.perf.PerformanceTracker;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.thirdparty.fsr2.common.Fsr2Utils;
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
        #if MC_VER > MC_1_21_10
        Matrix4f viewMatrix = new Matrix4f()
                .lookAt(
                        (float) camera.position().x, (float) camera.position().y, (float) camera.position().z,
                        (float) (camera.position().x + camera.forwardVector().x()),
                        (float) (camera.position().y + camera.forwardVector().y()),
                        (float) (camera.position().z + camera.forwardVector().z()),
                        camera.upVector().x(), camera.upVector().y(), camera.upVector().z()
                );
        #else
        Matrix4f viewMatrix = new Matrix4f()
                .lookAt(
                        (float) camera.getPosition().x, (float) camera.getPosition().y, (float) camera.getPosition().z,
                        (float) (camera.getPosition().x + camera.getLookVector().x),
                        (float) (camera.getPosition().y + camera.getLookVector().y),
                        (float) (camera.getPosition().z + camera.getLookVector().z),
                        camera.getUpVector().x, camera.getUpVector().y, camera.getUpVector().z
                );
        #endif
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

    public static Vector2f getPreviousJitterOffset() {
        if (SuperResolutionAPI.getCurrentAlgorithm() != null && SuperResolutionAPI.getCurrentAlgorithm().isSupportJitter()) {
            return Fsr2Utils.ffxFsr2GetJitterOffset(
                    RenderHandlerManager.getFrameCount() - 1,
                    Fsr2Utils.ffxFsr2GetJitterPhaseCount(
                            RenderHandlerManager.getRenderWidth(),
                            RenderHandlerManager.getScreenWidth()
                    )
            );
        }
        return new Vector2f(0);
    }

    public static Vector2f getJitterOffset() {
        if (SuperResolutionAPI.getCurrentAlgorithm() != null && SuperResolutionAPI.getCurrentAlgorithm().isSupportJitter()) {
            return Fsr2Utils.ffxFsr2GetJitterOffset(
                    RenderHandlerManager.getFrameCount(),
                    Fsr2Utils.ffxFsr2GetJitterPhaseCount(
                            RenderHandlerManager.getRenderWidth(),
                            RenderHandlerManager.getScreenWidth()
                    )
            );
        }
        return new Vector2f(0);
    }

    public static int getJitterSequenceLength() {
        if (SuperResolutionAPI.getCurrentAlgorithm() != null && SuperResolutionAPI.getCurrentAlgorithm().isSupportJitter()) {
            return Fsr2Utils.ffxFsr2GetJitterPhaseCount(
                    RenderHandlerManager.getRenderWidth(),
                    RenderHandlerManager.getScreenWidth()
            );
        }
        return 0;
    }

    public static DispatchResource getDispatchResource(
            ITexture color,
            ITexture depth,
            ITexture motionVectors,
            Vector2f jitterOffset,
            int jitterSequenceLength
    ) {
        return new DispatchResource(
                RenderHandlerManager.getRenderWidth(),
                RenderHandlerManager.getRenderHeight(),
                new Vector2f(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight()),

                RenderHandlerManager.getScreenWidth(),
                RenderHandlerManager.getScreenHeight(),
                new Vector2f(RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight()),

                RenderHandlerManager.getFrameCount(),
                PerformanceTracker.getLastResultCPU("Frame"),
                (float) param.verticalFov,
                (float) Math.tan(param.verticalFov / 2.0) * RenderHandlerManager.getRenderWidth() / RenderHandlerManager.getRenderHeight(),
                0.05F,
                Minecraft.getInstance().gameRenderer.getDepthFar(),
                jitterOffset,
                jitterSequenceLength,
                param.currentModelViewMatrix,
                param.currentProjectionMatrix,
                param.currentModelViewProjectionMatrix,
                param.currentViewMatrix,

                param.lastModelViewMatrix,
                param.lastProjectionMatrix,
                param.lastModelViewProjectionMatrix,
                param.lastViewMatrix,

                1.0f,
                ShaderCompatHandler.getCurrentLevelCompatConfig()
                        .map(p -> p.upscale.isHdrInput)
                        .orElse(false),

                new InputResourceSet(
                        color,
                        depth,
                        motionVectors == null ?
                                getMotionVectorsFrameBuffer() == null ?
                                        null :
                                        getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color) :
                                motionVectors,
                        null
                )
        );
    }

    public static void init() {
        MotionVectorsGenerator.init();
    }

    public static void update() {
    }

    public static class AlgorithmParam {
        public Matrix4f lastProjectionMatrix = new Matrix4f();
        public Matrix4f currentProjectionMatrix = new Matrix4f();
        public Matrix4f currentModelViewMatrix = new Matrix4f();
        public Matrix4f lastModelViewMatrix = new Matrix4f();
        public Matrix4f currentModelViewProjectionMatrix = new Matrix4f();
        public Matrix4f lastModelViewProjectionMatrix = new Matrix4f();
        public Matrix4f currentViewMatrix = new Matrix4f();
        public Matrix4f lastViewMatrix = new Matrix4f();

        public double verticalFov = 11.4514f;
    }
}
