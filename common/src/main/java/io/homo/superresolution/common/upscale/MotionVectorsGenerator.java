package io.homo.superresolution.common.upscale;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.StructuredUniformBuffer;
import io.homo.superresolution.core.graphics.impl.buffer.UniformStructBuilder;
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
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;

public class MotionVectorsGenerator {
    public static GlShaderProgram preprocessShader;
    public static GlShaderProgram pass1Shader;
    public static GlShaderProgram pass2Shader;
    public static GlShaderProgram pass3Shader;
    public static GlPipeline pipeline;
    public static GlTexture2D currentFrameTexture;
    public static GlTexture2D previousFrameTexture;
    public static GlFrameBuffer gradFrameBuffer;
    public static GlFrameBuffer deltaFrameBuffer;
    public static GlFrameBuffer preprocessFrameBuffer;
    public static GlFrameBuffer motionVectorsFrameBuffer;
    public static StructuredUniformBuffer structuredUniformBuffer;

    public static void initShaders() {
        preprocessShader = RenderSystems.current().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.VERTEX, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.FRAGMENT, "/shader/motion_vector/preprocess.frag.glsl"))
                        .name("motion_vector_preprocess")
                        .uniformBuffer("motion_vector_data", 0, structuredUniformBuffer.size())
                        .build()
        );
        preprocessShader.compile();

        pass1Shader = RenderSystems.current().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.VERTEX, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.FRAGMENT, "/shader/motion_vector/pass1.frag.glsl"))
                        .name("motion_vector_pass1")
                        .uniformBuffer("motion_vector_data", 0, structuredUniformBuffer.size())
                        .build()
        );
        pass1Shader.compile();

        pass2Shader = RenderSystems.current().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.VERTEX, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.FRAGMENT, "/shader/motion_vector/pass2.frag.glsl"))
                        .name("motion_vector_pass2")
                        .uniformBuffer("motion_vector_data", 0, structuredUniformBuffer.size())
                        .build()
        );
        pass2Shader.compile();

        pass3Shader = RenderSystems.current().createShaderProgram(
                ShaderDescription.create()
                        .vertex(ShaderSource.file(ShaderType.VERTEX, "/shader/motion_vector/common.vert.glsl"))
                        .fragment(ShaderSource.file(ShaderType.FRAGMENT, "/shader/motion_vector/pass3.frag.glsl"))
                        .name("motion_vector_pass3")
                        .uniformBuffer("motion_vector_data", 0, structuredUniformBuffer.size())
                        .build()
        );
        pass3Shader.compile();
    }

    public static void init() {
        structuredUniformBuffer = UniformStructBuilder.start()
                .floatEntry("exposure")
                .intEntry("window_radius")
                .floatEntry("min_value")
                .floatEntry("scale")
                .build();

        initShaders();

        pipeline = new GlPipeline();
        currentFrameTexture = (GlTexture2D) RenderSystems.current().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(MinecraftRenderHandle.getRenderWidth())
                        .height(MinecraftRenderHandle.getRenderHeight())
                        .format(TextureFormat.R32F)
                        .usages(TextureUsages.create().sampler().storage())
                        .build()
        );

        previousFrameTexture = (GlTexture2D) RenderSystems.current().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(MinecraftRenderHandle.getRenderWidth())
                        .height(MinecraftRenderHandle.getRenderHeight())
                        .format(TextureFormat.R32F)
                        .usages(TextureUsages.create().sampler().storage())
                        .build()
        );
        motionVectorsFrameBuffer = GlFrameBuffer.create(
                TextureFormat.RG16F,
                null,
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );

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

        preprocessFrameBuffer = GlFrameBuffer.create(
                TextureFormat.R32F,
                null,
                MinecraftRenderHandle.getRenderWidth(),
                MinecraftRenderHandle.getRenderHeight()
        );

        // Setup pipeline jobs
        pipeline.addJob("preprocess",
                GlPipelineJobBuilders.graphics(preprocessShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "tex_current",
                                FrameBufferTextureAdapter.ofColor(MinecraftRenderHandle.getRenderTarget()),
                                GlPipelineResourceAccess.READ,
                                null,
                                1
                        ))
                        .targetFramebuffer(preprocessFrameBuffer)
                        .build()
        );

        pipeline.addJob("copy_preprocess_fbo_to_current_frame_texture",
                GlPipelineJobBuilders.copy()
                        .from(FrameBufferTextureAdapter.ofColor(preprocessFrameBuffer))
                        .to(currentFrameTexture)
                        .build()
        );

        pipeline.addJob("pass1",
                GlPipelineJobBuilders.graphics(pass1Shader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "tex_current",
                                currentFrameTexture,
                                GlPipelineResourceAccess.READ,
                                null,
                                1
                        ))
                        .targetFramebuffer(gradFrameBuffer)
                        .build()
        );

        pipeline.addJob("pass2",
                GlPipelineJobBuilders.graphics(pass2Shader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "tex_current",
                                currentFrameTexture,
                                GlPipelineResourceAccess.READ,
                                null,
                                1
                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "tex_previous",
                                previousFrameTexture,
                                GlPipelineResourceAccess.READ,
                                null,
                                2
                        ))
                        .targetFramebuffer(deltaFrameBuffer)
                        .build()
        );

        pipeline.addJob("pass3",
                GlPipelineJobBuilders.graphics(pass3Shader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "grad_current",
                                FrameBufferTextureAdapter.ofColor(gradFrameBuffer),
                                GlPipelineResourceAccess.READ,
                                null,
                                1
                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "delta_time",
                                FrameBufferTextureAdapter.ofColor(deltaFrameBuffer),
                                GlPipelineResourceAccess.READ,
                                null,
                                2
                        ))
                        .targetFramebuffer(motionVectorsFrameBuffer)
                        .build()
        );

        pipeline.addJob("copy_current_frame_texture_to_previous_frame_texture",
                GlPipelineJobBuilders.copy()
                        .from(currentFrameTexture)
                        .to(previousFrameTexture)
                        .build()
        );

        resize();
    }

    public static void resize() {
        int width = MinecraftRenderHandle.getRenderWidth();
        int height = MinecraftRenderHandle.getRenderHeight();

        currentFrameTexture.resize(width, height);
        previousFrameTexture.resize(width, height);
        motionVectorsFrameBuffer.resizeFrameBuffer(width, height);
        gradFrameBuffer.resizeFrameBuffer(width, height);
        deltaFrameBuffer.resizeFrameBuffer(width, height);
        preprocessFrameBuffer.resizeFrameBuffer(width, height);
    }

    public static void update() {
        structuredUniformBuffer.setFloat("exposure", 3.0f);
        structuredUniformBuffer.setInt("window_radius", 2);
        structuredUniformBuffer.setFloat("min_value", 1e-6f);
        structuredUniformBuffer.setFloat("scale", 4f);
        structuredUniformBuffer.fillBuffer();

        pipeline.scheduleJob("preprocess");
        pipeline.executeJob("preprocess");

        pipeline.scheduleJob("copy_preprocess_fbo_to_current_frame_texture");
        pipeline.executeJob("copy_preprocess_fbo_to_current_frame_texture");

        pipeline.scheduleJob("pass1");
        pipeline.executeJob("pass1");

        pipeline.scheduleJob("pass2");
        pipeline.executeJob("pass2");

        pipeline.scheduleJob("pass3");
        pipeline.executeJob("pass3");

        pipeline.scheduleJob("copy_current_frame_texture_to_previous_frame_texture");
        pipeline.executeJob("copy_current_frame_texture_to_previous_frame_texture");
    }

    public static void destroy() {
        currentFrameTexture.destroy();
        previousFrameTexture.destroy();
        motionVectorsFrameBuffer.destroy();
        gradFrameBuffer.destroy();
        deltaFrameBuffer.destroy();
        preprocessFrameBuffer.destroy();
        preprocessShader.destroy();
        pass1Shader.destroy();
        pass2Shader.destroy();
        pass3Shader.destroy();
    }

    public static IFrameBuffer getMotionVectorsFrameBuffer() {
        return motionVectorsFrameBuffer;
    }
}