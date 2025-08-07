package io.homo.superresolution.common.upscale;

import io.homo.superresolution.api.InputResourceSet;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
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

    public static void setMatrix(Matrix4f proj, Matrix4f view) {
        setViewMatrix(view);
        setProjectionMatrix(proj);
        Matrix4f curViewProjectionMatrix = new Matrix4f(proj);
        curViewProjectionMatrix.mul(view);
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

    private static void setViewMatrix(Matrix4f cur) {
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
                    MinecraftRenderHandle.getFrameCount(),
                    new Vector2f(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight()),
                    new Vector2f(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight())
            );
            return jitter;
        }
        return new Vector2f(0);
    }


    public static Matrix4f applyJitterOffset(Matrix4f proj, Vector2f jitter) {
        proj.m20(proj.m20() + jitter.x);
        proj.m21(proj.m21() + jitter.y);
        return proj;
    }

    public static DispatchResource getDispatchResource(
            ITexture color,
            ITexture depth,
            ITexture motionVectors
    ) {
        return new DispatchResource(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                new Vector2f(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight()),

                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                new Vector2f(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight()),

                MinecraftRenderHandle.getFrameCount(),
                MinecraftRenderHandle.frameTime,
                (float) param.verticalFov,
                (float) Math.tan(param.verticalFov / 2.0) * MinecraftRenderHandle.getRenderWidth() / MinecraftRenderHandle.getRenderHeight(),
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
                        motionVectors == null ? getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color) : motionVectors
                )
        );
    }

    public static void init() {

        MotionVectorsGenerator.init();
    }

    public static void update() {
        getMotionVectorsFrameBuffer().clearFrameBuffer();
        if (SuperResolutionConfig.isGenerateMotionVectors()) {
            MotionVectorsGenerator.update();
        }
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
