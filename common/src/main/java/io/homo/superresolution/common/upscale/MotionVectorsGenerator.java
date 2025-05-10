package io.homo.superresolution.common.upscale;

import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBufferAttachment;
import io.homo.superresolution.core.gl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.gl.pipeline.*;
import io.homo.superresolution.core.gl.shader.GlGeneralShaderProgram;
import io.homo.superresolution.core.gl.texture.GlTexture;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.core.utils.FileReadHelper;

import static io.homo.superresolution.core.gl.Gl.*;
import static io.homo.superresolution.core.gl.GlConst.*;

public class MotionVectorsGenerator {
    private static final GlGeneralShaderProgram preprocess =
            GlGeneralShaderProgram.create()
                    .addShaderSource(new ShaderSource(ShaderSource.Type.FRAGMENT, "/shader/motion_vector/preprocess.frag.glsl", true))
                    .addShaderSource(new ShaderSource(ShaderSource.Type.VERTEX, "/shader/motion_vector/common.vert.glsl", true))
                    .setShaderName("motion_vector_preprocess")
                    .build();
    private static final GlGeneralShaderProgram pass1 =
            GlGeneralShaderProgram.create()
                    .addShaderSource(new ShaderSource(ShaderSource.Type.FRAGMENT, "/shader/motion_vector/pass1.frag.glsl", true))
                    .addShaderSource(new ShaderSource(ShaderSource.Type.VERTEX, "/shader/motion_vector/common.vert.glsl", true))
                    .setShaderName("motion_vector_pass1")
                    .build();

    private static final GlGeneralShaderProgram pass2 =
            GlGeneralShaderProgram.create()
                    .addShaderSource(new ShaderSource(ShaderSource.Type.FRAGMENT, "/shader/motion_vector/pass2.frag.glsl", true))
                    .addShaderSource(new ShaderSource(ShaderSource.Type.VERTEX, "/shader/motion_vector/common.vert.glsl", true))
                    .setShaderName("motion_vector_pass2")
                    .build();

    private static final GlGeneralShaderProgram pass3 =
            GlGeneralShaderProgram.create()
                    .addShaderSource(new ShaderSource(ShaderSource.Type.FRAGMENT, "/shader/motion_vector/pass3.frag.glsl", true))
                    .addShaderSource(new ShaderSource(ShaderSource.Type.VERTEX, "/shader/motion_vector/common.vert.glsl", true))
                    .setShaderName("motion_vector_pass3")
                    .build();

    private static final GlPipeline pipeline = new GlPipeline();
    public static IFrameBuffer gradFrameBuffer;
    public static IFrameBuffer deltaFrameBuffer;
    public static IFrameBuffer preprocessFrameBuffer;

    public static ITexture currentFrameTexture;
    public static ITexture previousFrameTexture;
    public static IFrameBuffer motionVectorsFrameBuffer;

    public static IFrameBuffer getMotionVectorsFrameBuffer() {
        return motionVectorsFrameBuffer;
    }

    public static void init() {
        motionVectorsFrameBuffer = new GlFrameBuffer();
        ((GlFrameBuffer) motionVectorsFrameBuffer).addAttachment(new GlFrameBufferAttachment(
                GlFrameBufferAttachment.FrameBufferAttachmentType.COLOR,
                GlTexture.create(
                        MinecraftRenderHandle.getRenderWidth(),
                        MinecraftRenderHandle.getRenderHeight(),
                        TextureFormat.RG16F
                )
        ));
        motionVectorsFrameBuffer.setClearColor(0, 0, 0, 1);
        gradFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RG32F,
                null,
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
        deltaFrameBuffer = GlFrameBuffer.create(
                TextureFormat.R32F,
                null,
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );

        previousFrameTexture = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32F
        );
        currentFrameTexture = GlTexture.create(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight(),
                TextureFormat.R32F
        );
        preprocessFrameBuffer = GlFrameBuffer.create(
                TextureFormat.R32F,
                null,
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
        pipeline.addJob("preprocess",
                GlPipelineJob.create()
                        .setType(GlPipelineJobType.Graphics)
                        .setProgram(preprocess)
                        .addResource(new GlPipelineResourceDescription(
                                GlPipelineResourceType.Sampler2D,
                                "tex_current",
                                MinecraftRenderHandle.getRenderTarget().getTexture(FrameBufferAttachmentType.COLOR),
                                GlPipelineResourceAccess.READ,
                                null,
                                1
                        ))
                        .setTargetFrameBuffer(preprocessFrameBuffer)
                        .build()
        );
        pipeline.addJob("pass1",
                GlPipelineJob.create()
                        .setType(GlPipelineJobType.Graphics)
                        .setProgram(pass1)
                        .addResource(new GlPipelineResourceDescription(
                                GlPipelineResourceType.Sampler2D,
                                "tex_current",
                                currentFrameTexture,
                                GlPipelineResourceAccess.READ,
                                null,
                                1
                        ))
                        .setTargetFrameBuffer(gradFrameBuffer)
                        .build()
        );
        pipeline.addJob("pass2",
                GlPipelineJob.create()
                        .setProgram(pass2)
                        .setType(GlPipelineJobType.Graphics)
                        .setTargetFrameBuffer(deltaFrameBuffer)
                        .addResource(new GlPipelineResourceDescription(
                                GlPipelineResourceType.Sampler2D,
                                "tex_current",
                                currentFrameTexture,
                                GlPipelineResourceAccess.READ,
                                null,
                                1
                        ))
                        .addResource(new GlPipelineResourceDescription(
                                GlPipelineResourceType.Sampler2D,
                                "tex_previous",
                                previousFrameTexture,
                                GlPipelineResourceAccess.READ,
                                null,
                                2
                        ))
                        .build()
        );
        pipeline.addJob("pass3", GlPipelineJob.create()
                .setProgram(pass3)
                .setType(GlPipelineJobType.Graphics)
                .setTargetFrameBuffer(null)
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Sampler2D,
                        "grad_current",
                        gradFrameBuffer.getTexture(FrameBufferAttachmentType.COLOR),
                        GlPipelineResourceAccess.READ,
                        null,
                        1
                ))
                .addResource(new GlPipelineResourceDescription(
                        GlPipelineResourceType.Sampler2D,
                        "delta_time",
                        deltaFrameBuffer.getTexture(FrameBufferAttachmentType.COLOR),
                        GlPipelineResourceAccess.READ,
                        null,
                        2
                ))
                .setTargetFrameBuffer(motionVectorsFrameBuffer)
                .build()
        );
    }

    public static void resize() {
        motionVectorsFrameBuffer.resizeFrameBuffer(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );

        gradFrameBuffer.resizeFrameBuffer(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
        deltaFrameBuffer.resizeFrameBuffer(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
        preprocessFrameBuffer.resizeFrameBuffer(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
        previousFrameTexture.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );
        currentFrameTexture.resize(
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );

    }

    public static void update(DispatchResource dispatchResource) {
        if (!preprocess.compiled) preprocess.compileShader();
        if (!pass1.compiled) pass1.compileShader();
        if (!pass2.compiled) pass2.compileShader();
        if (!pass3.compiled) pass3.compileShader();

        try (GlState ignored = new GlState()) {
            GlPipelineJobDispatchResource pipelineJobDispatchResource = new GlPipelineJobDispatchResource(
                    new Vec3(1, 1, 1)
            );
            pipeline.scheduleJobs(pipelineJobDispatchResource);
            preprocess.use();
            preprocess.uniforms().strictFloat("exposure").value(3.0f);
            pipeline.execute("preprocess", pipelineJobDispatchResource);
            glCopyImageSubData(
                    preprocessFrameBuffer.getTextureId(FrameBufferAttachmentType.COLOR), GL_TEXTURE_2D, 0, 0, 0, 0,
                    currentFrameTexture.getTextureId(), GL_TEXTURE_2D, 0, 0, 0, 0,
                    dispatchResource.renderWidth(), dispatchResource.renderHeight(), 1
            );
            pipeline.execute("pass1", pipelineJobDispatchResource);
            pipeline.execute("pass2", pipelineJobDispatchResource);
            pass3.use();
            pass3.uniforms()
                    .strictInt("window_radius").value(2)
                    .strictFloat("min_value").value(1e-6f)
                    .strictFloat("scale").value(4f);
            pipeline.execute("pass3", pipelineJobDispatchResource);
            glCopyImageSubData(
                    currentFrameTexture.getTextureId(), GL_TEXTURE_2D, 0, 0, 0, 0,
                    previousFrameTexture.getTextureId(), GL_TEXTURE_2D, 0, 0, 0, 0,
                    dispatchResource.renderWidth(), dispatchResource.renderHeight(), 1
            );
        }
    }
}
