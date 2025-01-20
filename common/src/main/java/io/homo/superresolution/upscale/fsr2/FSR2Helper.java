package io.homo.superresolution.upscale.fsr2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.impl.Destroyable;
import io.homo.superresolution.impl.Resizable;
import io.homo.superresolution.render.gl.framebuffer.MotionVectorsFrameBuffer;
import io.homo.superresolution.render.gl.shader.GeneralShaderProgram;
import io.homo.superresolution.render.gl.texture.Texture;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.utils.AlgorithmHelper;
import io.homo.superresolution.upscale.utils.NativeLibManager;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static io.homo.superresolution.render.gl.Gl.glBindFramebuffer;
import static io.homo.superresolution.render.gl.GlConst.GL_FRAMEBUFFER;
import static io.homo.superresolution.render.gl.GlConst.GL_RG16F;

public class FSR2Helper implements Resizable, Destroyable {
    private final Texture motionVectorsTexture;
    private final MotionVectorsFrameBuffer motionVectorsFBO;
    private final GeneralShaderProgram motionVectorsShader;
    public int frameIndex = 0;
    public Jitter jitter;
    public FSR2Helper(){
        RenderSystem.assertOnRenderThread();
        motionVectorsFBO = new MotionVectorsFrameBuffer(false);
        motionVectorsFBO.setClearColor(0.0f,0.0f,0.0f,1.0f);
        motionVectorsTexture = new Texture((int) (SuperResolution.getMinecraftWidth()* Config.getRenderScaleFactor()), (int) (SuperResolution.getMinecraftHeight()* Config.getRenderScaleFactor()), GL_RG16F);
        motionVectorsShader = (GeneralShaderProgram) GeneralShaderProgram.create()
                .addAllFragShaderTextList(AlgorithmHelper.readText("/shader/calc_motion_vector.fsh"))
                .addAllVertShaderTextList(AlgorithmHelper.readText("/shader/calc_motion_vector.vsh"))
                .setShaderName("motionVectorsShader")
                .build()
                .compileShader();
        this.resize(SuperResolution.getMinecraftWidth(), SuperResolution.getMinecraftHeight());
    }

    private void setMotionVectorsShaderUniform(){
        motionVectorsShader.setMatrix4("ProjMat", AlgorithmManager.param.currentProjectionMatrix);
        motionVectorsShader.setMatrix4("ModelViewMat",AlgorithmManager.param.currentModelViewMatrix);
        motionVectorsShader.setMatrix4("projectionInverse", AlgorithmManager.param.currentProjectionMatrix.invert());
        motionVectorsShader.setMatrix4("modelViewInverse",AlgorithmManager.param.currentModelViewMatrix.invert());
        motionVectorsShader.setMatrix4("lastProjection", AlgorithmManager.param.lastProjectionMatrix);
        motionVectorsShader.setMatrix4("lastModelView",AlgorithmManager.param.lastModelViewMatrix);
        motionVectorsShader.setVec2("pixelSize", (float) 1 /AlgorithmManager.helper.getRenderWidth(), (float) 1 /AlgorithmManager.helper.getRenderHeight());
        motionVectorsShader.setFloat("depth",0.5f);
    }

    public void update() {
        RenderSystem.assertOnRenderThread();
        this.frameIndex++;
        motionVectorsFBO.clear(Minecraft.ON_OSX);
        glBindFramebuffer(GL_FRAMEBUFFER,motionVectorsFBO.frameBufferId);
        motionVectorsShader.use();
        setMotionVectorsShaderUniform();
        BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
        bufferBuilder.addVertex(0.0F, 0.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 0.0F, 0.0F);
        bufferBuilder.addVertex(1.0F, 1.0F, 0.0F);
        bufferBuilder.addVertex(0.0F, 1.0F, 0.0F);
        BufferUploader.draw(bufferBuilder.buildOrThrow());
    }

    public void updateJitter() {
        RenderSystem.assertOnRenderThread();
        float[] j = getJitter();
        jitter = new Jitter();
        jitter.x = j[0];
        jitter.y = j[1];
    }


    public int getMotionVectorsTex(){
        return motionVectorsFBO.getColorTextureId();
    }
    public void resize(int width,int height){
        RenderSystem.assertOnRenderThread();
        this.motionVectorsTexture.resize(AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight());
        this.motionVectorsFBO.resize(AlgorithmManager.helper.getRenderWidth(),AlgorithmManager.helper.getRenderHeight(),Minecraft.ON_OSX);
    }
    public void destroy(){
        RenderSystem.assertOnRenderThread();
        this.motionVectorsTexture.destroy();
    }
    public float getCameraNear() {
        return 0.1f;
    }

    public float getCameraFar() {
        return 100f;
    }

    public float getCameraFovAngleVertical() {
        return (float) AlgorithmManager.param.fov * 0.0174532925199433f;
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
