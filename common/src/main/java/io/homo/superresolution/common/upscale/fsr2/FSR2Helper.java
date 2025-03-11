package io.homo.superresolution.common.upscale.fsr2;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.impl.Destroyable;
import io.homo.superresolution.common.impl.Resizable;
import io.homo.superresolution.common.render.impl.framebuffer.MotionVectorsFrameBuffer;
import io.homo.superresolution.common.render.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.utils.NativeLibManager;
import io.homo.superresolution.common.utils.FileReadHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static io.homo.superresolution.common.render.gl.GlConst.GL_RG16F;

public class FSR2Helper implements Resizable, Destroyable {
    private final GlTexture motionVectorsTexture;
    private final MotionVectorsFrameBuffer motionVectorsFBO;
    private final GlGeneralShaderProgram motionVectorsShader;
    public int frameIndex = 0;
    public Jitter jitter;

    public FSR2Helper() {
        RenderSystem.assertOnRenderThread();
        motionVectorsFBO = new MotionVectorsFrameBuffer(false);
        motionVectorsFBO.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        motionVectorsTexture = new GlTexture((int) (SuperResolution.getMinecraftWidth() * Config.getRenderScaleFactor()), (int) (SuperResolution.getMinecraftHeight() * Config.getRenderScaleFactor()), GL_RG16F);
        motionVectorsShader = (GlGeneralShaderProgram) GlGeneralShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/calc_motion_vector.fsh"))
                .addAllVertShaderTextList(FileReadHelper.readText("/shader/calc_motion_vector.vsh"))
                .setShaderName("motionVectorsShader")
                .build()
                .compileShader();
        this.resize(SuperResolution.getMinecraftWidth(), SuperResolution.getMinecraftHeight());
    }

    public void update() {
        RenderSystem.assertOnRenderThread();
        this.frameIndex++;
    }

    public void updateJitter() {
        RenderSystem.assertOnRenderThread();
        float[] j = getJitter();
        jitter = new Jitter();
        jitter.x = j[0];
        jitter.y = j[1];
    }


    public int getMotionVectorsTex() {
        return motionVectorsFBO.getColorTextureId();
    }

    public void resize(int width, int height) {
        RenderSystem.assertOnRenderThread();
        this.motionVectorsTexture.resize(AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight());
        this.motionVectorsFBO.resize(AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight());
    }

    public void destroy() {
        RenderSystem.assertOnRenderThread();
        this.motionVectorsTexture.destroy();
    }

    private Matrix4f createTranslationMatrix(Vector3f translation) {
        Matrix4f matrix = new Matrix4f();
        matrix.setTranslation(translation);
        return matrix;
    }

    public float[] getJitter() {
        int jitterPhaseCount = NativeLibManager.nativeApi.ffxFsr2GetJitterPhaseCount(AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getScreenWidth());
        float[] jitterOffset = NativeLibManager.nativeApi.ffxFsr2GetJitterOffset(frameIndex, jitterPhaseCount);
        float jitterX = 2.0f * jitterOffset[0] / (float) AlgorithmManager.helper.getRenderWidth();
        float jitterY = -2.0f * jitterOffset[1] / (float) AlgorithmManager.helper.getRenderHeight();
        return new float[]{jitterX, jitterY};
    }

    public Matrix4f applyJitter(Matrix4f proM) {
        Matrix4f jitterTranslationMatrix = createTranslationMatrix(new Vector3f(jitter.x, jitter.y, 0));
        return proM.mul(jitterTranslationMatrix);
    }

    public static class Jitter {
        public float x = 0.0f;
        public float y = 0.0f;
    }
}
