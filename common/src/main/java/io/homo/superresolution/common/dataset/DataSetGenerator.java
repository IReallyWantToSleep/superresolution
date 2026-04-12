/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.dataset;

import com.mojang.blaze3d.platform.InputConstants;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.LevelRenderEndEvent;
import io.homo.superresolution.api.event.LevelRenderStartEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.SuperResolutionKeyMapping;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.CopyOperation;
import io.homo.superresolution.core.graphics.impl.buffer.*;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.opengl.utils.GlTextureCopier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.opengl.GL33.*;

public class DataSetGenerator {
    public static File OUTPUT_DIR = Paths.get(SuperResolutionConfig.DATASET_PATH.get()).toFile();
    #if MC_VER > MC_1_21_8
    public static final KeyMapping SAVE_KEYMAPPING = new KeyMapping(
            "key.super_resolution.save_data",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F7,
            SuperResolutionKeyMapping.CATEGORY
    );
    #else
    public static final KeyMapping SAVE_KEYMAPPING = new KeyMapping(
            "key.super_resolution.save_data",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F7,
            "Super Resolution"
    );
    #endif
    #if MC_VER > MC_1_21_8
    public static final KeyMapping SEQUENCE_KEYMAPPING = new KeyMapping(
            "key.super_resolution.sequence_capture",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F8,
            SuperResolutionKeyMapping.CATEGORY
    );
    #else
    public static final KeyMapping SEQUENCE_KEYMAPPING = new KeyMapping(
            "key.super_resolution.sequence_capture",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F8,
            "Super Resolution"
    );
    #endif
    private static ITexture tempColorTexture;
    private static ITexture tempDepthTexture;

    private static IFrameBuffer preprocessDepthFrameBuffer;
    private static IShaderProgram depthPreprocessShader;
    private static IBufferData depthPreprocessConfigData;
    private static IBuffer depthPreprocessConfigUBO;

    private static String id = UUID.randomUUID().toString().replace("-", "");
    private static String previousId;

    private static final int PBO_RING_SIZE = 8;

    private static boolean seqActive = false;
    private static boolean seqDraining = false;
    private static int seqFrameIndex = 0;
    private static int seqTotalDrained = 0;
    private static String seqSessionId;
    private static File seqOutputDir;
    private static boolean seqKeyWasDown = false;

    private static final int[] seqColorPBOs = new int[PBO_RING_SIZE];
    private static final int[] seqDepthPBOs = new int[PBO_RING_SIZE];
    private static final long[] seqFences = new long[PBO_RING_SIZE];
    private static final int[] seqPboFrameIndex = new int[PBO_RING_SIZE];
    private static int seqPboHead = 0;
    private static int seqPboTail = 0;
    private static int seqPboCount = 0;

    private static int seqColorDataSize = 0;
    private static int seqDepthDataSize = 0;
    private static int seqWidth = 0;
    private static int seqHeight = 0;
    private static TextureFormat seqColorFormat;
    private static TextureFormat seqDepthFormat;

    private static final List<Float> seqFrameDepthFar = new ArrayList<>();

    private static ExecutorService seqDiskExecutor;


    public static void init() {

        SuperResolutionAPI.EVENT_BUS.addListener(DataSetGenerator::onLevelBegin);
        SuperResolutionAPI.EVENT_BUS.addListener(DataSetGenerator::onLevelEnd);
    }

    private static void writeImage(ITexture texture, String path) {
        Path outputPath = Paths.get(path);
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            SuperResolution.LOGGER.error("Failed to create parent directories for: " + path);
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
            SuperResolution.LOGGER.error("Failed to read pixel data from texture");
            return;
        }


        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            byte[] arr = new byte[dataSize];
            pixelData.get(arr);
            fos.write(arr);
        } catch (IOException e) {
            SuperResolution.LOGGER.error("Failed to write texture data to file: " + path);
            e.printStackTrace();
        } finally {
            MemoryUtil.memFree(pixelData);
        }
    }

    private static void onLevelBegin(LevelRenderStartEvent event) {
        int frameCount = RenderHandlerManager.getFrameCount();

        boolean sequenceKeyDown = SEQUENCE_KEYMAPPING.isDown();
        if (sequenceKeyDown && !seqKeyWasDown && !seqActive && !seqDraining) {
            initSequenceCapture();
        }
        seqKeyWasDown = sequenceKeyDown;

        if (!SAVE_KEYMAPPING.isDown()) {
            previousId = id;
            return;
        }
        OUTPUT_DIR = Paths.get(SuperResolutionConfig.DATASET_PATH.get()).toFile();
        if (!OUTPUT_DIR.exists() && !OUTPUT_DIR.mkdirs()) {
            SuperResolution.LOGGER.error("Failed to create output directory: " + OUTPUT_DIR.getAbsolutePath());
        }

        File dir = Paths.get(OUTPUT_DIR.getPath()).toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            SuperResolution.LOGGER.error("Failed to create frame output directory: " + dir.getAbsolutePath());
        }
        if (tempColorTexture != null) {
            tempColorTexture.destroy();
        }
        tempColorTexture = GlTexture2D.create(TextureDescription.create()
                .size(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight())
                .type(TextureType.Texture2D)
                .mipmapsDisabled()
                .usages(TextureUsages.create().sampler())
                .format(TextureFormat.RGB16F)
                .build());

        if (tempDepthTexture != null) {
            tempDepthTexture.destroy();
        }
        tempDepthTexture = GlTexture2D.create(TextureDescription.create()
                .size(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight())
                .type(TextureType.Texture2D)
                .mipmapsDisabled()
                .usages(TextureUsages.create().sampler())
                .format(TextureFormat.R32F)
                .build());

        if (preprocessDepthFrameBuffer == null) {
            preprocessDepthFrameBuffer = RenderSystems.current().device().createFramebuffer(
                    FramebufferDescription.create()
                            .colorAttachment(tempDepthTexture)
                            .build()
            );
        }
        if (depthPreprocessConfigData == null) {
            depthPreprocessConfigData = Std140StructBuilder.start()
                    .floatEntry("near")
                    .floatEntry("far")
                    .build();
        }
        if (depthPreprocessConfigUBO == null) {
            depthPreprocessConfigUBO = RenderSystems.current().device().createBuffer(
                    BufferDescription.create()
                            .size(depthPreprocessConfigData.size())
                            .usage(BufferUsage.Ubo)
                            .build()
            );
            depthPreprocessConfigUBO.setBufferData(depthPreprocessConfigData);
        }
        if (depthPreprocessShader == null) {
            depthPreprocessShader = RenderSystems.current().device().createShaderProgram(
                    ShaderDescription.graphics(
                                    new ShaderSource(ShaderType.Fragment, "/shader/preprocess_depth.frag.glsl", true),
                                    new ShaderSource(ShaderType.Vertex, "/shader/preprocess_depth.vert.glsl", true)
                            )
                            .name("SRPreprocessDepthShader")
                            .uniformBuffer("camera_config", 0, (int) depthPreprocessConfigData.size())
                            .uniformSamplerTexture("tex", 0)
                            .build()
            );
            depthPreprocessShader.compile();
        }

        previousId = id;
        id = UUID.randomUUID().toString().replace("-", "");
    }

    private static String getTextureName(ITexture texture, String desc, String id, String previousId) {
        if (desc.contains("Depth")) {
            return "%s-%s_%s-%s_%s.%s+%s!%s_%s.texture.bin".formatted(
                    desc,
                    texture.getWidth(),
                    texture.getHeight(),
                    texture.getTextureFormat().name(),
                    texture.getTextureFormat().getChannelCount(),
                    id,
                    previousId,
                    String.valueOf(MinecraftUtils.getCameraNear()),
                    String.valueOf(MinecraftUtils.getCameraFar())
            );
        }
        return "%s-%s_%s-%s_%s.%s+%s.texture.bin".formatted(
                desc,
                texture.getWidth(),
                texture.getHeight(),
                texture.getTextureFormat().name(),
                texture.getTextureFormat().getChannelCount(),
                id,
                previousId
        );
    }

    private static void onLevelEnd(LevelRenderEndEvent event) {
        int frameCount = RenderHandlerManager.getFrameCount();

        handleSequenceCapture();

        if (!SAVE_KEYMAPPING.isDown()) {
            previousId = id;
            return;
        }
        File dir = Paths.get(OUTPUT_DIR.getPath()).toFile();
        File HR_RGB_IMAGE = Paths.get(dir.getPath(), getTextureName(RenderHandlerManager.getRenderTarget().getTexture(FrameBufferAttachmentType.Color), "Color", id, previousId)).toFile();
        GlTextureCopier.copy(
                CopyOperation.create()
                        .src(RenderHandlerManager.getRenderTarget().getTexture(FrameBufferAttachmentType.Color))
                        .dst(tempColorTexture)
                        .fromTo(CopyOperation.TextureChannel.R, CopyOperation.TextureChannel.R)
                        .fromTo(CopyOperation.TextureChannel.G, CopyOperation.TextureChannel.G)
                        .fromTo(CopyOperation.TextureChannel.B, CopyOperation.TextureChannel.B)
        );
        writeImage(
                RenderHandlerManager.getRenderTarget().getTexture(FrameBufferAttachmentType.Color),
                HR_RGB_IMAGE.getAbsolutePath()
        );
        SuperResolution.LOGGER.info("写入{}", HR_RGB_IMAGE.getAbsolutePath());

        File HR_DEPTH_IMAGE = Paths.get(dir.getPath(), getTextureName(tempDepthTexture, "Depth", id, previousId)).toFile();
        writeImage(
                RenderHandlerManager.getDepthTexture(),
                HR_DEPTH_IMAGE.getAbsolutePath()
        );
        SuperResolution.LOGGER.info("写入{}", HR_DEPTH_IMAGE.getAbsolutePath());
    }

    private static int getGLPixelFormat(TextureFormat format) {
        return switch (format) {
            case RGBA8, RGBA16F, RGBA16,RGBA32F -> GL_RGBA;
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
            case R32F, RG32F, DEPTH32F, DEPTH32F_STENCIL8,RGBA32F -> GL_FLOAT;
            case R32UI -> GL_UNSIGNED_INT;
            case DEPTH24_STENCIL8 -> GL_UNSIGNED_INT_24_8;

            case R11G11B10F -> GL_UNSIGNED_INT_10F_11F_11F_REV;
        };
    }

    private static boolean readPixels(ITexture texture, ByteBuffer buffer, int offset, int length) {
        IFrameBuffer temp = RenderSystems.current().device().createFramebuffer(
                FramebufferDescription.create()
                        .colorAttachment(texture)
                        .build()
        );
        glBindFramebuffer(GL_FRAMEBUFFER, (int) ((GlFrameBuffer) temp).handle());
        glPixelStorei(GL_PACK_ALIGNMENT, 1);

        int pixelFormat = getGLPixelFormat(texture.getTextureFormat());
        int pixelType = getGLPixelType(texture.getTextureFormat());

        try {
            glReadPixels(0, 0, texture.getWidth(), texture.getHeight(), pixelFormat, pixelType, buffer);
            return true;
        } catch (Exception e) {
            SuperResolution.LOGGER.error("Failed to read pixels from texture");
            e.printStackTrace();
            return false;
        } finally {
            temp.destroy();
        }
    }



    private static void initSequenceCapture() {
        OUTPUT_DIR = Paths.get(SuperResolutionConfig.DATASET_PATH.get()).toFile();
        if (!OUTPUT_DIR.exists() && !OUTPUT_DIR.mkdirs()) {
            SuperResolution.LOGGER.error("Failed to create output directory: {}", OUTPUT_DIR.getAbsolutePath());
            return;
        }

        seqSessionId = UUID.randomUUID().toString().replace("-", "");
        seqOutputDir = Paths.get(OUTPUT_DIR.getPath(), "sequence_" + seqSessionId).toFile();
        if (!seqOutputDir.mkdirs()) {
            SuperResolution.LOGGER.error("Failed to create sequence output directory: {}", seqOutputDir.getAbsolutePath());
            return;
        }

        seqWidth = RenderHandlerManager.getRenderWidth();
        seqHeight = RenderHandlerManager.getRenderHeight();

        if (tempColorTexture != null) {
            tempColorTexture.destroy();
        }
        tempColorTexture = GlTexture2D.create(TextureDescription.create()
                .size(seqWidth, seqHeight)
                .type(TextureType.Texture2D)
                .mipmapsDisabled()
                .usages(TextureUsages.create().sampler())
                .format(TextureFormat.RGB16F)
                .build());

        if (tempDepthTexture != null) {
            tempDepthTexture.destroy();
        }
        tempDepthTexture = GlTexture2D.create(TextureDescription.create()
                .size(seqWidth, seqHeight)
                .type(TextureType.Texture2D)
                .mipmapsDisabled()
                .usages(TextureUsages.create().sampler())
                .format(TextureFormat.R32F)
                .build());

        ITexture colorSource = RenderHandlerManager.getRenderTarget().getTexture(FrameBufferAttachmentType.Color);
        ITexture depthSource = RenderHandlerManager.getDepthTexture();
        if (colorSource == null || depthSource == null) {
            SuperResolution.LOGGER.error("无法获取颜色或深度纹理，中止帧序列捕获");
            seqOutputDir.delete();
            return;
        }
        seqColorFormat = colorSource.getTextureFormat();
        seqDepthFormat = depthSource.getTextureFormat();
        seqColorDataSize = seqWidth * seqHeight * seqColorFormat.getBytesPerPixel();
        seqDepthDataSize = seqWidth * seqHeight * seqDepthFormat.getBytesPerPixel();

        for (int i = 0; i < PBO_RING_SIZE; i++) {
            seqColorPBOs[i] = glGenBuffers();
            glBindBuffer(GL_PIXEL_PACK_BUFFER, seqColorPBOs[i]);
            glBufferData(GL_PIXEL_PACK_BUFFER, seqColorDataSize, GL_STREAM_READ);

            seqDepthPBOs[i] = glGenBuffers();
            glBindBuffer(GL_PIXEL_PACK_BUFFER, seqDepthPBOs[i]);
            glBufferData(GL_PIXEL_PACK_BUFFER, seqDepthDataSize, GL_STREAM_READ);

            seqFences[i] = 0;
            seqPboFrameIndex[i] = -1;
        }
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

        seqPboHead = 0;
        seqPboTail = 0;
        seqPboCount = 0;
        seqFrameIndex = 0;
        seqTotalDrained = 0;
        seqFrameDepthFar.clear();
        seqActive = true;
        seqDraining = false;

        seqDiskExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SR-SequenceCapture-DiskWriter");
            t.setDaemon(true);
            return t;
        });

        SuperResolution.LOGGER.info("开始帧序列捕获");
    }

    private static void handleSequenceCapture() {
        if (!seqActive && !seqDraining) return;

        drainCompletedPBOs(false);

        if (seqActive) {
            boolean keyDown = SEQUENCE_KEYMAPPING.isDown();
            if (!keyDown) {
                seqActive = false;
                seqDraining = true;
                SuperResolution.LOGGER.info("帧序列捕获结束（按键释放，已捕获 {} 帧）", seqFrameIndex);
            } else if (RenderHandlerManager.getRenderWidth() != seqWidth ||
                    RenderHandlerManager.getRenderHeight() != seqHeight) {
                SuperResolution.LOGGER.warn("渲染分辨率在帧序列捕获期间发生变化，中止捕获");
                seqActive = false;
                seqDraining = true;
            } else {
                captureFrameIntoPBO();
            }
        }

        if (seqDraining && seqPboCount == 0) {
            finishSequenceCapture();
        }
    }

    private static void captureFrameIntoPBO() {
        if (seqPboCount >= PBO_RING_SIZE) {
            drainCompletedPBOs(true);
        }

        int slot = seqPboHead;
        int frameIdx = seqFrameIndex;

        GlTextureCopier.copy(
                CopyOperation.create()
                        .src(RenderHandlerManager.getRenderTarget().getTexture(FrameBufferAttachmentType.Color))
                        .dst(tempColorTexture)
                        .fromTo(CopyOperation.TextureChannel.R, CopyOperation.TextureChannel.R)
                        .fromTo(CopyOperation.TextureChannel.G, CopyOperation.TextureChannel.G)
                        .fromTo(CopyOperation.TextureChannel.B, CopyOperation.TextureChannel.B)
        );

        readPixelsIntoPBO(tempColorTexture, seqColorPBOs[slot]);
        readPixelsIntoPBO(RenderHandlerManager.getDepthTexture(), seqDepthPBOs[slot]);

        seqFences[slot] = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        seqPboFrameIndex[slot] = frameIdx;

        seqFrameDepthFar.add(MinecraftUtils.getCameraFar());

        seqPboHead = (seqPboHead + 1) % PBO_RING_SIZE;
        seqPboCount++;
        seqFrameIndex++;
    }

    private static void readPixelsIntoPBO(ITexture texture, int pbo) {
        IFrameBuffer temp = RenderSystems.current().device().createFramebuffer(
                FramebufferDescription.create()
                        .colorAttachment(texture)
                        .build()
        );
        glBindFramebuffer(GL_FRAMEBUFFER, (int) ((GlFrameBuffer) temp).handle());
        glPixelStorei(GL_PACK_ALIGNMENT, 1);

        int pixelFormat = getGLPixelFormat(texture.getTextureFormat());
        int pixelType = getGLPixelType(texture.getTextureFormat());

        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbo);
        glReadPixels(0, 0, texture.getWidth(), texture.getHeight(), pixelFormat, pixelType, 0L);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

        temp.destroy();
    }

    private static void drainCompletedPBOs(boolean forceWait) {
        while (seqPboCount > 0) {
            int slot = seqPboTail;
            long fence = seqFences[slot];
            if (fence == 0) break;

            if (forceWait) {
                glClientWaitSync(fence, GL_SYNC_FLUSH_COMMANDS_BIT, Long.MAX_VALUE);
            } else {
                int status = glClientWaitSync(fence, 0, 0);
                if (status != GL_ALREADY_SIGNALED && status != GL_CONDITION_SATISFIED) {
                    break;
                }
            }

            glDeleteSync(fence);
            seqFences[slot] = 0;

            int frameIdx = seqPboFrameIndex[slot];

            byte[] colorData = mapPBOData(seqColorPBOs[slot], seqColorDataSize);
            byte[] depthData = mapPBOData(seqDepthPBOs[slot], seqDepthDataSize);

            final int fi = frameIdx;
            final float depthFar = seqFrameDepthFar.get(frameIdx);
            final String sessionId = seqSessionId;
            final File outputDir = seqOutputDir;
            final int w = seqWidth;
            final int h = seqHeight;
            final TextureFormat cFmt = seqColorFormat;
            final TextureFormat dFmt = seqDepthFormat;

            seqDiskExecutor.submit(() ->
                    writeSequenceFrame(fi, colorData, depthData, sessionId, outputDir, w, h, cFmt, dFmt, depthFar));

            seqPboTail = (seqPboTail + 1) % PBO_RING_SIZE;
            seqPboCount--;
            seqTotalDrained++;

            if (forceWait) break;
        }
    }

    private static byte[] mapPBOData(int pbo, int dataSize) {
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbo);
        ByteBuffer mapped = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY);
        if (mapped == null) {
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
            SuperResolution.LOGGER.error("Failed to map PBO for readback");
            return new byte[0];
        }
        byte[] data = new byte[dataSize];
        mapped.get(data);
        glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
        return data;
    }

    private static void writeSequenceFrame(int frameIndex, byte[] colorData, byte[] depthData,
                                           String sessionId, File outputDir, int width, int height,
                                           TextureFormat colorFormat, TextureFormat depthFormat, float depthFar) {
        String colorFileName = "frame_%04d_Color-%d_%d-%s_%d.%s.texture.bin".formatted(
                frameIndex, width, height,
                colorFormat.name(), colorFormat.getChannelCount(),
                sessionId
        );
        String depthFileName = "frame_%04d_Depth-%d_%d-%s_%d.%s!%s_%s.texture.bin".formatted(
                frameIndex, width, height,
                depthFormat.name(), depthFormat.getChannelCount(),
                sessionId,
                "0.05", String.valueOf(depthFar)
        );

        writeBytes(colorData, new File(outputDir, colorFileName));
        writeBytes(depthData, new File(outputDir, depthFileName));

        SuperResolution.LOGGER.info("写入帧序列帧 {}: {}", frameIndex, colorFileName);
    }

    private static void writeBytes(byte[] data, File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            SuperResolution.LOGGER.error("Failed to write sequence frame: {}", file.getAbsolutePath(), e);
        }
    }

    private static void finishSequenceCapture() {
        for (int i = 0; i < PBO_RING_SIZE; i++) {
            if (seqColorPBOs[i] != 0) {
                glDeleteBuffers(seqColorPBOs[i]);
                seqColorPBOs[i] = 0;
            }
            if (seqDepthPBOs[i] != 0) {
                glDeleteBuffers(seqDepthPBOs[i]);
                seqDepthPBOs[i] = 0;
            }
        }

        seqDraining = false;
        seqDiskExecutor.shutdown();
        seqDiskExecutor = null;

        SuperResolution.LOGGER.info("帧序列捕获全部完成 ({} 帧): {}", seqTotalDrained, seqOutputDir.getAbsolutePath());
    }
}

