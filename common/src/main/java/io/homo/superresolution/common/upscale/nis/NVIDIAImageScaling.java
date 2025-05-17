package io.homo.superresolution.common.upscale.nis;

import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.core.gl.pipeline.GlPipelineJobBuilders;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.gl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.gl.Gl;
import io.homo.superresolution.core.gl.GlConst;
import io.homo.superresolution.core.gl.GlState;
import io.homo.superresolution.core.gl.buffer.GlUniformBuffer;
import io.homo.superresolution.core.gl.pipeline.*;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.stb.STBImage.*;

import io.homo.superresolution.core.gl.shader.GlComputeShaderProgram;
import io.homo.superresolution.core.gl.texture.GlTexture2D;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferTextureAdapter;
import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.core.impl.shader.ShaderSource;
import io.homo.superresolution.core.impl.texture.ITexture;
import io.homo.superresolution.core.impl.texture.TextureFormat;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.common.upscale.nis.enums.NISHDRMode;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static io.homo.superresolution.core.gl.GlConst.GL_UNSIGNED_BYTE;

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
    public void init() {
        config = new NVIDIAImageScalingConfig();
        uniformBuffer = new GlUniformBuffer<>(config);
        input = MinecraftRenderHandle.getRenderTarget();
        output = GlTexture2D.create(
                MinecraftRenderHandle.getScreenWidth(),
                MinecraftRenderHandle.getScreenHeight(),
                TextureFormat.RGBA8
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
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/nis/nis_scaler.comp.glsl", true))
                .setShaderName("nis_scaler")
                .build()
                .compileShader();
        sharpenShader = GlComputeShaderProgram.create()
                .addShaderSource(new ShaderSource(ShaderSource.Type.COMPUTE, "/shader/nis/nis_sharpen.comp.glsl", true))
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
    public void blitToScreen(int width, int height) {
        GlTexture2D.blitToScreen(
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
