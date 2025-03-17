package io.homo.superresolution.common.upscale;

import io.homo.superresolution.common.impl.Vec2;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.impl.framebuffer.MotionVectorsFrameBuffer;
import io.homo.superresolution.common.upscale.fsr1.FSR1;
import io.homo.superresolution.common.upscale.fsr2.FSR2;
import io.homo.superresolution.common.upscale.nis.NVIDIAImageScaling;
import io.homo.superresolution.common.upscale.none.None;
import io.homo.superresolution.common.upscale.sgsr.Sgsr;
import io.homo.superresolution.common.upscale.utils.AlgorithmHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

public class AlgorithmManager {
    public static AlgorithmHelper helper;
    public static AlgorithmParam param = new AlgorithmParam();
    private static MotionVectorsFrameBuffer motionVectorsFrameBuffer;

    static {
        helper = new AlgorithmHelper();
    }

    public static void destroy() {
        helper.destroy();
    }

    public static void resize(int width, int height) {
        motionVectorsFrameBuffer.resizeFrameBuffer(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
        helper.resize(width, height);
    }

    public static AbstractAlgorithm getAlgorithm(AlgorithmType type) {
        AbstractAlgorithm algo = null;
        switch (type) {
            case FSR1 -> algo = FSR1.create();
            case FSR2 -> algo = FSR2.create();
            case NIS -> algo = NVIDIAImageScaling.create();
            case SGSR -> algo = Sgsr.create();
            case NONE -> algo = None.create();
        }
        if (algo != null) {
            algo.init();
        }
        return algo;
    }

    public static boolean isSupportAlgorithm(AlgorithmType type) {
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
                        (float) camera.getPosition().x, (float) camera.getPosition().y, (float) camera.getPosition().z, // 相机位置
                        (float) (camera.getPosition().x + camera.getLookVector().x),                        // 目标点（前向方向）
                        (float) (camera.getPosition().y + camera.getLookVector().y),
                        (float) (camera.getPosition().z + camera.getLookVector().z),
                        camera.getUpVector().x, camera.getUpVector().y, camera.getUpVector().z                                         // 上向量
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
            param.lastProjectionMatrix = cur;
        } else {
            param.lastProjectionMatrix = param.currentProjectionMatrix;
        }
        param.currentProjectionMatrix = cur;
    }

    private static void setViewMatrix(Matrix4f cur) {
        if (param.lastModelViewMatrix == null) {
            param.lastModelViewMatrix = cur;
        } else {
            param.lastModelViewMatrix = param.currentModelViewMatrix;
        }
        param.currentModelViewMatrix = cur;
    }

    public static DispatchResource getDispatchResource() {
        return new DispatchResource(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
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
                motionVectorsFrameBuffer,
                new Vec2(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight()),
                new Vec2(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight())
        );
    }

    public static void init() {
        motionVectorsFrameBuffer = new MotionVectorsFrameBuffer(false);
        motionVectorsFrameBuffer.setClearColor(0, 0, 0, 1);
    }

    public static void update() {
        MotionVectorsGenerator.update(getDispatchResource(), motionVectorsFrameBuffer);
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
