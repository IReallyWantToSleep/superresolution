package io.homo.superresolution.common.upscale.nis;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.impl.Vec3;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.gl.Gl;
import io.homo.superresolution.common.render.gl.GlConst;
import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.common.render.gl.pipeline.*;
import io.homo.superresolution.common.render.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.render.impl.texture.ITexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.nis.enums.NISHDRMode;
import io.homo.superresolution.common.utils.FileReadHelper;
import org.lwjgl.opengl.GL43;

import static io.homo.superresolution.common.render.gl.GlConst.GL_UNSIGNED_BYTE;

public class NVIDIAImageScaling extends AbstractAlgorithm {
    private NVIDIAImageScalingConfig config;
    private ITexture output;
    private GlPipeline pipeline;
    private GlComputeShaderProgram scaleShader;
    private GlComputeShaderProgram sharpenShader;
    private ITexture coefScaler;
    private ITexture coefUSM;
    private GlUniformBuffer<NVIDIAImageScalingConfig> uniformBuffer;

    @Override
    protected boolean isSupport() {
        return AlgorithmDescriptions.NIS.getRequirement().check().support() && SuperResolution.interopManager.vulkanApp != null;
    }

    @Override
    public void init() {
        config = new NVIDIAImageScalingConfig();
        uniformBuffer = new GlUniformBuffer<>(config);
        input = MinecraftRenderHandle.getRenderTarget();
        output = GlTexture.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8
        );
        try (GlState ignored = new GlState()) {
            coefScaler = GlTexture.create(2, 64, TextureFormat.RGBA8);
            Gl.glBindTexture(GlConst.GL_TEXTURE_2D, coefScaler.getTextureId());
            GL43.glTexSubImage2D(
                    GlConst.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    2,
                    64,
                    TextureFormat.RGBA8.gl(),
                    GL_UNSIGNED_BYTE,
                    NVIDIAImageScalingConst.coef_scale
            );
            coefUSM = GlTexture.create(2, 64, TextureFormat.RGBA8);
            Gl.glBindTexture(GlConst.GL_TEXTURE_2D, coefUSM.getTextureId());
            GL43.glTexSubImage2D(
                    GlConst.GL_TEXTURE_2D,
                    0,
                    0,
                    0,
                    2,
                    64,
                    TextureFormat.RGBA8.gl(),
                    GL_UNSIGNED_BYTE,
                    NVIDIAImageScalingConst.coef_usm
            );
        }
        scaleShader = GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/nis/nis_scaler.comp.glsl"))
                .setShaderName("nis_scaler")
                .build()
                .compileShader();
        sharpenShader = GlComputeShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/nis/nis_sharpen.comp.glsl"))
                .setShaderName("nis_sharpen")
                .build()
                .compileShader();
        pipeline = new GlPipeline();
        pipeline.addJob("nis_scaler",
                PipelineJob.create()
                        .setType(PipelineJobType.Compute)
                        .setProgram(scaleShader)
                        .addResource(new PipelineResourceDescription(
                                PipelineResourceType.Sampler2D,
                                "in_texture",
                                FrameBufferTextureAdapter.ofColor(input),
                                PipelineResourceAccess.READ,
                                null,
                                2
                        ))
                        .addResource(new PipelineResourceDescription(
                                PipelineResourceType.Image2D,
                                "out_texture",
                                output,
                                PipelineResourceAccess.WRITE,
                                null,
                                3
                        ))
                        .addResource(new PipelineResourceDescription(
                                PipelineResourceType.Sampler2D,
                                "coef_scaler",
                                coefScaler,
                                PipelineResourceAccess.READ,
                                null,
                                4
                        ))
                        .addResource(new PipelineResourceDescription(
                                PipelineResourceType.Sampler2D,
                                "coef_usm",
                                coefUSM,
                                PipelineResourceAccess.READ,
                                null,
                                5
                        ))
                        .build()
        );
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        PipelineJobDispatchResource pipelineJobDispatchResource = new PipelineJobDispatchResource(
                new Vec3(
                        (float) Math.ceil((double) dispatchResource.renderWidth() / 32),
                        (float) Math.ceil((double) dispatchResource.renderHeight() / 24),
                        1.0f
                )
        );
        config.NVScalerUpdateConfig(
                Config.getSharpness(),
                0,
                0,
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight(),
                dispatchResource.renderWidth(),
                dispatchResource.renderHeight(),
                0,
                0,
                dispatchResource.screenWidth(),
                dispatchResource.screenHeight(),
                dispatchResource.screenWidth(),
                dispatchResource.screenHeight(),
                NISHDRMode.None
        );
        uniformBuffer.update();
        pipeline.scheduleJob("nis_scaler", pipelineJobDispatchResource);
        scaleShader.use();
        uniformBuffer.bind(0);
        pipeline.executeJob("nis_scaler", pipelineJobDispatchResource);
        return true;
    }

    @Override
    public void blitToScreen(int width, int height) {
        GlTexture.blitToScreen(
                width,
                height,
                width,
                height,
                output.getTextureId()
        );
    }

    @Override
    public void destroy() {
        output.destroy();
        scaleShader.destroy();
        sharpenShader.destroy();
        coefUSM.destroy();
        coefScaler.destroy();
        uniformBuffer.delete();
    }

    @Override
    public void resize(int width, int height) {
        output.resize(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
    }

    @Override
    public int getOutputTextureId() {
        return output.getTextureId();
    }
}
