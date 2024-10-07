package io.homo.superresolution.upscale.fsr2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.utils.AlgorithmHelper;
import io.homo.superresolution.upscale.utils.GeneralShaderProgram;
import io.homo.superresolution.upscale.utils.NativeLibManager;
import io.homo.superresolution.render.gl.utils.MotionVectorsFrameBuffer;
import io.homo.superresolution.render.gl.utils.Texture;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static io.homo.superresolution.render.gl.GlConst.*;
import static io.homo.superresolution.render.gl.Gl.*;

public class FSR2Helper implements CanResize, CanDestroy {
    private final Texture motionVectorsTexture;
    private final MotionVectorsFrameBuffer motionVectorsFBO;
    private final GeneralShaderProgram motionVectorsShader;
    public int renderWidth;
    public int renderHeight;
    public int screenWidth;
    public int screenHeight;
    public int frameIndex = 0;
    public Jitter jitter;
    public FSR2Helper(){
        RenderSystem.assertOnRenderThread();
        motionVectorsFBO = new MotionVectorsFrameBuffer(false);
        motionVectorsTexture = new Texture((int) (SuperResolution.getMinecraftWidth()* Config.getRenderScaleFactor()), (int) (SuperResolution.getMinecraftHeight()* Config.getRenderScaleFactor()), GL_RG16F);
        motionVectorsShader = GeneralShaderProgram.create()
                .addAllFragShaderTextList(AlgorithmHelper.readText("/shader/calc_motion_vector.fsh"))
                .addAllVertShaderTextList(AlgorithmHelper.readText("/shader/calc_motion_vector.vsh"))
                .setShaderName("motionVectorsShader")
                .build()
                .compileShader();
        this.resize(SuperResolution.getMinecraftWidth(), SuperResolution.getMinecraftHeight());
    }

    public void setMatrix4(int id,String name, Matrix4f x) {
        int location = glGetUniformLocation(id, name);
        float[] data = new float[16];
        x.get(data);
        glUniformMatrix4fv(location,false, data);
    }
    public void setVec2(int id,String name, float x, float y) {
        int location = glGetUniformLocation(id, name);
        glUniform2f(location, x, y);
    }
    private void setMotionVectorsShaderUniform(){
        setMatrix4(motionVectorsShader.shaderProgram,"ProjMat", AlgorithmManager.param.currentProjectionMatrix);
        setMatrix4(motionVectorsShader.shaderProgram,"ModelViewMat",AlgorithmManager.param.poseStack.last().pose());
        setMatrix4(motionVectorsShader.shaderProgram,"lastProjMat",AlgorithmManager.param.lastProjectionMatrix);
        //setVec2(motionVectorsShader.shaderProgram,"screenSize",renderWidth,renderHeight);
        //GL42.glBindImageTexture(0, this.motionVectorsTexture.id, 0, false, 0, GL15.GL_WRITE_ONLY,GL30.GL_RG16F);
    }

    public void update() {
        RenderSystem.assertOnRenderThread();
        this.frameIndex++;
        int[] curFBO = new int[1];
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING,curFBO);
        glBindFramebuffer(GL_FRAMEBUFFER,motionVectorsFBO.frameBufferId);
        motionVectorsShader.use();
        setMotionVectorsShaderUniform();
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder.vertex(0.0, renderHeight, 0.0).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(renderWidth, renderHeight, 0.0).uv(1f, 0.0F).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(renderWidth, 0.0, 0.0).uv(1f, 1f).color(255, 255, 255, 255).endVertex();
        bufferBuilder.vertex(0.0, 0.0, 0.0).uv(0.0F, 1f).color(255, 255, 255, 255).endVertex();
        BufferUploader.draw(bufferBuilder.end());
        motionVectorsTexture.copyFromFBO(motionVectorsFBO.frameBufferId);
        glBindFramebuffer(GL_FRAMEBUFFER,curFBO[0]);

    }

    public void updateJitter() {
        RenderSystem.assertOnRenderThread();
        float[] j = getJitter();
        jitter = new Jitter();
        jitter.x = j[0];
        jitter.y = j[1];
    }


    public int getMotionVectorsTex(){
        return motionVectorsTexture.id;
    }
    public void resize(int width,int height){
        RenderSystem.assertOnRenderThread();
        screenWidth = width;
        screenHeight = height;
        renderWidth = (int) (width* Config.getRenderScaleFactor());
        renderHeight = (int) (height* Config.getRenderScaleFactor());
        this.motionVectorsTexture.resize(renderWidth,renderHeight);
        this.motionVectorsFBO.resize(renderWidth,renderHeight,Minecraft.ON_OSX);
    }
    public void destroy(){
        RenderSystem.assertOnRenderThread();
        this.motionVectorsTexture.destroy();
    }
    public float getCameraNear() {
        if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            return 0.05f;
        }
        return 4.0f;
    }

    public float getCameraFar() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gameRenderer.getDepthFar();
    }

    public float getCameraFovAngleVertical() {
        Minecraft minecraft = Minecraft.getInstance();
        double fovRadians = Math.toRadians(minecraft.options.fov().get());
        double aspectRatio = (double)screenWidth / screenHeight;
        double verticalFieldOfViewRadians = 2 * Math.atan((screenHeight / 2.0) / (Math.tan(fovRadians / 2) * aspectRatio));
        return (float)verticalFieldOfViewRadians;
    }

    private Matrix4f createTranslationMatrix(Vector3f translation) {
        Matrix4f matrix = new Matrix4f();
        matrix.setTranslation(translation);
        return matrix;
    }

    public float[] getJitter() {
        int jitterPhaseCount = NativeLibManager.fsr2api.ffxFsr2GetJitterPhaseCount(renderWidth, screenWidth);
        float[] jitterOffset = NativeLibManager.fsr2api.ffxFsr2GetJitterOffset(frameIndex, jitterPhaseCount);
        float jitterX = 2.0f * jitterOffset[0] / (float) renderWidth;
        float jitterY = -2.0f * jitterOffset[1] / (float) renderHeight;
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
