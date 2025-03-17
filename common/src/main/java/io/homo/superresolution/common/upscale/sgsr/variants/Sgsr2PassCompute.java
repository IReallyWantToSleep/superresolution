package io.homo.superresolution.common.upscale.sgsr.variants;

import io.homo.superresolution.common.impl.Vec3;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.pipeline.*;
import io.homo.superresolution.common.render.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.common.render.gl.texture.GlSampler;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferWrapper;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.render.impl.texture.TextureSupplier;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.AbstractSgsrVariant;
import io.homo.superresolution.common.upscale.sgsr.Sgsr;
import io.homo.superresolution.common.upscale.sgsr.SgsrUtils;
import io.homo.superresolution.common.utils.FileReadHelper;

public class Sgsr2PassCompute extends AbstractSgsrVariant {
    private GlComputeShaderProgram convertShader;
    private GlComputeShaderProgram upscaleShader;
    private GlPipeline sgsrPipeline;
    private ITexture YCoCgColor;
    private ITexture MotionDepthClipAlphaBuffer;
    private ITexture PrevHistoryOutput;
    private ITexture HistoryOutput;


    @Override
    public void dispatch(DispatchResource resource, Sgsr sgsr) {
        int dispatchX = SgsrUtils.divideRoundUp(resource.screenWidth(), 8);
        int dispatchY = SgsrUtils.divideRoundUp(resource.screenHeight(), 8);
        PipelineJobDispatchResource pipelineDispatchResource = new PipelineJobDispatchResource(
                new Vec3(
                        dispatchX,
                        dispatchY,
                        1
                )
        );
        swapHistoryOutput();
        sgsrPipeline.scheduleJob("convert", pipelineDispatchResource);
        sgsr.getParams().bind(0);
        sgsrPipeline.executeJob("convert", pipelineDispatchResource);
        sgsrPipeline.scheduleJob("upscale", pipelineDispatchResource);
        sgsr.getParams().bind(0);
        sgsrPipeline.executeJob("upscale", pipelineDispatchResource);
    }

    @Override
    public void init(Sgsr sgsr) {
        convertShader = (GlComputeShaderProgram) GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/sgsr/2pass_cs/sgsr2_convert.comp.glsl"))
                .setShaderName("SGSR_2PCS_A")
                .build()
                .compileShader();
        upscaleShader = (GlComputeShaderProgram) GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/sgsr/2pass_cs/sgsr2_upscale.comp.glsl"))
                .setShaderName("SGSR_2PCS_B")
                .build()
                .compileShader();
        sgsrPipeline = new GlPipeline();
        YCoCgColor = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32UI
        );
        MotionDepthClipAlphaBuffer = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.RGBA16F
        );
        PrevHistoryOutput = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA16F
        );
        HistoryOutput = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA16F
        );
        sgsrPipeline.addJob("convert", PipelineJob.create()
                .setProgram(convertShader)
                .setType(PipelineJobType.Compute)
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "InputColor",
                        FrameBufferWrapper.ofColor(sgsr.getInputFrameBuffer()),
                        PipelineResourceAccess.READ,
                        null,
                        1
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "InputDepth",
                        FrameBufferWrapper.ofDepth(sgsr.getInputFrameBuffer()),
                        PipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.NearestClamp),
                        2
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "InputVelocity",
                        FrameBufferWrapper.ofColor(AlgorithmManager.getDispatchResource().motionVectors()),
                        PipelineResourceAccess.READ,
                        null,
                        3
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "MotionDepthClipAlphaBuffer",
                        MotionDepthClipAlphaBuffer,
                        PipelineResourceAccess.WRITE,
                        null,
                        0
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "YCoCgColor",
                        YCoCgColor,
                        PipelineResourceAccess.WRITE,
                        null,
                        1
                ))
                .build()
        );

        sgsrPipeline.addJob("upscale", PipelineJob.create()
                .setProgram(upscaleShader)
                .setType(PipelineJobType.Compute)
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "PrevHistoryOutput",
                        TextureSupplier.of(() -> PrevHistoryOutput),
                        PipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.LinearClamp),
                        7
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "MotionDepthClipAlphaBuffer",
                        MotionDepthClipAlphaBuffer,
                        PipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.LinearClamp),
                        8
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "YCoCgColor",
                        YCoCgColor,
                        PipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.NearestClamp),
                        9
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "SceneColorOutput",
                        FrameBufferWrapper.ofColor(sgsr.getOutputFrameBuffer()),
                        PipelineResourceAccess.WRITE,
                        null,
                        0
                ))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "HistoryOutput",
                        TextureSupplier.of(() -> HistoryOutput),
                        PipelineResourceAccess.WRITE,
                        null,
                        1
                ))
                .build()
        );
    }

    private void swapHistoryOutput() {
        ITexture tempA = PrevHistoryOutput;
        PrevHistoryOutput = HistoryOutput;
        HistoryOutput = tempA;
    }

    @Override
    public void destroy() {
        HistoryOutput.destroy();
        PrevHistoryOutput.destroy();
        convertShader.destroy();
        upscaleShader.destroy();
        MotionDepthClipAlphaBuffer.destroy();
        YCoCgColor.destroy();
    }

    @Override
    public void resize(int width, int height) {
        HistoryOutput.resize(width, height);
        PrevHistoryOutput.resize(width, height);
        MotionDepthClipAlphaBuffer.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
        YCoCgColor.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
    }
}
