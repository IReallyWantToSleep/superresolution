package io.homo.superresolution.common.upscale.sgsr.v2.variants;

import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.pipeline.*;
import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.gl.texture.GlSampler;
import io.homo.superresolution.core.gl.texture.GlTexture;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.impl.texture.TextureSupplier;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.AbstractSgsrVariant;
import io.homo.superresolution.common.upscale.sgsr.v2.Sgsr2;
import io.homo.superresolution.common.upscale.sgsr.v2.SgsrUtils;
import io.homo.superresolution.core.utils.FileReadHelper;

public class Sgsr2PassCompute extends AbstractSgsrVariant {
    private GlComputeShaderProgram convertShader;
    private GlComputeShaderProgram upscaleShader;
    private GlPipeline sgsrPipeline;
    private ITexture PrevLumaHistory;
    private ITexture YCoCgColor;

    private ITexture MotionDepthClipAlphaBuffer;
    private ITexture PrevHistoryOutput;
    private ITexture HistoryOutput;

    @Override
    public void dispatch(DispatchResource resource, Sgsr2 sgsr) {
        int dispatchX = SgsrUtils.divideRoundUp(resource.screenWidth(), 8);
        int dispatchY = SgsrUtils.divideRoundUp(resource.screenHeight(), 8);
        GlPipelineJobDispatchResource pipelineDispatchResource = new GlPipelineJobDispatchResource(
                new Vec3(
                        dispatchX,
                        dispatchY,
                        1));
        swapHistoryOutput();
        //swapLumaHistory();
        sgsrPipeline.scheduleJob("convert", pipelineDispatchResource);
        sgsr.getParams().bind(0);
        sgsrPipeline.executeJob("convert", pipelineDispatchResource);
        sgsrPipeline.scheduleJob("upscale", pipelineDispatchResource);
        sgsr.getParams().bind(0);
        sgsrPipeline.executeJob("upscale", pipelineDispatchResource);
    }

    @Override
    public void init(Sgsr2 sgsr) {
        convertShader = GlComputeShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/sgsr/2pass_cs/sgsr2_convert.comp.glsl", true))
                .setShaderName("SGSR_2PCS_A")
                .build()
                .compileShader();
        upscaleShader = GlComputeShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/sgsr/2pass_cs/sgsr2_upscale.comp.glsl", true))
                .setShaderName("SGSR_2PCS_B")
                .build()
                .compileShader();
        sgsrPipeline = new GlPipeline();
        PrevLumaHistory = GlTexture.create(
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
        PrevHistoryOutput = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA16F);
        HistoryOutput = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA16F);
        sgsrPipeline.addJob("convert", GlPipelineJob.create()
                .setProgram(convertShader)
                .setType(GlPipelineJobType.Compute)
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Sampler2D,
                        "InputColor",
                        FrameBufferTextureAdapter.ofColor(sgsr.getInputFrameBuffer()),
                        GlPipelineResourceAccess.READ,
                        null,
                        1))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Sampler2D,
                        "InputDepth",
                        FrameBufferTextureAdapter.ofDepth(sgsr.getInputFrameBuffer()),
                        GlPipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.NearestClamp),
                        2))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Sampler2D,
                        "InputVelocity",
                        FrameBufferTextureAdapter.ofColor(
                                AlgorithmManager.getDispatchResource().motionVectors()),
                        GlPipelineResourceAccess.READ,
                        null,
                        3))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Image2D,
                        "MotionDepthClipAlphaBuffer",
                        MotionDepthClipAlphaBuffer,
                        GlPipelineResourceAccess.WRITE,
                        null,
                        0))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Image2D,
                        "YCoCgColor",
                        YCoCgColor,
                        GlPipelineResourceAccess.WRITE,
                        null,
                        1))
                .build());

        sgsrPipeline.addJob("upscale", GlPipelineJob.create()
                .setProgram(upscaleShader)
                .setType(GlPipelineJobType.Compute)
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Sampler2D,
                        "PrevHistoryOutput",
                        TextureSupplier.of(() -> PrevHistoryOutput),
                        GlPipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.LinearClamp),
                        7))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Sampler2D,
                        "MotionDepthClipAlphaBuffer",
                        MotionDepthClipAlphaBuffer,
                        GlPipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.LinearClamp),
                        8))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Sampler2D,
                        "YCoCgColor",
                        YCoCgColor,
                        GlPipelineResourceAccess.READ,
                        GlSampler.create(GlSampler.SamplerType.NearestClamp),
                        9))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Image2D,
                        "SceneColorOutput",
                        FrameBufferTextureAdapter.ofColor(sgsr.getOutputFrameBuffer()),
                        GlPipelineResourceAccess.WRITE,
                        null,
                        0))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Image2D,
                        "HistoryOutput",
                        TextureSupplier.of(() -> HistoryOutput),
                        GlPipelineResourceAccess.WRITE,
                        null,
                        1))
                .build());
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
                MinecraftRenderHandle.getRenderHeight());
        YCoCgColor.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight());
    }
}
