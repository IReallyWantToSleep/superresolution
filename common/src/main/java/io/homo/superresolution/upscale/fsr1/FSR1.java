package io.homo.superresolution.upscale.fsr1;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import io.homo.superresolution.upscale.AbstractAlgorithm;
import io.homo.superresolution.upscale.AlgorithmManager;
import io.homo.superresolution.upscale.utils.AlgorithmHelper;
import io.homo.superresolution.upscale.utils.ComputeShaderProgram;
import io.homo.superresolution.render.gl.utils.Texture;
import net.minecraft.client.Minecraft;

import static io.homo.superresolution.render.gl.GlConst.*;
import static io.homo.superresolution.render.gl.Gl.*;

public class FSR1 extends AbstractAlgorithm {
    private Texture fsr1TempTexture;
    private Texture output;
    private static ComputeShaderProgram fsr1EASUShader;
    private static ComputeShaderProgram fsr1RCASShader;
    @Override
    public void init() {
        input = ResolutionControl.getInstance().getFramebuffer();
        initShader();
        fsr1TempTexture = new Texture(AlgorithmManager.helper.renderWidth, AlgorithmManager.helper.renderHeight,GL_RGBA8);
        output = new Texture(AlgorithmManager.helper.screenWidth, AlgorithmManager.helper.screenHeight,GL_RGBA8);
        this.resize(AlgorithmManager.helper.screenWidth, AlgorithmManager.helper.screenHeight);
    }

    public static int checkFP16Support(){
        if (AlgorithmManager.helper.hasGLExtension("EXT_shader_16bit_storage") &&
                AlgorithmManager.helper.hasGLExtension("EXT_shader_explicit_arithmetic_types")
        ) {
            return 1;
        }
        if (AlgorithmManager.helper.hasGLExtension("NV_gpu_shader5")) {
            return 2;
        }
        return 0;
    }

    public static void initShader(){
        SuperResolution.LOGGER.info("正在加载FSR1着色器");
        fsr1EASUShader = ComputeShaderProgram.create()
                .addDefineText("FP16_CRITERIA", String.valueOf(checkFP16Support()))
                .setShaderName("FSR1_EASU")
                .addAllShaderTextList(AlgorithmHelper.readText("/shader/fsr1_easu.fsh"))
                .build()
                .compileShader();
        fsr1RCASShader = ComputeShaderProgram.create()
                .addDefineText("FP16_CRITERIA", String.valueOf(checkFP16Support()))
                .setShaderName("FSR1_RCAS")
                .addAllShaderTextList(AlgorithmHelper.readText("/shader/fsr1_rcas.fsh"))
                .build()
                .compileShader();
        SuperResolution.LOGGER.info("完成加载FSR1着色器");
    }

    @Override
    public boolean run(float frameTimeDelta) {
        callEASU();
        callRCAS();
        return true;
    }

    public void setVec2(int id,String name, float x, float y) {
        int location = glGetUniformLocation(id, name);
        glUniform2f(location, x, y);
    }
    public void setFloat(int id,String name, float x) {
        int location = glGetUniformLocation(id, name);
        glUniform1f(location, x);
    }

    private void setFSR1ShaderUniform(ComputeShaderProgram si){
        setVec2(si.shaderProgram,"renderViewportSize",AlgorithmManager.helper.renderWidth,AlgorithmManager.helper.renderHeight);
        setVec2(si.shaderProgram,"containerTextureSize",AlgorithmManager.helper.renderWidth,AlgorithmManager.helper.renderHeight);
        setVec2(si.shaderProgram,"upscaledViewportSize",AlgorithmManager.helper.screenWidth,AlgorithmManager.helper.screenHeight);
        setFloat(si.shaderProgram,"sharpness",0.2f);
    }

    private void callEASU(){
        fsr1EASUShader.use();
        setFSR1ShaderUniform(fsr1EASUShader);
        glBindTextureUnit(0,this.input.getColorTextureId());
        glBindImageTexture(1, this.fsr1TempTexture.id, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);
        int workRegionDim = 16;
        int dispatchX = (AlgorithmManager.helper.screenWidth + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (AlgorithmManager.helper.screenHeight + (workRegionDim - 1)) / workRegionDim;
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
        int dispatchX = (AlgorithmManager.helper.screenWidth + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (AlgorithmManager.helper.screenHeight + (workRegionDim - 1)) / workRegionDim;
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

        Texture.texBlitToScreen(width,height,this.output.id);
    }

    public static FSR1 create() {
        return new FSR1();
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
    public int getOutputTexId(){
        return output.id;
    }
}
