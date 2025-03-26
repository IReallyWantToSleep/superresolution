package io.homo.superresolution.common.upscale.sgsr.v1;

import io.homo.superresolution.common.impl.Vec3;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.RenderTargetBindPoint;
import io.homo.superresolution.common.render.gl.Gl;
import io.homo.superresolution.common.render.gl.GlConst;
import io.homo.superresolution.common.render.gl.GlState;
import io.homo.superresolution.common.render.gl.pipeline.*;
import io.homo.superresolution.common.render.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.common.render.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.common.render.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferWrapper;
import io.homo.superresolution.common.render.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.render.impl.framebuffer.StorageFrameBuffer;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.upscale.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.sgsr.v2.SgsrUtils;
import io.homo.superresolution.common.upscale.utils.AlgorithmHelper;
import io.homo.superresolution.common.utils.FileReadHelper;

public class Sgsr1 extends AbstractAlgorithm {
    private GlPipeline pipeline;
    private AbstractGlShaderProgram sgsrShader;

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
                                .setType(PipelineJobType.Graphics)
                                .setProgram(sgsrShader)
                                .addResource(new PipelineResourceDescription(
                                        PipelineResourceType.Sampler2D,
                                        "ps0",
                                        FrameBufferWrapper.ofColor(input),
                                        PipelineResourceAccess.READ,
                                        null,
                                        0
                                ))
                                .build()
                );


        this.resize(AlgorithmManager.helper.getScreenWidth(), AlgorithmManager.helper.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        GlState.save("sgsr1");
        pipeline.scheduleJobs(PipelineJobDispatchResource.nothing());
        sgsrShader.use();
        sgsrShader.setVec2("renderSize", dispatchResource.renderWidth(), dispatchResource.renderHeight());
        sgsrShader.setVec2("renderSizeRcp",
                1.0f / dispatchResource.renderWidth(), 1.0f / dispatchResource.renderHeight());
        sgsrShader.setFloat("EdgeThreshold", 8f / 255f);
        sgsrShader.setFloat("EdgeSharpness", 2f);
        output.bind(RenderTargetBindPoint.WRITE);
        pipeline.executeJobs(PipelineJobDispatchResource.nothing());

        sgsrShader.clear();
        Gl.glBindFramebuffer(GlConst.GL_DRAW_FRAMEBUFFER, GlState.get("sgsr1").writeFBO());
        Gl.glBindFramebuffer(GlConst.GL_READ_FRAMEBUFFER, GlState.get("sgsr1").readFBO());
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
