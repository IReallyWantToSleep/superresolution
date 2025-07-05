package io.homo.superresolution.common.upscale.fsr1;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.*;
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
import io.homo.superresolution.core.math.Vector3f;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.common.upscale.DispatchResource;

public class FSR1 extends AbstractAlgorithm {
    private GlShaderProgram fsr1EASUShader;
    private GlShaderProgram fsr1RCASShader;
    private GlPipeline fsrUpscalePipeline;
    private GlTexture2D fsr1TempTexture;
    private GlFrameBuffer outputFbo;
    private GlTexture2D output;
    private StructuredUniformBuffer fsr1UBOData;
    private IBuffer fsr1UBO;

    @Override
    public void init() {
        input = MinecraftRenderHandle.getRenderTarget();
        fsr1UBOData = UniformStructBuilder.start()
                .vec2Entry("renderViewportSize")
                .vec2Entry("containerTextureSize")
                .vec2Entry("upscaledViewportSize")
                .floatEntry("sharpness")
                .build();
        fsr1UBO = RenderSystems.current().device().createBuffer(
                BufferDescription.create()
                        .size(fsr1UBOData.size())
                        .usage(BufferUsage.UBO)
                        .build()
        );
        fsr1UBO.setBufferData(fsr1UBOData);
        initShader();
        fsrUpscalePipeline = new GlPipeline();
        fsr1TempTexture = (GlTexture2D) RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(MinecraftRenderHandle.getRenderWidth())
                        .height(MinecraftRenderHandle.getRenderHeight())
                        .format(TextureFormat.RGBA8)
                        .usages(TextureUsages.create().sampler().storage())
                        .build()
        );
        output = (GlTexture2D) RenderSystems.current().device().createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .width(MinecraftRenderHandle.getScreenWidth())
                        .height(MinecraftRenderHandle.getScreenHeight())
                        .format(TextureFormat.RGBA8)
                        .usages(TextureUsages.create().sampler().storage())
                        .build()
        );
        outputFbo = GlFrameBuffer.create(
                output,
                null,
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        fsrUpscalePipeline.addJob("fsr1_easu",
                GlPipelineJobBuilders.compute(fsr1EASUShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "temp",
                                fsr1TempTexture,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                1
                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "input",
                                FrameBufferTextureAdapter.ofColor(input),
                                GlPipelineResourceAccess.READ,
                                null,
                                0
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build()
        );
        fsrUpscalePipeline.addJob("fsr1_rcas",
                GlPipelineJobBuilders.compute(fsr1RCASShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "temp",
                                fsr1TempTexture,
                                GlPipelineResourceAccess.READ,
                                null,
                                0
                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "output",
                                output,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                1
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build()
        );
        this.resize(MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
    }

    public void initShader() {
        int fp16 = Config.SPECIAL.FSR1.FP16.get() ? checkFP16Support() : 0;
        fsr1EASUShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(ShaderSource.file(ShaderType.COMPUTE, "/shader/fsr1/fsr1_main.comp.glsl"))
                        .name("FSR1_EASU")
                        .addDefine("FSR_FP16_CRITERIA", String.valueOf(fp16))
                        .addDefine("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                        .addDefine("FSR_EASU", String.valueOf(1))
                        .uniformBuffer("fsr1_data", 0, (int) fsr1UBOData.size())
                        .build()
        );
        fsr1EASUShader.compile();
        fsr1RCASShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.compute(ShaderSource.file(ShaderType.COMPUTE, "/shader/fsr1/fsr1_main.comp.glsl"))
                        .name("FSR1_RCAS")
                        .addDefine("FSR_FP16_CRITERIA", String.valueOf(fp16))
                        .addDefine("FSR_HALF", String.valueOf(fp16 == 0 ? 0 : 1))
                        .addDefine("FSR_RCAS", String.valueOf(1))
                        .uniformBuffer("fsr1_data", 0, (int) fsr1UBOData.size())
                        .build()
        );
        fsr1RCASShader.compile();
    }

    private Vector3f getWorkGroupSize() {
        int workRegionDim = 16;
        int dispatchX = (MinecraftRenderHandle.getScreenWidth() + (workRegionDim - 1)) / workRegionDim;
        int dispatchY = (MinecraftRenderHandle.getScreenHeight() + (workRegionDim - 1)) / workRegionDim;
        return new Vector3f(
                dispatchX,
                dispatchY,
                1
        );
    }

    public static int checkFP16Support() {
        if (GraphicsCapabilities.hasGLExtension("GL_EXT_shader_16bit_storage") &&
                GraphicsCapabilities.hasGLExtension("GL_EXT_shader_explicit_arithmetic_types")
        ) {
            return 1;
        }
        if (GraphicsCapabilities.hasGLExtension("GL_NV_gpu_shader5")) { //glslang似乎有bug？GL_NV_gpu_shader5扩展无法使用
            return 0;
        }
        return 0;
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        fsr1UBOData.setVec2("renderViewportSize", MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
        fsr1UBOData.setVec2("containerTextureSize", MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
        fsr1UBOData.setVec2("upscaledViewportSize", MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
        fsr1UBOData.setFloat("sharpness", Config.getSharpness());
        fsr1UBOData.fillBuffer();
        fsr1UBO.upload();
        fsr1RCASShader.uniforms().buffer("fsr1_data").set(fsr1UBO);
        fsr1EASUShader.uniforms().buffer("fsr1_data").set(fsr1UBO);
        fsrUpscalePipeline.scheduleJob("fsr1_easu");
        fsrUpscalePipeline.executeJob("fsr1_easu");
        fsrUpscalePipeline.scheduleJob("fsr1_rcas");
        fsrUpscalePipeline.executeJob("fsr1_rcas");
        return true;
    }


    @Override
    public void destroy() {
        output.destroy();
        fsr1TempTexture.destroy();
        fsr1EASUShader.destroy();
        fsr1RCASShader.destroy();
        fsr1UBOData.free();
        fsr1UBO.destroy();
        outputFbo.destroy();
    }

    @Override
    public void resize(int width, int height) {
        fsr1TempTexture.resize(width, height);
        output.resize(width, height);
        outputFbo.resizeFrameBuffer(width, height);
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFbo;
    }

    @Override
    public int getInputTextureId() {
        return super.getInputTextureId();
    }

    @Override
    public int getOutputTextureId() {
        return output.handle();
    }
}
