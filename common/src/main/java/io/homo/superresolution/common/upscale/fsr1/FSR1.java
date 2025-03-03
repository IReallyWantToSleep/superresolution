package io.homo.superresolution.common.upscale.fsr1;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.shader.ComputeShaderProgram;
import io.homo.superresolution.common.render.gl.shader.GeneralShaderProgram;
import io.homo.superresolution.common.render.gl.texture.Texture;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.AlgorithmType;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.utils.AlgorithmHelper;
import io.homo.superresolution.common.utils.FileReadHelper;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class FSR1 extends AbstractAlgorithm {
    private ComputeShaderProgram fsr1EASUShader;
    private ComputeShaderProgram fsr1RCASShader;
    private Texture fsr1TempTexture;
    private Texture output;

    public static int checkFP16Support() {
        if (AlgorithmHelper.hasGLExtension("GL_EXT_shader_16bit_storage") &&
                AlgorithmHelper.hasGLExtension("GL_EXT_shader_explicit_arithmetic_types")
        ) {
            return 1;
        }
        if (AlgorithmHelper.hasGLExtension("GL_NV_gpu_shader5")) {
            return 2;
        }
        return 0;
    }

    public static FSR1 create() {
        return new FSR1();
    }

    public void initShader() {
        int fp16 = Config.getInstance().getSpecial().fsr1.fp16 ? checkFP16Support() : 0;
        GeneralShaderProgram.ShaderInclude fsrEasuInclude = GeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_easu.comp.glsl"),
                "fsr1_easu.comp.glsl"
        );
        GeneralShaderProgram.ShaderInclude fsrEasuFp16Include = GeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_easu_fp16.comp.glsl"),
                "fsr1_easu_fp16.comp.glsl"
        );
        GeneralShaderProgram.ShaderInclude fsrRcasInclude = GeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_rcas.comp.glsl"),
                "fsr1_rcas.comp.glsl"
        );
        GeneralShaderProgram.ShaderInclude fsrRcasFp16Include = GeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_rcas_fp16.comp.glsl"),
                "fsr1_rcas_fp16.comp.glsl"
        );
        GeneralShaderProgram.ShaderInclude fsrCommonInclude = GeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_common.glsl"),
                "fsr1_common.glsl"
        );
        fsr1EASUShader = (ComputeShaderProgram) ComputeShaderProgram.create()
                .addDefineText("FSR_FP16_CRITERIA", String.valueOf(fp16))
                .addDefineText("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                .addDefineText("FSR_EASU", String.valueOf(1))
                .setShaderName("FSR1_EASU")
                .addShaderInclude(fsrEasuInclude)
                .addShaderInclude(fsrEasuFp16Include)
                .addShaderInclude(fsrRcasInclude)
                .addShaderInclude(fsrRcasFp16Include)
                .addShaderInclude(fsrCommonInclude)
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/fsr1/fsr1_main.comp.glsl"))
                .build()
                .compileShader();
        fsr1RCASShader = (ComputeShaderProgram) ComputeShaderProgram.create()
                .addDefineText("FSR_FP16_CRITERIA", String.valueOf(fp16))
                .addDefineText("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                .addDefineText("FSR_RCAS", String.valueOf(1))
                .setShaderName("FSR1_RCAS")
                .addShaderInclude(fsrEasuInclude)
                .addShaderInclude(fsrEasuFp16Include)
                .addShaderInclude(fsrRcasInclude)
                .addShaderInclude(fsrRcasFp16Include)
                .addShaderInclude(fsrCommonInclude)
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/fsr1/fsr1_main.comp.glsl"))
                .build()
                .compileShader();
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        initShader();
        fsr1TempTexture = new Texture(AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight(), GL_RGBA8);
        output = new Texture(AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight(), GL_RGBA8);
        this.resize(AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        callEASU();
        callRCAS();
        return true;
    }

    private void setFSR1ShaderUniform(ComputeShaderProgram shaderProgram) {
        shaderProgram.setVec2("renderViewportSize", AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight());
        shaderProgram.setVec2("containerTextureSize", AlgorithmManager.helper.getRenderWidth(), AlgorithmManager.helper.getRenderHeight());
        shaderProgram.setVec2("upscaledViewportSize", AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight());
        shaderProgram.setFloat("sharpness", Config.getSharpness());
    }

    private void callEASU() {
        fsr1EASUShader.use();
        setFSR1ShaderUniform(fsr1EASUShader);
        glBindTextureUnit(0, this.input.getColorTextureId());
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

    private void callRCAS() {
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
        Texture.blitToScreen(output.width, output.height, width, height, this.output.id);
    }

    @Override
    public void destroy() {
        this.output.destroy();
        this.fsr1TempTexture.destroy();
    }

    @Override
    public void resize(int width, int height) {
        this.fsr1TempTexture.resize(width, height);
        this.output.resize(width, height);
    }

    @Override
    public int getOutputTextureId() {
        return output.id;
    }

    @Override
    public int getInputTextureId() {
        return input.getColorTextureId();
    }

    @Override
    protected boolean isSupport() {
        return AlgorithmType.FSR1.getValue().check().support();
    }
}
