package io.homo.superresolution.common.upscale.sgsr.variants;

import io.homo.superresolution.common.render.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.AbstractSgsrVariant;
import io.homo.superresolution.common.upscale.sgsr.Sgsr;
import io.homo.superresolution.common.upscale.sgsr.SgsrUtils;
import io.homo.superresolution.common.utils.FileReadHelper;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.Gl.glMemoryBarrier;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class Sgsr2PassCompute extends AbstractSgsrVariant {
    private GlComputeShaderProgram convertShader1;
    private GlComputeShaderProgram upscaleShader2;

    @Override
    public void dispatch(DispatchResource resource, Sgsr sgsr) {

    }

    @Override
    public void init(Sgsr sgsr) {
        convertShader1 = (GlComputeShaderProgram) GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("sgsr2_convert.comp.glsl"))
                .setShaderName("SGSR_2PCS_A")
                .build()
                .compileShader();
        upscaleShader2 = (GlComputeShaderProgram) GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("sgsr2_upscale.comp.glsl"))
                .setShaderName("SGSR_2PCS_B")
                .build()
                .compileShader();
    }

    private void pass1(DispatchResource resource, Sgsr sgsr) {
        convertShader1.use();
        int dispatchX = SgsrUtils.divideRoundUp(resource.screenWidth(), 8);
        int dispatchY = SgsrUtils.divideRoundUp(resource.screenHeight(), 8);
        glDispatchCompute(
                dispatchX,
                dispatchY,
                1
        );
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

    }

    private void pass2(DispatchResource resource, Sgsr sgsr) {

    }

    @Override
    public void destroy() {
        convertShader1.destroy();
        upscaleShader2.destroy();
    }

    @Override
    public void resize(int width, int height) {

    }
}
