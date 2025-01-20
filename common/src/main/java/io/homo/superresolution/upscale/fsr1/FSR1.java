package io.homo.superresolution.upscale.fsr1;

import io.homo.superresolution.config.Config;
import io.homo.superresolution.render.MinecraftRenderingStates;
import io.homo.superresolution.render.gl.shader.ComputeShaderProgram;
import io.homo.superresolution.render.gl.texture.Texture;
import io.homo.superresolution.upscale.AbstractAlgorithm;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.utils.AlgorithmHelper;
import net.minecraft.client.Minecraft;

import static io.homo.superresolution.render.gl.Gl.*;
import static io.homo.superresolution.render.gl.GlConst.*;

public class FSR1 extends AbstractAlgorithm {
    private static ComputeShaderProgram fsr1EASUShader;
    private static ComputeShaderProgram fsr1RCASShader;
    private Texture fsr1TempTexture;
    private Texture output;

    public static int checkFP16Support(){
        if (AlgorithmHelper.hasGLExtension("EXT_shader_16bit_storage") &&
                AlgorithmHelper.hasGLExtension("EXT_shader_explicit_arithmetic_types")
        ) {
            return 1;
        }
        if (AlgorithmHelper.hasGLExtension("NV_gpu_shader5")) {
            return 2;
        }
        return 0;
    }

    public static void initShader(){
        fsr1EASUShader = (ComputeShaderProgram) ComputeShaderProgram.create()
                .addDefineText("FP16_CRITERIA", String.valueOf(checkFP16Support()))
                .setShaderName("FSR1_EASU")
                .addAllFragShaderTextList(AlgorithmHelper.readText("/shader/fsr1_easu.fsh"))
                .build()
                .compileShader();
        fsr1RCASShader = (ComputeShaderProgram) ComputeShaderProgram.create()
                .addDefineText("FP16_CRITERIA", String.valueOf(checkFP16Support()))
                .setShaderName("FSR1_RCAS")
                .addAllFragShaderTextList(AlgorithmHelper.readText("/shader/fsr1_rcas.fsh"))
                .build()
                .compileShader();
    }

    public static FSR1 create() {
        return new FSR1();
    }

    @Override
    public void init() {
        input = MinecraftRenderingStates.getRenderTarget();
        initShader();
        fsr1TempTexture = new Texture(AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight(),GL_RGBA8);
        output = new Texture(AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight(),GL_RGBA8);
        this.resize(AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight());
    }

    @Override
    public boolean dispatch(float frameTimeDelta) {
        callEASU();
        callRCAS();
        return true;
    }

    private void setFSR1ShaderUniform(ComputeShaderProgram shaderProgram){
        shaderProgram.setVec2("renderViewportSize",AlgorithmManager.helper.getRenderWidth(),AlgorithmManager.helper.getRenderHeight());
        shaderProgram.setVec2("containerTextureSize",AlgorithmManager.helper.getRenderWidth(),AlgorithmManager.helper.getRenderHeight());
        shaderProgram.setVec2("upscaledViewportSize",AlgorithmManager.helper.getScreenWidth(),AlgorithmManager.helper.getScreenHeight());
        shaderProgram.setFloat("sharpness", Config.getSharpness());
    }

    private void callEASU(){
        fsr1EASUShader.use();
        setFSR1ShaderUniform(fsr1EASUShader);
        glBindTextureUnit(0,this.input.getColorTextureId());
        glBindImageTexture(1, this.fsr1TempTexture.id, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);
        int workRegionDim = 16;
        int dispatchX = (AlgorithmManager.helper.getScreenWidth() + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (AlgorithmManager.helper.getScreenHeight() + (workRegionDim - 1)) / workRegionDim;
        glDispatchCompute(
                dispatchX,
                dispatchY,
                1
        );
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        fsr1EASUShader.clear();
    }

    private void callRCAS(){
        fsr1RCASShader.use();
        setFSR1ShaderUniform(fsr1RCASShader);
        glBindImageTexture(0, this.fsr1TempTexture.id, 0, false, 0, GL_READ_ONLY, GL_RGBA8);
        glBindImageTexture(1, this.output.id, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);
        int workRegionDim = 16;
        int dispatchX = (AlgorithmManager.helper.getScreenWidth() + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (AlgorithmManager.helper.getScreenHeight() + (workRegionDim - 1)) / workRegionDim;
        glDispatchCompute(
                dispatchX,
                dispatchY,
                1
        );
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        fsr1RCASShader.clear();
    }

    @Override
    public void blitToScreen(int width, int height) {
        Texture.blitToScreen(output.width,output.height,width,height,this.output.id);
    }

    @Override
    public void destroy() {
        this.output.destroy();
        this.fsr1TempTexture.destroy();
    }

    @Override
    public void resize(int width, int height) {
        this.input.resize(width,height, Minecraft.ON_OSX);
        this.fsr1TempTexture.resize(width,height);
        this.output.resize(width,height);
    }

    @Override
    public int getOutputTextureId(){
        return output.id;
    }
}
