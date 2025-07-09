package io.homo.superresolution.common.dataset;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import io.homo.superresolution.api.event.LevelRenderEndEvent;
import io.homo.superresolution.api.event.LevelRenderStartEvent;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobBuilders;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.pipeline.jobs.GlPipelineJobBuilders;
import io.homo.superresolution.core.graphics.opengl.pipeline.jobs.GlPipelineJob;
import io.homo.superresolution.core.graphics.opengl.pipeline.jobs.GlPipelineJobDispatchResource;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceAccess;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceDescription;
import io.homo.superresolution.core.graphics.opengl.pipeline.resource.GlPipelineResourceType;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL33.*;

public class DataSetGenerator {
    public static final File OUTPUT_DIR = Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "msrDataset").toFile();
    private static final KeyMapping SAVE_KEYMAPPING = new KeyMapping(
            "key.super_resolution.save_data",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F7,
            "Super Resolution"
    );
    private static GlFrameBuffer tempFbo;
    private static GlShaderProgram copyShader;
    private static GlShaderProgram copyShader0;

    public static void init() {
        if (!OUTPUT_DIR.exists() && !OUTPUT_DIR.mkdirs()) {
            System.err.println("Failed to create output directory: " + OUTPUT_DIR.getAbsolutePath());
        }
        KeyMappingRegistry.register(SAVE_KEYMAPPING);
        LevelRenderStartEvent.EVENT.register(DataSetGenerator::onLevelBegin);
        LevelRenderEndEvent.EVENT.register(DataSetGenerator::onLevelEnd);
        tempFbo = GlFrameBuffer.create(
                TextureFormat.RGBA8, null, 1, 1
        );
        copyShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.graphics(
                                new ShaderSource(ShaderType.FRAGMENT, "/shader/depth_to_r16f.frag.glsl", true),
                                new ShaderSource(ShaderType.VERTEX, "/shader/depth_to_r16f.vert.glsl", true)
                        )
                        .name("dataset_build_copy_depth_to_color")
                        .uniformSamplerTexture("tex", 0)
                        .build()
        );
        copyShader0 = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.graphics(
                                new ShaderSource(ShaderType.FRAGMENT, "/shader/rg16f_to_rgb.frag.glsl", true),
                                new ShaderSource(ShaderType.VERTEX, "/shader/rg16f_to_rgb.vert.glsl", true)
                        )
                        .name("dataset_build_copy_rg16f_to_rgb")
                        .uniformSamplerTexture("tex", 0)
                        .build()
        );
    }

    private static void writeImage(ITexture texture, String path) {
        Path outputPath = Paths.get(path);
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            System.err.println("Failed to create parent directories for: " + path);
            e.printStackTrace();
            return;
        }

        int width = texture.getWidth();
        int height = texture.getHeight();
        TextureFormat format = texture.getTextureFormat();

        int bytesPerPixel;
        switch (format) {
            case RGBA8, RG16F, R32F, R32UI, R11G11B10F, DEPTH32F, DEPTH24_STENCIL8, DEPTH24 -> bytesPerPixel = 4;
            case RGB8 -> bytesPerPixel = 3;
            case RG8, R16F, R16_SNORM -> bytesPerPixel = 2;
            case R8 -> bytesPerPixel = 1;
            case RGBA16F, RG32F -> bytesPerPixel = 8;
            default -> throw new IllegalArgumentException("Unknown TextureFormat: " + format);
        }

        int dataSize = width * height * bytesPerPixel;
        ByteBuffer pixelData = MemoryUtil.memCalloc(dataSize);

        boolean success = readPixels(texture, pixelData, 0, dataSize);
        if (!success) {
            System.err.println("Failed to read pixel data from texture");
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            byte[] arr = new byte[dataSize];
            pixelData.get(arr);
            fos.write(arr);
        } catch (IOException e) {
            System.err.println("Failed to write texture data to file: " + path);
            e.printStackTrace();
        } finally {
            MemoryUtil.memFree(pixelData);
        }
    }

    private static void onLevelBegin() {
        int frameCount = MinecraftRenderHandle.getFrameCount();
        if (!SAVE_KEYMAPPING.isDown()) return;
        File dir = Paths.get(OUTPUT_DIR.getPath(), String.valueOf(frameCount)).toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("Failed to create frame output directory: " + dir.getAbsolutePath());
        }
        tempFbo.resizeFrameBuffer(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
    }

    private static void onLevelEnd() {
        int frameCount = MinecraftRenderHandle.getFrameCount();
        if (!SAVE_KEYMAPPING.isDown()) return;
        File dir = Paths.get(OUTPUT_DIR.getPath(), String.valueOf(frameCount)).toFile();
        File HR_RGB_IMAGE = Paths.get(dir.getPath(), "hr_rgb.bin").toFile();
        File HR_DEPTH_IMAGE = Paths.get(dir.getPath(), "hr_depth.bin").toFile();
        File HR_MV_IMAGE = Paths.get(dir.getPath(), "hr_mv.bin").toFile();

        writeImage(
                MinecraftRenderHandle.getRenderTarget().getTexture(FrameBufferAttachmentType.Color),
                HR_RGB_IMAGE.getAbsolutePath()
        );
        PipelineJobBuilders
                .graphics(copyShader)
                .targetFramebuffer(tempFbo)
                .build()
                .resource(
                        "tex",
                        PipelineJobResource.Texture.create(
                                MinecraftRenderHandle.getRenderTarget().getTexture(FrameBufferAttachmentType.AnyDepth)
                        )
                ).execute(RenderSystems.current());
        writeImage(
                tempFbo.getTexture(FrameBufferAttachmentType.Color),
                HR_DEPTH_IMAGE.getAbsolutePath()
        );
        PipelineJobBuilders
                .graphics(copyShader0)
                .targetFramebuffer(tempFbo)
                .build()
                .resource(
                        "tex",
                        PipelineJobResource.Texture.create(
                                AlgorithmManager.getMotionVectorsFrameBuffer().getTexture(FrameBufferAttachmentType.Color)
                        )
                ).execute(RenderSystems.current());
        writeImage(
                tempFbo.getTexture(FrameBufferAttachmentType.Color),
                HR_MV_IMAGE.getAbsolutePath()
        );
    }

    private static int getGLPixelFormat(TextureFormat format) {
        return switch (format) {
            case RGBA8, RGBA16F -> GL_RGBA;
            case RGB8, R11G11B10F -> GL_RGB;
            case RG8, RG16F, RG32F -> GL_RG;
            case R8, R16F, R32F, R32UI, R16_SNORM, DEPTH32F, DEPTH24_STENCIL8, DEPTH24 -> GL_RED;
        };
    }

    private static int getGLPixelType(TextureFormat format) {
        return switch (format) {
            case RGBA16F, RG16F, R16F, R16_SNORM -> GL_HALF_FLOAT;
            case RGBA8, RGB8, RG8, R8, DEPTH24 -> GL_UNSIGNED_BYTE;
            case R32F, RG32F, DEPTH32F -> GL_FLOAT;
            case R32UI -> GL_UNSIGNED_INT;
            case DEPTH24_STENCIL8 -> GL_UNSIGNED_INT_24_8;
            case R11G11B10F -> GL_UNSIGNED_INT_10F_11F_11F_REV;
        };
    }

    private static boolean readPixels(ITexture texture, ByteBuffer buffer, int offset, int length) {
        GlFrameBuffer temp = GlFrameBuffer.create(
                texture,
                null,
                texture.getWidth(),
                texture.getHeight()
        );
        glBindFramebuffer(GL_FRAMEBUFFER, temp.handle());
        glPixelStorei(GL_PACK_ALIGNMENT, 1);

        int pixelFormat = getGLPixelFormat(texture.getTextureFormat());
        int pixelType = getGLPixelType(texture.getTextureFormat());

        try {
            glReadPixels(0, 0, texture.getWidth(), texture.getHeight(), pixelFormat, pixelType, buffer);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to read pixels from texture");
            e.printStackTrace();
            return false;
        } finally {
            temp.destroy();
        }
    }
}