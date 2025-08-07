package io.homo.superresolution.common.upscale.sgsr.v2.variants;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.Pipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobBuilders;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineResourceAccess;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.math.Vector3i;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.AbstractSgsrVariant;
import io.homo.superresolution.common.upscale.sgsr.v2.Sgsr2;
import io.homo.superresolution.common.upscale.sgsr.v2.SgsrUtils;

import java.util.Optional;

public class Sgsr3PassCompute extends AbstractSgsrVariant {
    private GlShaderProgram activateShader;
    private GlShaderProgram convertShader;
    private GlShaderProgram upscaleShader;
    private Pipeline sgsrPipeline;
    private ITexture PrevLumaHistory;
    private ITexture LumaHistory;
    private ITexture YCoCgColor;

    private ITexture MotionDepthClipAlphaBuffer;
    private ITexture MotionDepthAlphaBuffer;
    private ITexture PrevHistoryOutput;
    private ITexture HistoryOutput;

    private Vector3i getWorkGroupSize() {
        int dispatchX = SgsrUtils.divideRoundUp(MinecraftRenderHandle.getScreenWidth(), 8);
        int dispatchY = SgsrUtils.divideRoundUp(MinecraftRenderHandle.getScreenHeight(), 8);
        return new Vector3i(
                dispatchX,
                dispatchY,
                1
        );
    }

    @Override
    public void dispatch(DispatchResource resource, Sgsr2 sgsr) {
        swapHistoryOutput();
        swapLumaHistory();
        RenderSystems.opengl().device().commendEncoder().begin();
        ICommandBuffer commandBuffer = RenderSystems.current().device().commendEncoder().getCommandBuffer();
        sgsrPipeline.executeJob(commandBuffer, "convert");
        sgsrPipeline.executeJob(commandBuffer, "activate");
        sgsrPipeline.executeJob(commandBuffer, "upscale");
        RenderSystems.opengl().device().commendEncoder().end();
        RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
    }

    @Override
    public void init(Sgsr2 sgsr) {
        activateShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.COMPUTE, "/shader/sgsr/3pass_cs/sgsr2_activate.comp.glsl", true))
                        .name("SGSR_3PCS_A")
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("PrevLumaHistory", 1)
                        .uniformSamplerTexture("MotionDepthAlphaBuffer", 2)
                        .uniformSamplerTexture("YCoCgColor", 3)
                        .uniformStorageTexture("MotionDepthClipAlphaBuffer", 4)
                        .uniformStorageTexture("LumaHistory", 5)
                        .build()
        );
        activateShader.compile();

        convertShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.COMPUTE, "/shader/sgsr/3pass_cs/sgsr2_convert.comp.glsl", true))
                        .name("SGSR_3PCS_B")
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("InputOpaqueColor", 1)
                        .uniformSamplerTexture("InputColor", 2)
                        .uniformSamplerTexture("InputDepth", 3)
                        .uniformSamplerTexture("InputVelocity", 4)
                        .uniformStorageTexture("YCoCgColor", 5)
                        .uniformStorageTexture("MotionDepthAlphaBuffer", 6)
                        .build()
        );
        convertShader.compile();

        upscaleShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.COMPUTE, "/shader/sgsr/3pass_cs/sgsr2_upscale.comp.glsl", true))
                        .name("SGSR_3PCS_C")
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("PrevHistoryOutput", 1)
                        .uniformSamplerTexture("MotionDepthClipAlphaBuffer", 2)
                        .uniformSamplerTexture("YCoCgColor", 3)
                        .uniformStorageTexture("HistoryOutput", 4)
                        .uniformStorageTexture("SceneColorOutput", 5)
                        .build()
        );
        upscaleShader.compile();

        sgsrPipeline = new Pipeline();
        PrevLumaHistory = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.R32UI)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        LumaHistory = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.R32UI)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        YCoCgColor = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.R32UI)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        MotionDepthClipAlphaBuffer = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        MotionDepthAlphaBuffer = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getRenderWidth())
                .height(MinecraftRenderHandle.getRenderHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        PrevHistoryOutput = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getScreenWidth())
                .height(MinecraftRenderHandle.getScreenHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());
        HistoryOutput = RenderSystems.current().device().createTexture(TextureDescription.create()
                .type(TextureType.Texture2D)
                .width(MinecraftRenderHandle.getScreenWidth())
                .height(MinecraftRenderHandle.getScreenHeight())
                .format(TextureFormat.RGBA16F)
                .usages(TextureUsages.create().storage().sampler())
                .build());

        sgsrPipeline.job("convert",
                PipelineJobBuilders.compute(convertShader)
                        .resource("InputOpaqueColor",
                                PipelineJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(sgsr.getInputResourceSet().colorTexture())
                                )
                        )
                        .resource("InputColor",
                                PipelineJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(sgsr.getInputResourceSet().colorTexture())
                                )
                        )
                        .resource("InputDepth",
                                PipelineJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(sgsr.getInputResourceSet().depthTexture())
                                )
                        )
                        .resource("InputVelocity",
                                PipelineJobResource.SamplerTexture.create(
                                        () -> Optional.ofNullable(sgsr.getInputResourceSet().motionVectorsTexture())
                                )
                        )
                        .resource("YCoCgColor",
                                PipelineJobResource.StorageTexture.create(
                                        YCoCgColor,
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("MotionDepthAlphaBuffer",
                                PipelineJobResource.StorageTexture.create(
                                        MotionDepthAlphaBuffer,
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("Params",
                                PipelineJobResource.UniformBuffer.create(
                                        sgsr.getParams()
                                )
                        )
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build());

        sgsrPipeline.job("activate",
                PipelineJobBuilders.compute(activateShader)
                        .resource("PrevLumaHistory",
                                PipelineJobResource.SamplerTexture.create(
                                        TextureSupplier.of(() -> PrevLumaHistory)
                                )
                        )
                        .resource("MotionDepthAlphaBuffer",
                                PipelineJobResource.SamplerTexture.create(
                                        MotionDepthAlphaBuffer
                                )
                        )
                        .resource("YCoCgColor",
                                PipelineJobResource.SamplerTexture.create(
                                        YCoCgColor
                                )
                        )
                        .resource("MotionDepthClipAlphaBuffer",
                                PipelineJobResource.StorageTexture.create(
                                        MotionDepthClipAlphaBuffer,
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("LumaHistory",
                                PipelineJobResource.StorageTexture.create(
                                        TextureSupplier.of(() -> LumaHistory),
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("Params",
                                PipelineJobResource.UniformBuffer.create(
                                        sgsr.getParams()
                                )
                        )
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build());

        sgsrPipeline.job("upscale",
                PipelineJobBuilders.compute(upscaleShader)
                        .resource("PrevHistoryOutput",
                                PipelineJobResource.SamplerTexture.create(
                                        TextureSupplier.of(() -> PrevHistoryOutput)
                                )
                        )
                        .resource("MotionDepthClipAlphaBuffer",
                                PipelineJobResource.SamplerTexture.create(
                                        MotionDepthClipAlphaBuffer
                                )
                        )
                        .resource("YCoCgColor",
                                PipelineJobResource.SamplerTexture.create(
                                        YCoCgColor
                                )
                        )
                        .resource("HistoryOutput",
                                PipelineJobResource.StorageTexture.create(
                                        TextureSupplier.of(() -> HistoryOutput),
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("SceneColorOutput",
                                PipelineJobResource.StorageTexture.create(
                                        FrameBufferTextureAdapter.ofColor(sgsr.getOutputFrameBuffer()),
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("Params",
                                PipelineJobResource.UniformBuffer.create(
                                        sgsr.getParams()
                                )
                        )
                        .workGroupSupplier(this::getWorkGroupSize)
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
        activateShader.destroy();
        convertShader.destroy();
        upscaleShader.destroy();
        MotionDepthClipAlphaBuffer.destroy();
        MotionDepthAlphaBuffer.destroy();
        PrevLumaHistory.destroy();
        LumaHistory.destroy();
        YCoCgColor.destroy();
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
