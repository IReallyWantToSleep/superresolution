package io.homo.superresolution.common.upscale.sgsr.v1;

import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.common.render.gl.framebuffer.FrameBufferAttachment;
import io.homo.superresolution.common.render.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.common.render.gl.pipeline.*;
import io.homo.superresolution.common.render.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.common.render.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.common.render.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.common.render.gl.texture.GlTexture;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.utils.FileReadHelper;

public class Sgsr1 extends AbstractAlgorithm {
    private GlPipeline pipeline;
    private AbstractGlShaderProgram sgsrShader;

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        GlFrameBuffer output_ = new GlFrameBuffer();
        output_.addAttachment(new FrameBufferAttachment(
                FrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                GlTexture.create(
                        MinecraftRenderHandle.getScreenWidth(),
                        MinecraftRenderHandle.getScreenHeight(),
                        TextureFormat.RGBA8
                )
        ));
        output = output_;
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
                                        FrameBufferTextureAdapter.ofColor(input),
                                        PipelineResourceAccess.READ,
                                        null,
                                        0
                                ))
                                .build()
                );


        this.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        pipeline.scheduleJobs(PipelineJobDispatchResource.nothing());
        sgsrShader.use();
        sgsrShader.setVec2("renderSize", dispatchResource.renderWidth(), dispatchResource.renderHeight());
        sgsrShader.setVec2("renderSizeRcp",
                1.0f / dispatchResource.renderWidth(), 1.0f / dispatchResource.renderHeight());
        sgsrShader.setFloat("EdgeThreshold", 8f / 255f);
        sgsrShader.setFloat("EdgeSharpness", 2f);
        output.bind(FrameBufferBindPoint.WRITE);
        pipeline.executeJobs(PipelineJobDispatchResource.nothing());

        sgsrShader.clear();
        return false;
    }


    @Override
    public void blitToScreen(int width, int height) {
        GlTexture.blitToScreen(
                width,
                height,
                width,
                height,
                output.getTextureId(FrameBufferAttachmentType.COLOR)
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
