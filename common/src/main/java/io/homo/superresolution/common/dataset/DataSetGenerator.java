package io.homo.superresolution.common.dataset;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import io.homo.superresolution.api.event.LevelRenderEndEvent;
import io.homo.superresolution.api.event.LevelRenderStartEvent;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobBuilders;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineJobResource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.utils.GlTextureCopier;
import net.minecraft.client.KeyMapping;
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
    public static File OUTPUT_DIR = Paths.get("K:/", "msrDataset").toFile();
    private static final KeyMapping SAVE_KEYMAPPING = new KeyMapping(
            "key.super_resolution.save_data",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F7,
            "Super Resolution"
    );
    private static ITexture tempTexture;

    public static void init() {
        KeyMappingRegistry.register(SAVE_KEYMAPPING);
        LevelRenderStartEvent.EVENT.register(DataSetGenerator::onLevelBegin);
        LevelRenderEndEvent.EVENT.register(DataSetGenerator::onLevelEnd);
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

        int bytesPerPixel = format.getBytesPerPixel();

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
        OUTPUT_DIR = Paths.get(SuperResolutionConfig.DATASET_PATH.get()).toFile();
        if (!OUTPUT_DIR.exists() && !OUTPUT_DIR.mkdirs()) {
            System.err.println("Failed to create output directory: " + OUTPUT_DIR.getAbsolutePath());
        }

        int frameCount = MinecraftRenderHandle.getFrameCount();
        if (!SAVE_KEYMAPPING.isDown()) return;
        File dir = Paths.get(OUTPUT_DIR.getPath(), String.valueOf(frameCount)).toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("Failed to create frame output directory: " + dir.getAbsolutePath());
        }
        if (tempTexture == null) {
            tempTexture = GlTexture2D.create(TextureDescription.create()
                    .size(1, 1)
                    .type(TextureType.Texture2D)
                    .mipmapsDisabled()
                    .usages(TextureUsages.create().sampler())
                    .format(TextureFormat.RGB16F)
                    .build());
        }
        tempTexture.resize(MinecraftRenderHandle.getRenderWidth(), MinecraftRenderHandle.getRenderHeight());
    }

    private static void onLevelEnd() {
        int frameCount = MinecraftRenderHandle.getFrameCount();
        if (!SAVE_KEYMAPPING.isDown()) return;
        File dir = Paths.get(OUTPUT_DIR.getPath(), String.valueOf(frameCount)).toFile();
        File HR_RGB_IMAGE = Paths.get(dir.getPath(), "hr_rgb.bin").toFile();
        GlTextureCopier.copy(
                CopyOperation.create()
                        .src(MinecraftRenderHandle.getRenderTarget().getTexture(FrameBufferAttachmentType.Color))
                        .dst(tempTexture)
                        .fromTo(CopyOperation.TextureChancel.R, CopyOperation.TextureChancel.R)
                        .fromTo(CopyOperation.TextureChancel.G, CopyOperation.TextureChancel.G)
                        .fromTo(CopyOperation.TextureChancel.B, CopyOperation.TextureChancel.B)
        );
        writeImage(
                tempTexture,
                HR_RGB_IMAGE.getAbsolutePath()
        );
    }

    private static int getGLPixelFormat(TextureFormat format) {
        return switch (format) {
            case RGBA8, RGBA16F, RGBA16 -> GL_RGBA;
            case RGB8, R11G11B10F, RGB16F -> GL_RGB;
            case RG8, RG16F, RG32F -> GL_RG;
            case R8, R16F, R32F, R32UI, R16_SNORM, DEPTH32F, DEPTH24_STENCIL8, DEPTH24, DEPTH32, DEPTH_COMPONENT,
                 DEPTH32F_STENCIL8 -> GL_RED;
        };
    }

    private static int getGLPixelType(TextureFormat format) {
        return switch (format) {
            case RGBA16F, RG16F, R16F, R16_SNORM, RGB16F -> GL_HALF_FLOAT;
            case RGBA8, RGB8, RG8, R8, DEPTH24, DEPTH32, DEPTH_COMPONENT, RGBA16 -> GL_UNSIGNED_BYTE;
            case R32F, RG32F, DEPTH32F, DEPTH32F_STENCIL8 -> GL_FLOAT;
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
        glBindFramebuffer(GL_FRAMEBUFFER, (int) temp.handle());
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