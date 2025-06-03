package io.homo.superresolution.common.upscale.sgsr.v2.variants;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.jobs.GlPipelineJobBuilders;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.AbstractSgsrVariant;
import io.homo.superresolution.common.upscale.sgsr.v2.Sgsr2;
import io.homo.superresolution.common.upscale.sgsr.v2.SgsrUtils;

public class Sgsr2PassCompute extends AbstractSgsrVariant {
    private GlShaderProgram convertShader;
    private GlShaderProgram upscaleShader;
    private GlPipeline sgsrPipeline;
    private ITexture PrevLumaHistory;
    private ITexture YCoCgColor;

    private ITexture MotionDepthClipAlphaBuffer;
    private ITexture PrevHistoryOutput;
    private ITexture HistoryOutput;

    private Vec3 getWorkGroupSize() {
        int dispatchX = SgsrUtils.divideRoundUp(MinecraftRenderHandle.getScreenWidth(), 8);
        int dispatchY = SgsrUtils.divideRoundUp(MinecraftRenderHandle.getScreenHeight(), 8);
        return new Vec3(
                dispatchX,
                dispatchY,
                1
        );
    }

    @Override
    public void dispatch(DispatchResource resource, Sgsr2 sgsr) {
        swapHistoryOutput();
        sgsrPipeline.execute("convert");
        sgsrPipeline.execute("upscale");
    }

    @Override
    public void init(Sgsr2 sgsr) {
        convertShader = RenderSystems.current().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.COMPUTE, "/shader/sgsr/2pass_cs/sgsr2_convert.comp.glsl", true))
                        .name("SGSR_2PCS_A")
                        .build()
        );
        convertShader.compile();
        upscaleShader = RenderSystems.current().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.COMPUTE, "/shader/sgsr/2pass_cs/sgsr2_upscale.comp.glsl", true))
                        .name("SGSR_2PCS_B")
                        .build()
        );
        upscaleShader.compile();
        sgsrPipeline = new GlPipeline();
        PrevLumaHistory = RenderSystems.current().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.R32UI)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        YCoCgColor = RenderSystems.current().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.R32UI)
                .usages(TextureUsages.create().storage().sampler())
                .build());

        MotionDepthClipAlphaBuffer = RenderSystems.current().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        PrevHistoryOutput = RenderSystems.current().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getScreenWidth())
                .height(MinecraftRenderHandle.getScreenHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        HistoryOutput = RenderSystems.current().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getScreenWidth())
                .height(MinecraftRenderHandle.getScreenHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        sgsrPipeline.addJob("convert",
                GlPipelineJobBuilders.compute(convertShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "InputColor",
                                FrameBufferTextureAdapter.ofColor(sgsr.getInputFrameBuffer()),
                                GlPipelineResourceAccess.READ,
                                null,
                                1))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "InputDepth",
                                FrameBufferTextureAdapter.ofDepth(sgsr.getInputFrameBuffer()),
                                GlPipelineResourceAccess.READ,
                                GlSampler.create(GlSampler.SamplerType.NearestClamp),
                                2))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "InputVelocity",
                                FrameBufferTextureAdapter.ofColor(
                                        AlgorithmManager.getDispatchResource().motionVectors()),
                                GlPipelineResourceAccess.READ,
                                null,
                                3))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "MotionDepthClipAlphaBuffer",
                                MotionDepthClipAlphaBuffer,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                0))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "YCoCgColor",
                                YCoCgColor,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                1))
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
                                7))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "MotionDepthClipAlphaBuffer",
                                MotionDepthClipAlphaBuffer,
                                GlPipelineResourceAccess.READ,
                                GlSampler.create(GlSampler.SamplerType.LinearClamp),
                                8))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "YCoCgColor",
                                YCoCgColor,
                                GlPipelineResourceAccess.READ,
                                GlSampler.create(GlSampler.SamplerType.NearestClamp),
                                9))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "SceneColorOutput",
                                FrameBufferTextureAdapter.ofColor(sgsr.getOutputFrameBuffer()),
                                GlPipelineResourceAccess.WRITE,
                                null,
                                0))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "HistoryOutput",
                                TextureSupplier.of(() -> HistoryOutput),
                                GlPipelineResourceAccess.WRITE,
                                null,
                                1))
                        .resource(GlPipelineResourceDescription.createUBOResource(
                                "Params",
                                sgsr.getParams(),
                                0
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
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
