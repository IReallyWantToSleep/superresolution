package io.homo.superresolution.common.upscale.fsr1;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.core.gl.pipeline.GlPipelineJobBuilders;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.core.GraphicsCapabilities;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.pipeline.*;
import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.texture.TextureFrameBufferAdapter;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

public class FSR1 extends AbstractAlgorithm {
    private GlComputeShaderProgram fsr1EASUShader;
    private GlComputeShaderProgram fsr1RCASShader;
    private GlPipeline fsrUpscalePipeline;
    private GlTexture2D fsr1TempTexture;
    private GlTexture2D output;

    public static int checkFP16Support() {
        if (GraphicsCapabilities.hasGLExtension("GL_EXT_shader_16bit_storage") &&
                GraphicsCapabilities.hasGLExtension("GL_EXT_shader_explicit_arithmetic_types")
        ) {
            return 1;
        }
        if (GraphicsCapabilities.hasGLExtension("GL_NV_gpu_shader5")) { //glslang似乎有bug？GL_NV_gpu_shader5扩展无法使用
            return 0;
        }
        return 0;
    }


    public void initShader() {
        int fp16 = Config.getInstance().getSpecial().fsr1.fp16 ? checkFP16Support() : 0;
        fsr1EASUShader = GlComputeShaderProgram.create()
                .addDefineText("FSR_FP16_CRITERIA", String.valueOf(fp16))
                .addDefineText("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                .addDefineText("FSR_EASU", String.valueOf(1))
                .setShaderName("FSR1_EASU")
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/fsr1/fsr1_main.comp.glsl", true))
                .build()
                .compileShader();
        fsr1RCASShader = GlComputeShaderProgram.create()
                .addDefineText("FSR_FP16_CRITERIA", String.valueOf(fp16))
                .addDefineText("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                .addDefineText("FSR_RCAS", String.valueOf(1))
                .setShaderName("FSR1_RCAS")
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/fsr1/fsr1_main.comp.glsl", true))
                .build()
                .compileShader();
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        initShader();
        fsrUpscalePipeline = new GlPipeline();
        fsr1TempTexture = GlTexture2D.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.RGBA8
        );
        output = GlTexture2D.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8
        );
        fsrUpscalePipeline.addJob("fsr1_easu",
                GlPipelineJobBuilders.compute(fsr1EASUShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "temp",
                                fsr1TempTexture,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                1
                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "input",
                                FrameBufferTextureAdapter.ofColor(input),
                                GlPipelineResourceAccess.READ,
                                null,
                                0
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build()
        );
        fsrUpscalePipeline.addJob("fsr1_rcas",
                GlPipelineJobBuilders.compute(fsr1RCASShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "temp",
                                fsr1TempTexture,
                                GlPipelineResourceAccess.READ,
                                null,
                                0
                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "output",
                                output,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                1
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build()
        );
        this.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
    }

    private Vec3 getWorkGroupSize() {
        int workRegionDim = 16;
        int dispatchX = (MinecraftRenderHandle.getScreenWidth() + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (MinecraftRenderHandle.getScreenHeight() + (workRegionDim - 1)) / workRegionDim;
        return new Vec3(
                dispatchX,
                dispatchY,
                1
        );
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        fsrUpscalePipeline.scheduleJob("fsr1_easu");
        setFSR1ShaderUniform(fsr1EASUShader);
        fsrUpscalePipeline.executeJob("fsr1_easu");
        fsrUpscalePipeline.scheduleJob("fsr1_rcas");
        setFSR1ShaderUniform(fsr1RCASShader);
        fsrUpscalePipeline.executeJob("fsr1_rcas");
        return true;
    }

    private void setFSR1ShaderUniform(GlComputeShaderProgram shaderProgram) {
        shaderProgram.use();
        shaderProgram.uniforms()
                .safeVec2("renderViewportSize").value(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight())
                .safeVec2("containerTextureSize").value(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight())
                .safeVec2("upscaledViewportSize").value(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight())
                .safeFloat("sharpness").value(Config.getSharpness());
    }

    @Override
    public void blitToScreen(int width, int height) {
        GlTexture2D.blitToScreen(output.getWidth(), output.getHeight(), width, height, this.output.getTextureId());
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
        return output.getTextureId();
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
