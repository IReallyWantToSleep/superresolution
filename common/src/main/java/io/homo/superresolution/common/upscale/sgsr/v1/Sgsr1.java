package io.homo.superresolution.common.upscale.sgsr.v1;

import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.RenderTargetBindPoint;
import io.homo.superresolution.common.render.gl.pipeline.*;
import io.homo.superresolution.common.render.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.common.render.gl.texture.GlSampler;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferWrapper;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.StorageFrameBuffer;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.utils.FileReadHelper;

public class Sgsr1 extends AbstractAlgorithm {
    private GlPipeline pipeline;
    private GlGeneralShaderProgram sgsrShader;

    public static Sgsr1 create() {
        return new Sgsr1();
    }

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        output = new StorageFrameBuffer(false);
        sgsrShader = GlGeneralShaderProgram.create()
                .addAllFragShaderTextList(FileReadHelper.readText("/shader/sgsr/v1/sgsr1_shader.frag.glsl"))
                .addAllVertShaderTextList(FileReadHelper.readText("/shader/sgsr/v1/sgsr1_shader.vert.glsl"))
                .setShaderName("SGSRV1")
                .addDefineText("UseEdgeDirection", "")
                .build()
                .compileShader();
        pipeline = new GlPipeline()
                .addJob("main",
                        PipelineJob.create()
                                .setProgram(sgsrShader)
                                .addResource(new PipelineResourceDescription(
                                        PipelineResourceType.Sampler2D,
                                        "ps0",
                                        FrameBufferWrapper.ofColor(input),
                                        PipelineResourceAccess.READ,
                                        GlSampler.create(GlSampler.SamplerType.LinearClamp),
                                        0
                                ))
                                .build()
                );
        this.resize(AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        pipeline.scheduleJobs(PipelineJobDispatchResource.nothing());
        sgsrShader.use();
        sgsrShader.setVec2("renderSize", dispatchResource.renderWidth(), dispatchResource.screenHeight());
        sgsrShader.setVec2("renderSizeRcp", (float) 1 / dispatchResource.renderWidth(), (float) 1 / dispatchResource.screenHeight());
        sgsrShader.setFloat("EdgeThreshold", 8f / 255f);
        sgsrShader.setFloat("EdgeSharpness", 2f);
        output.bind(RenderTargetBindPoint.WRITE);
        pipeline.executeJobs(PipelineJobDispatchResource.nothing());
        return false;
    }


    @Override
    public void blitToScreen(int width, int height) {
        GlTexture.blitToScreen(
                width,
                height,
                width,
                height,
                output.getTextureId(IFrameBuffer.FrameBufferAttachmentType.COLOR)
        );
    }

    @Override
    public void resize(int width, int height) {
        this.output.resizeFrameBuffer(width, height);
    }

    @Override
    public void destroy() {
        output.destroy();
        sgsrShader.destroy();
    }
}
