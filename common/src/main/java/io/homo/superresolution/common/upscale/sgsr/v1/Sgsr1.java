package io.homo.superresolution.common.upscale.sgsr.v1;

import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.pipeline.GlPipelineJobBuilders;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBufferAttachment;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.gl.pipeline.*;
import io.homo.superresolution.core.gl.shader.AbstractGlShaderProgram;
import io.homo.superresolution.core.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

public class Sgsr1 extends AbstractAlgorithm {
    private GlPipeline pipeline;
    private AbstractGlShaderProgram sgsrShader;

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        GlFrameBuffer output_ = new GlFrameBuffer();
        output_.addAttachment(new GlFrameBufferAttachment(
                GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                GlTexture2D.create(
                        MinecraftRenderHandle.getScreenWidth(),
                        MinecraftRenderHandle.getScreenHeight(),
                        TextureFormat.RGBA8
                )
        ));
        output = output_;
        sgsrShader = GlGeneralShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderSource.Type.FRAGMENT, "/shader/sgsr/v1/sgsr1_shader.frag.glsl", true))
                .addShaderSource(new ShaderSource(ShaderSource.Type.VERTEX, "/shader/sgsr/v1/sgsr1_shader.vert.glsl", true))
                .setShaderName("SGSRV1")
                .addDefineText("UseEdgeDirection", "")
                .build()
                .compileShader();
        pipeline = new GlPipeline()
                .addJob("main",
                        GlPipelineJobBuilders.graphics(sgsrShader)
                                .resource(GlPipelineResourceDescription.createTextureResource(
                                        GlPipelineResourceType.Sampler2D,
                                        "ps0",
                                        FrameBufferTextureAdapter.ofColor(input),
                                        GlPipelineResourceAccess.READ,
                                        null,
                                        0
                                ))
                                .targetFramebuffer(output)
                                .build()
                );


        this.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        pipeline.scheduleJobs();
        sgsrShader.use();
        sgsrShader.uniforms()
                .safeVec2("renderSize").value(dispatchResource.renderWidth(), dispatchResource.renderHeight())
                .safeVec2("renderSizeRcp").value(1.0f / dispatchResource.renderWidth(), 1.0f / dispatchResource.renderHeight())
                .safeFloat("EdgeThreshold").value(8f / 255f)
                .safeFloat("EdgeSharpness").value(2f);
        pipeline.executeJobs();
        sgsrShader.clear();
        return false;
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

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return super.getOutputFrameBuffer();
    }
}
