package io.homo.superresolution.common.upscale.sgsr.v1;

import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.UniformBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.texture.TextureType;
import io.homo.superresolution.core.graphics.impl.texture.TextureUsages;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.jobs.GlPipelineJobBuilders;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.impl.Vec2;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

public class Sgsr1 extends AbstractAlgorithm {
    private GlShaderProgram sgsrShader;
    private GlPipeline pipeline;
    private GlTexture2D outputColor;
    private GlFrameBuffer outputFbo;
    private UniformBuffer buffer;

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();

        buffer = UniformBuffer.create()
                .vec2Entry("renderSize")
                .vec2Entry("renderSizeRcp")
                .floatEntry("EdgeThreshold")
                .floatEntry("EdgeSharpness")
                .build();

        outputColor = (GlTexture2D) RenderSystems.current().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(MinecraftRenderHandle.getScreenWidth())
                        .height(MinecraftRenderHandle.getScreenHeight())
                        .format(TextureFormat.RGBA8)
                        .usages(TextureUsages.create().sampler().attachmentColor())
                        .build()
        );
        outputFbo = GlFrameBuffer.create(
                outputColor,
                null,
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        output = outputFbo;
        sgsrShader = RenderSystems.current().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.VERTEX, "/shader/sgsr/v1/sgsr1_shader.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.FRAGMENT, "/shader/sgsr/v1/sgsr1_shader.frag.glsl"))
                        .name("SGSRV1")
                        .addDefine("UseEdgeDirection", "")
                        .uniformBlock("sgsr1_data", 0, buffer.getSize())
                        .build()
        );
        sgsrShader.compile();

        pipeline = new GlPipeline();
        pipeline.addJob("sgsr1_main",
                GlPipelineJobBuilders.graphics(sgsrShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "ps0",
                                FrameBufferTextureAdapter.ofColor(input),
                                GlPipelineResourceAccess.READ,
                                null,
                                0
                        ))
                        .targetFramebuffer(outputFbo)
                        .build()
        );

        this.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        buffer.setVec2("renderSize", dispatchResource.renderWidth(), dispatchResource.renderHeight());
        buffer.setVec2("renderSizeRcp", 1.0f / dispatchResource.renderWidth(), 1.0f / dispatchResource.renderHeight());
        buffer.setFloat("EdgeThreshold", 8f / 255f);
        buffer.setFloat("EdgeSharpness", 2f);
        buffer.fillBuffer();
        sgsrShader.uniforms().block("sgsr1_data").set(buffer);
        pipeline.scheduleJob("sgsr1_main");
        pipeline.executeJob("sgsr1_main");
        return true;
    }

    @Override
    public void resize(int width, int height) {
        outputColor.resize(width, height);
        outputFbo.resizeFrameBuffer(width, height);
    }

    @Override
    public void destroy() {
        outputColor.destroy();
        sgsrShader.destroy();
        outputFbo.destroy();
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFbo;
    }
}