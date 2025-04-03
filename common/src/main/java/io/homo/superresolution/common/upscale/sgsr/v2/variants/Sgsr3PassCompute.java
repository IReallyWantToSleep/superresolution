package io.homo.superresolution.common.upscale.sgsr.v2.variants;

import io.homo.superresolution.common.impl.Vec3;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.pipeline.*;
import io.homo.superresolution.common.render.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.common.render.gl.texture.GlSampler;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.render.impl.texture.TextureSupplier;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.AbstractSgsrVariant;
import io.homo.superresolution.common.upscale.sgsr.v2.Sgsr2;
import io.homo.superresolution.common.upscale.sgsr.v2.SgsrUtils;
import io.homo.superresolution.common.utils.FileReadHelper;

public class Sgsr3PassCompute extends AbstractSgsrVariant {
    private GlComputeShaderProgram activateShader;

    private GlComputeShaderProgram convertShader;
    private GlComputeShaderProgram upscaleShader;
    private GlPipeline sgsrPipeline;
    private ITexture PrevLumaHistory;
    private ITexture LumaHistory;
    private ITexture YCoCgColor;

    private ITexture MotionDepthClipAlphaBuffer;
    private ITexture MotionDepthAlphaBuffer;
    private ITexture PrevHistoryOutput;
    private ITexture HistoryOutput;

    @Override
    public void dispatch(DispatchResource resource, Sgsr2 sgsr) {
        int dispatchX = SgsrUtils.divideRoundUp(resource.screenWidth(), 8);
        int dispatchY = SgsrUtils.divideRoundUp(resource.screenHeight(), 8);
        PipelineJobDispatchResource pipelineDispatchResource = new PipelineJobDispatchResource(
                new Vec3(
                        dispatchX,
                        dispatchY,
                        1));
        swapHistoryOutput();
        swapLumaHistory();
        sgsrPipeline.scheduleJob("convert", pipelineDispatchResource);
        sgsr.getParams().bind(0);
        sgsrPipeline.executeJob("convert", pipelineDispatchResource);
        sgsrPipeline.scheduleJob("activate", pipelineDispatchResource);
        sgsr.getParams().bind(0);
        sgsrPipeline.executeJob("activate", pipelineDispatchResource);
        sgsrPipeline.scheduleJob("upscale", pipelineDispatchResource);
        sgsr.getParams().bind(0);
        sgsrPipeline.executeJob("upscale", pipelineDispatchResource);
    }

    @Override
    public void init(Sgsr2 sgsr) {
        activateShader = GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper
                        .readText("/shader/sgsr/3pass_cs/sgsr2_activate.comp.glsl"))
                .setShaderName("SGSR_3PCS_A")
                .build()
                .compileShader();
        convertShader = GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper
                        .readText("/shader/sgsr/3pass_cs/sgsr2_convert.comp.glsl"))
                .setShaderName("SGSR_3PCS_B")
                .build()
                .compileShader();
        upscaleShader = GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper
                        .readText("/shader/sgsr/3pass_cs/sgsr2_upscale.comp.glsl"))
                .setShaderName("SGSR_3PCS_C")
                .build()
                .compileShader();
        sgsrPipeline = new GlPipeline();
        PrevLumaHistory = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32UI);
        LumaHistory = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32UI);
        YCoCgColor = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32UI);
        MotionDepthClipAlphaBuffer = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.RGBA16F);
        MotionDepthAlphaBuffer = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.RGBA16F);
        PrevHistoryOutput = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA16F);
        HistoryOutput = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA16F);
        sgsrPipeline.addJob("convert", PipelineJob.create()
                .setProgram(convertShader)
                .setType(PipelineJobType.Compute)
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "InputOpaqueColor",
                        FrameBufferTextureAdapter.ofColor(sgsr.getInputFrameBuffer()),
                        PipelineResourceAccess.READ,
                        null,
                        1))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "InputColor",
                        FrameBufferTextureAdapter.ofColor(sgsr.getInputFrameBuffer()),
                        PipelineResourceAccess.READ,
                        null,
                        2))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "InputDepth",
                        FrameBufferTextureAdapter.ofDepth(sgsr.getInputFrameBuffer()),
                        PipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.NearestClamp),
                        3))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "InputVelocity",
                        FrameBufferTextureAdapter.ofColor(
                                AlgorithmManager.getDispatchResource().motionVectors()),
                        PipelineResourceAccess.READ,
                        null,
                        4))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "YCoCgColor",
                        YCoCgColor,
                        PipelineResourceAccess.WRITE,
                        null,
                        5))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "MotionDepthAlphaBuffer",
                        MotionDepthAlphaBuffer,
                        PipelineResourceAccess.WRITE,
                        null,
                        6))
                .build());
        sgsrPipeline.addJob("activate", PipelineJob.create()
                .setProgram(activateShader)
                .setType(PipelineJobType.Compute)
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "PrevLumaHistory",
                        TextureSupplier.of(() -> PrevLumaHistory),
                        PipelineResourceAccess.READ,
                        null,
                        1))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "MotionDepthAlphaBuffer",
                        MotionDepthAlphaBuffer,
                        PipelineResourceAccess.READ,
                        null,
                        2))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "YCoCgColor",
                        YCoCgColor,
                        PipelineResourceAccess.READ,
                        null,
                        3))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "MotionDepthClipAlphaBuffer",
                        MotionDepthClipAlphaBuffer,
                        PipelineResourceAccess.WRITE,
                        null,
                        4))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "LumaHistory",
                        TextureSupplier.of(() -> LumaHistory),
                        PipelineResourceAccess.WRITE,
                        null,
                        5))
                .build());
        sgsrPipeline.addJob("upscale", PipelineJob.create()
                .setProgram(upscaleShader)
                .setType(PipelineJobType.Compute)
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "PrevHistoryOutput",
                        TextureSupplier.of(() -> PrevHistoryOutput),
                        PipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.LinearClamp),
                        1))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "MotionDepthClipAlphaBuffer",
                        MotionDepthClipAlphaBuffer,
                        PipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.LinearClamp),
                        2))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Sampler2D,
                        "YCoCgColor",
                        TextureSupplier.of(() -> YCoCgColor),
                        PipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.NearestClamp),
                        3))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "SceneColorOutput",
                        FrameBufferTextureAdapter.ofColor(sgsr.getOutputFrameBuffer()),
                        PipelineResourceAccess.WRITE,
                        null,
                        5))
                .addResource(new PipelineResourceDescription(
                        PipelineResourceType.Image2D,
                        "HistoryOutput",
                        TextureSupplier.of(() -> HistoryOutput),
                        PipelineResourceAccess.WRITE,
                        null,
                        4))
                .build());
    }

    private void swapHistoryOutput() {
        ITexture tempA = PrevHistoryOutput;
        PrevHistoryOutput = HistoryOutput;
        HistoryOutput = tempA;
    }

    private void swapLumaHistory() {
        ITexture tempA = PrevLumaHistory;
        PrevLumaHistory = LumaHistory;
        LumaHistory = tempA;
    }

    @Override
    public void destroy() {
        HistoryOutput.destroy();
        PrevHistoryOutput.destroy();
        convertShader.destroy();
        upscaleShader.destroy();
        MotionDepthClipAlphaBuffer.destroy();
        PrevLumaHistory.destroy();
        LumaHistory.destroy();

    }

    @Override
    public void resize(int width, int height) {
        HistoryOutput.resize(width, height);
        PrevHistoryOutput.resize(width, height);
        MotionDepthAlphaBuffer.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight());
        YCoCgColor.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight());
        MotionDepthClipAlphaBuffer.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight());
        PrevLumaHistory.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight());
        LumaHistory.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight());
    }
}
