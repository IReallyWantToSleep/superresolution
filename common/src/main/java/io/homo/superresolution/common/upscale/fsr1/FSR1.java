package io.homo.superresolution.common.upscale.fsr1;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.pipeline.*;
import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.core.gl.texture.GlTexture;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.texture.TextureFrameBufferAdapter;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.utils.FileReadHelper;

public class FSR1 extends AbstractAlgorithm {
    private GlComputeShaderProgram fsr1EASUShader;
    private GlComputeShaderProgram fsr1RCASShader;
    private GlPipeline fsrUpscalePipeline;
    private GlTexture fsr1TempTexture;
    private GlTexture output;

    public static int checkFP16Support() {
        if (GraphicsCapabilities.hasGLExtension("GL_EXT_shader_16bit_storage") &&
                GraphicsCapabilities.hasGLExtension("GL_EXT_shader_explicit_arithmetic_types")
        ) {
            return 1;
        }
        if (GraphicsCapabilities.hasGLExtension("GL_NV_gpu_shader5")) {
            return 2;
        }
        return 0;
    }


    public void initShader() {
        int fp16 = Config.getInstance().getSpecial().fsr1.fp16 ? checkFP16Support() : 0;
        GlGeneralShaderProgram.ShaderInclude fsrEasuInclude = GlGeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_easu.comp.glsl"),
                "fsr1_easu.comp.glsl"
        );
        GlGeneralShaderProgram.ShaderInclude fsrEasuFp16Include = GlGeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_easu_fp16.comp.glsl"),
                "fsr1_easu_fp16.comp.glsl"
        );
        GlGeneralShaderProgram.ShaderInclude fsrRcasInclude = GlGeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_rcas.comp.glsl"),
                "fsr1_rcas.comp.glsl"
        );
        GlGeneralShaderProgram.ShaderInclude fsrRcasFp16Include = GlGeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_rcas_fp16.comp.glsl"),
                "fsr1_rcas_fp16.comp.glsl"
        );
        GlGeneralShaderProgram.ShaderInclude fsrCommonInclude = GlGeneralShaderProgram.ShaderInclude.create(
                FileReadHelper.readText("/shader/fsr1/fsr1_common.glsl"),
                "fsr1_common.glsl"
        );
        fsr1EASUShader = GlComputeShaderProgram.create()
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
        fsr1RCASShader = GlComputeShaderProgram.create()
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
        fsrUpscalePipeline = new GlPipeline();
        fsr1TempTexture = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.RGBA8
        );
        output = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8
        );
        fsrUpscalePipeline.addJob("fsr1_easu", PipelineJob.create()
                .setType(PipelineJobType.Compute)
                .setProgram(fsr1EASUShader)
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "temp",
                        fsr1TempTexture,
                        PipelineResourceAccess.WRITE,
                        null,
                        1
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "input",
                        FrameBufferTextureAdapter.ofColor(input),
                        PipelineResourceAccess.READ,
                        null,
                        0
                ))
                .build()
        );
        fsrUpscalePipeline.addJob("fsr1_rcas", PipelineJob.create()
                .setType(PipelineJobType.Compute)
                .setProgram(fsr1RCASShader)
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "temp",
                        fsr1TempTexture,
                        PipelineResourceAccess.READ,
                        null,
                        0
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "output",
                        output,
                        PipelineResourceAccess.WRITE,
                        null,
                        1
                ))
                .build()
        );
        this.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        int workRegionDim = 16;
        int dispatchX = (MinecraftRenderHandle.getScreenWidth() + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (MinecraftRenderHandle.getScreenHeight() + (workRegionDim - 1)) / workRegionDim;
        PipelineJobDispatchResource pipelineDispatchResource = new PipelineJobDispatchResource(
                new Vec3(
                        dispatchX,
                        dispatchY,
                        1
                )
        );
        fsrUpscalePipeline.scheduleJob("fsr1_easu", pipelineDispatchResource);
        setFSR1ShaderUniform(fsr1EASUShader);
        fsrUpscalePipeline.executeJob("fsr1_easu", pipelineDispatchResource);
        fsrUpscalePipeline.scheduleJob("fsr1_rcas", pipelineDispatchResource);
        setFSR1ShaderUniform(fsr1RCASShader);
        fsrUpscalePipeline.executeJob("fsr1_rcas", pipelineDispatchResource);
        return true;
    }

    private void setFSR1ShaderUniform(GlComputeShaderProgram shaderProgram) {
        shaderProgram.uniforms()
                .strictVec2("renderViewportSize").value(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight())
                .strictVec2("containerTextureSize").value(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight())
                .strictVec2("upscaledViewportSize").value(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight())
                .strictFloat("sharpness").value(Config.getSharpness());
    }

    @Override
    public void blitToScreen(int width, int height) {
        GlTexture.blitToScreen(output.width, output.height, width, height, this.output.id);
    }

    @Override
    public void destroy() {
        output.destroy();
        fsr1TempTexture.destroy();
        fsr1EASUShader.destroy();
        fsr1RCASShader.destroy();
    }

    @Override
    public void resize(int width, int height) {
        fsr1TempTexture.resize(width, height);
        output.resize(width, height);
    }

    @Override
    public int getOutputTextureId() {
        return output.id;
    }

    @Override
    public int getInputTextureId() {
        return super.getInputTextureId();
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return TextureFrameBufferAdapter.of(output);
    }
}
