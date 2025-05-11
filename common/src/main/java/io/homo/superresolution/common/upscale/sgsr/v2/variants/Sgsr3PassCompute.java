package io.homo.superresolution.common.upscale.sgsr.v2.variants;

import io.homo.superresolution.core.gl.pipeline.GlPipelineJobBuilders;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.pipeline.*;
import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.gl.texture.GlSampler;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
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
        swapHistoryOutput();
        swapLumaHistory();
        sgsrPipeline.execute("convert");
        sgsrPipeline.execute("activate");
        sgsrPipeline.execute("upscale");
    }

    @Override
    public void init(Sgsr2 sgsr) {
        activateShader = GlComputeShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/sgsr/3pass_cs/sgsr2_activate.comp.glsl", true))
                .setShaderName("SGSR_3PCS_A")
                .build()
                .compileShader();
        convertShader = GlComputeShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/sgsr/3pass_cs/sgsr2_convert.comp.glsl", true))
                .setShaderName("SGSR_3PCS_B")
                .build()
                .compileShader();
        upscaleShader = GlComputeShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/sgsr/3pass_cs/sgsr2_upscale.comp.glsl", true))
                .setShaderName("SGSR_3PCS_C")
                .build()
                .compileShader();
        sgsrPipeline = new GlPipeline();
        PrevLumaHistory = GlTexture2D.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32UI);
        LumaHistory = GlTexture2D.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32UI);
        YCoCgColor = GlTexture2D.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32UI);
        MotionDepthClipAlphaBuffer = GlTexture2D.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.RGBA16F);
        MotionDepthAlphaBuffer = GlTexture2D.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.RGBA16F);
        PrevHistoryOutput = GlTexture2D.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA16F);
        HistoryOutput = GlTexture2D.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA16F);
        sgsrPipeline.addJob("convert",
                GlPipelineJobBuilders.compute(convertShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "InputOpaqueColor",
                                FrameBufferTextureAdapter.ofColor(sgsr.getInputFrameBuffer()),
                                GlPipelineResourceAccess.READ,
                                null,
                                1))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "InputColor",
                                FrameBufferTextureAdapter.ofColor(sgsr.getInputFrameBuffer()),
                                GlPipelineResourceAccess.READ,
                                null,
                                2))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "InputDepth",
                                FrameBufferTextureAdapter.ofDepth(sgsr.getInputFrameBuffer()),
                                GlPipelineResourceAccess.READ,
                                GlSampler.create(GlSampler.SamplerType.NearestClamp),
                                3))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "InputVelocity",
                                FrameBufferTextureAdapter.ofColor(
                                        AlgorithmManager.getDispatchResource().motionVectors()),
                                GlPipelineResourceAccess.READ,
                                null,
                                4))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "YCoCgColor",
                                YCoCgColor,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                5))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "MotionDepthAlphaBuffer",
                                MotionDepthAlphaBuffer,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                6))
                        .resource(GlPipelineResourceDescription.createUBOResource(
                                "Params",
                                sgsr.getParams(),
                                0
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build());
        sgsrPipeline.addJob("activate",
                GlPipelineJobBuilders.compute(activateShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "PrevLumaHistory",
                                TextureSupplier.of(() -> PrevLumaHistory),
                                GlPipelineResourceAccess.READ,
                                null,
                                1))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "MotionDepthAlphaBuffer",
                                MotionDepthAlphaBuffer,
                                GlPipelineResourceAccess.READ,
                                null,
                                2))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "YCoCgColor",
                                YCoCgColor,
                                GlPipelineResourceAccess.READ,
                                null,
                                3))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "MotionDepthClipAlphaBuffer",
                                MotionDepthClipAlphaBuffer,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                4))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "LumaHistory",
                                TextureSupplier.of(() -> LumaHistory),
                                GlPipelineResourceAccess.WRITE,
                                null,
                                5))
                        .resource(GlPipelineResourceDescription.createUBOResource(
                                "Params",
                                sgsr.getParams(),
                                0
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build());
        sgsrPipeline.addJob("upscale",
                GlPipelineJobBuilders.compute(upscaleShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "PrevHistoryOutput",
                                TextureSupplier.of(() -> PrevHistoryOutput),
                                GlPipelineResourceAccess.READ,
                                GlSampler.create(GlSampler.SamplerType.LinearClamp),
                                1))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "MotionDepthClipAlphaBuffer",
                                MotionDepthClipAlphaBuffer,
                                GlPipelineResourceAccess.READ,
                                GlSampler.create(GlSampler.SamplerType.LinearClamp),
                                2))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "YCoCgColor",
                                TextureSupplier.of(() -> YCoCgColor),
                                GlPipelineResourceAccess.READ,
                                GlSampler.create(GlSampler.SamplerType.NearestClamp),
                                3))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "SceneColorOutput",
                                FrameBufferTextureAdapter.ofColor(sgsr.getOutputFrameBuffer()),
                                GlPipelineResourceAccess.WRITE,
                                null,
                                5))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "HistoryOutput",
                                TextureSupplier.of(() -> HistoryOutput),
                                GlPipelineResourceAccess.WRITE,
                                null,
                                4))
                        .resource(GlPipelineResourceDescription.createUBOResource(
                                "Params",
                                sgsr.getParams(),
                                0
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build());
    }

    private Vec3 getWorkGroupSize() {
        int dispatchX = SgsrUtils.divideRoundUp(MinecraftRenderHandle.getScreenWidth(), 8);
        int dispatchY = SgsrUtils.divideRoundUp(MinecraftRenderHandle.getScreenHeight(), 8);
        return new Vec3(
                dispatchX,
                dispatchY,
                1
        );
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
        activateShader.destroy();
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
