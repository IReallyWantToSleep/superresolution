package io.homo.superresolution.common.upscale.nis;

public class NVIDIAImageScaling {
    /*
    private NVIDIAImageScalingConfig config;
    private ITexture output;
    private GlPipeline pipeline;
    private GlComputeShaderProgram scaleShader;
    private GlComputeShaderProgram sharpenShader;
    private ITexture coefScaler;
    private ITexture coefUSM;
    private GlUniformBuffer<NVIDIAImageScalingConfig> uniformBuffer;
    private GlFrameBuffer outputFbo;

    @Override
    public void init() {
        config = new NVIDIAImageScalingConfig();
        uniformBuffer = new GlUniformBuffer<>(config);
        input = MinecraftRenderHandle.getRenderTarget();
        output = GlTexture2D.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8
        );
        outputFbo = GlFrameBuffer.create(
                output,
                null,
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        stbi_set_flip_vertically_on_load(true);

        try (GlState ignored = new GlState()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer buf = stbi_load("I:/super_resolution_moddev/superresolution/common/src/main/resources/assets/super_resolution/textures/coef_scaler.png", w, h, channels, 4);

                coefScaler = GlTexture2D.create(
                        NVIDIAImageScalingConst.kFilterSize / 4,
                        NVIDIAImageScalingConst.kPhaseCount,
                        TextureFormat.RGBA8
                );
                Gl.glBindTexture(GlConst.GL_TEXTURE_2D, coefScaler.getTextureId());
                if (buf != null) {
                    GL43.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
                    GL43.glTexSubImage2D(
                            GlConst.GL_TEXTURE_2D,
                            0,
                            0,
                            0,
                            NVIDIAImageScalingConst.kFilterSize / 4,
                            NVIDIAImageScalingConst.kPhaseCount,
                            GL_RGBA,
                            GL_UNSIGNED_BYTE,
                            buf
                    );
                    stbi_image_free(buf);
                }
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer buf = stbi_load("I:/super_resolution_moddev/superresolution/common/src/main/resources/assets/super_resolution/textures/coef_usm.png", w, h, channels, 4);
                coefUSM = GlTexture2D.create(
                        NVIDIAImageScalingConst.kFilterSize / 4,
                        NVIDIAImageScalingConst.kPhaseCount,
                        TextureFormat.RGBA8
                );
                Gl.glBindTexture(GlConst.GL_TEXTURE_2D, coefUSM.getTextureId());
                if (buf != null) {
                    GL43.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
                    GL43.glTexSubImage2D(
                            GlConst.GL_TEXTURE_2D,
                            0,
                            0,
                            0,
                            NVIDIAImageScalingConst.kFilterSize / 4,
                            NVIDIAImageScalingConst.kPhaseCount,
                            GL_RGBA,
                            GL_UNSIGNED_BYTE,
                            buf
                    );
                    stbi_image_free(buf);
                }
            }

        }
        stbi_set_flip_vertically_on_load(false);

        scaleShader = GlComputeShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderType.COMPUTE, "/shader/nis/nis_scaler.comp.glsl", true))
                .setShaderName("nis_scaler")
                .build()
                .compileShader();
        sharpenShader = GlComputeShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderType.COMPUTE, "/shader/nis/nis_sharpen.comp.glsl", true))
                .setShaderName("nis_sharpen")
                .build()
                .compileShader();
        pipeline = new GlPipeline();
        pipeline.addJob("nis_scaler",
                GlPipelineJobBuilders.compute(scaleShader)
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "in_texture",
                                FrameBufferTextureAdapter.ofColor(input),
                                GlPipelineResourceAccess.READ,
                                null,
                                NVIDIAImageScalingConst.IN_TEX_BINDING
                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Image2D,
                                "out_texture",
                                output,
                                GlPipelineResourceAccess.WRITE,
                                null,
                                NVIDIAImageScalingConst.OUT_TEX_BINDING
                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "coef_scaler",
                                coefScaler,
                                GlPipelineResourceAccess.READ,
                                null,
                                NVIDIAImageScalingConst.COEF_SCALAR_BINDING

                        ))
                        .resource(GlPipelineResourceDescription.createTextureResource(
                                GlPipelineResourceType.Sampler2D,
                                "coef_usm",
                                coefUSM,
                                GlPipelineResourceAccess.READ,
                                null,
                                NVIDIAImageScalingConst.COEF_USM_BINDING
                        ))
                        .resource(GlPipelineResourceDescription.createUBOResource(
                                "const_buffer",
                                uniformBuffer,
                                0
                        ))
                        .workGroupSupplier(this::getWorkGroupSize)
                        .build()
        );
    }

    private Vec3 getWorkGroupSize() {
        return new Vec3(
                (float) Math.ceil(MinecraftRenderHandle.getScreenWidth() / 32) + 1,
                (float) Math.ceil(MinecraftRenderHandle.getScreenHeight() / 24) + 1,
                1.0f
        );
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
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
        uniformBuffer.struct().updateData(dispatchResource);
        uniformBuffer.update();
        pipeline.execute("nis_scaler");
        return true;
    }

    @Override
    public void destroy() {
        output.destroy();
        scaleShader.destroy();
        sharpenShader.destroy();
        coefUSM.destroy();
        coefScaler.destroy();
        uniformBuffer.delete();
        outputFbo.destroy();
    }

    @Override
    public void resize(int width, int height) {
        output.resize(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
        outputFbo.resizeFrameBuffer(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight()
        );
    }

    @Override
    public int getOutputTextureId() {
        return output.getTextureId();
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFbo;
    }*/
}
