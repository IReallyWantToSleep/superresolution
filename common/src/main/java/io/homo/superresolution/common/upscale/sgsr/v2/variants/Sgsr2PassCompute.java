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
import io.homo.superresolution.core.math.Vector3f;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.AbstractSgsrVariant;
import io.homo.superresolution.common.upscale.sgsr.v2.Sgsr2;
import io.homo.superresolution.common.upscale.sgsr.v2.SgsrUtils;
import io.homo.superresolution.core.math.Vector3i;

import java.util.Optional;

public class Sgsr2PassCompute extends AbstractSgsrVariant {
    private GlShaderProgram convertShader;
    private GlShaderProgram upscaleShader;
    private Pipeline sgsrPipeline;
    private ITexture PrevLumaHistory;
    private ITexture YCoCgColor;

    private ITexture MotionDepthClipAlphaBuffer;
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
        RenderSystems.opengl().device().commendEncoder().begin();
        ICommandBuffer commandBuffer = RenderSystems.current().device().commendEncoder().getCommandBuffer();
        sgsrPipeline.executeJob(commandBuffer, "convert");
        sgsrPipeline.executeJob(commandBuffer, "upscale");
        RenderSystems.opengl().device().commendEncoder().end();
        RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
    }

    @Override
    public void init(Sgsr2 sgsr) {
        convertShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.COMPUTE, "/shader/sgsr/2pass_cs/sgsr2_convert.comp.glsl", true))
                        .name("SGSR_2PCS_A")
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("InputColor", 1)
                        .uniformSamplerTexture("InputDepth", 2)
                        .uniformSamplerTexture("InputVelocity", 3)
                        .uniformStorageTexture("MotionDepthClipAlphaBuffer", 0)
                        .uniformStorageTexture("YCoCgColor", 1)
                        .build()
        );
        convertShader.compile();
        upscaleShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(new ShaderSource(ShaderType.COMPUTE, "/shader/sgsr/2pass_cs/sgsr2_upscale.comp.glsl", true))
                        .name("SGSR_2PCS_B")
                        .uniformBuffer("Params", 0, (int) sgsr.getParams().getSize())
                        .uniformSamplerTexture("PrevHistoryOutput", 7)
                        .uniformSamplerTexture("MotionDepthClipAlphaBuffer", 8)
                        .uniformSamplerTexture("YCoCgColor", 9)
                        .uniformStorageTexture("SceneColorOutput", 0)
                        .uniformStorageTexture("HistoryOutput", 1)
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
                        .resource("MotionDepthClipAlphaBuffer",
                                PipelineJobResource.StorageTexture.create(
                                        MotionDepthClipAlphaBuffer,
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("YCoCgColor",
                                PipelineJobResource.StorageTexture.create(
                                        YCoCgColor,
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
                        .resource("SceneColorOutput",
                                PipelineJobResource.StorageTexture.create(
                                        FrameBufferTextureAdapter.ofColor(sgsr.getOutputFrameBuffer()),
                                        PipelineResourceAccess.Write
                                )
                        )
                        .resource("HistoryOutput",
                                PipelineJobResource.StorageTexture.create(
                                        TextureSupplier.of(() -> HistoryOutput),
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
