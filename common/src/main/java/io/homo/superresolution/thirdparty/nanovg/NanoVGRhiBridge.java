/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.thirdparty.nanovg;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsages;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.state.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.impl.vertex.*;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties;

public final class NanoVGRhiBridge {
    private static final int NVG_IMAGE_GENERATE_MIPMAPS = 1 << 0;
    private static final int NVG_IMAGE_REPEATX = 1 << 1;
    private static final int NVG_IMAGE_REPEATY = 1 << 2;
    private static final int NVG_IMAGE_NEAREST = 1 << 5;

    private static final int NVG_TEXTURE_ALPHA = 0x01;
    private static final int NVG_TEXTURE_RGBA = 0x02;

    private static final int GLNVG_FILL = 1;
    private static final int GLNVG_CONVEXFILL = 2;
    private static final int GLNVG_STROKE = 3;
    private static final int GLNVG_TRIANGLES = 4;

    private static final int STENCIL_MODE_DISABLED = 0;
    private static final int STENCIL_MODE_FILL_WRITE = 1;
    private static final int STENCIL_MODE_FILL_AA = 2;
    private static final int STENCIL_MODE_FILL_CLEAR = 3;
    private static final int STENCIL_MODE_STROKE_BASE = 4;
    private static final int STENCIL_MODE_STROKE_AA = 5;
    private static final int STENCIL_MODE_STROKE_CLEAR = 6;

    private static final int VERTEX_STRIDE_BYTES = 16;
    private static final int PATH_STRIDE_BYTES = 16;
    private static final int CALL_STRIDE_BYTES = 48;
    private static final int CALL_STRIDE_BYTES_LEGACY = 44;
    private static final long SHARED_RENDER_PASS_KEY = 0L;

    private static final Map<Integer, ITexture> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<Long, RenderPass> PASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Long, GraphicsPipeline> GRAPHICS_PIPELINE_CACHE = new ConcurrentHashMap<>();
    private static final int NVG_BF_ZERO = 0;
    private static final int NVG_BF_ONE = 1;
    private static final int NVG_BF_SRC_COLOR = 0x0300;
    private static final int NVG_BF_ONE_MINUS_SRC_COLOR = 0x0301;
    private static final int NVG_BF_SRC_ALPHA = 0x0302;
    private static final int NVG_BF_ONE_MINUS_SRC_ALPHA = 0x0303;
    private static final int NVG_BF_DST_ALPHA = 0x0304;
    private static final int NVG_BF_ONE_MINUS_DST_ALPHA = 0x0305;
    private static final int NVG_BF_DST_COLOR = 0x0306;
    private static final int NVG_BF_ONE_MINUS_DST_COLOR = 0x0307;
    private static final int NVG_BF_SRC_ALPHA_SATURATE = 0x0308;
    private static IFrameBuffer targetFramebuffer;
    private static ITexture dummyTexture;
    private static IShaderProgram shader;
    private static IBuffer frameUniformBuffer;
    private static IBuffer fragUniformBuffer;
    private static IVertexBuffer dynamicVertexBuffer;
    private static IDevice device = RenderSystems.current().device();
    private static int dynamicVertexCapacity;
    private static int frameVertexWriteOffset;
    private static int fragUniformWriteOffset;
    private static int fragUniformSourceStride;
    private static int fragUniformBindingStride;
    private static ByteBuffer stagedFragUniforms;
    private static ICommandBuffer activeCommandBuffer;
    private static FlushBatchState activeFlushBatchState;
    private static IDevice activeBatchDevice;
    private static IFrameBuffer activeBatchFramebuffer;
    private static float activeBatchViewWidth;
    private static float activeBatchViewHeight;
    private static int activeBatchFragUniformSourceStride;
    private static int activeBatchFragUniformBindingStride;
    private static boolean explicitBatchActive;
    private static float viewportWidth;
    private static float viewportHeight;

    private NanoVGRhiBridge() {
    }

    public static void setTargetFramebuffer(IFrameBuffer framebuffer) {
        if (targetFramebuffer != framebuffer) {
            closeActiveBatch(true);
            targetFramebuffer = framebuffer;
            GRAPHICS_PIPELINE_CACHE.values().forEach(GraphicsPipeline::destroy);
            GRAPHICS_PIPELINE_CACHE.clear();
            PASS_CACHE.values().forEach(RenderPass::destroy);
            PASS_CACHE.clear();
        }
    }

    public static void beginBatch(IDevice device) {
        explicitBatchActive = true;
        NanoVGRhiBridge.device = device;
    }

    public static void endBatch() {
        explicitBatchActive = false;
        closeActiveBatch(true);
    }

    public static boolean nCreateTexture(int imageId, int type, int width, int height, int imageFlags, ByteBuffer data, int dataSize) {
        TextureFormat format;
        if (type == NVG_TEXTURE_RGBA) {
            format = TextureFormat.RGBA8;
        } else if (type == NVG_TEXTURE_ALPHA) {
            format = TextureFormat.R8;
        } else {
            return false;
        }
        TextureFilterMode filterMode = (imageFlags & NVG_IMAGE_NEAREST) != 0 ? TextureFilterMode.Nearest : TextureFilterMode.Linear;
        TextureWrapMode wrapMode = ((imageFlags & NVG_IMAGE_REPEATX) != 0 || (imageFlags & NVG_IMAGE_REPEATY) != 0)
                ? TextureWrapMode.Repeat
                : TextureWrapMode.ClampToEdge;

        TextureDescription description = TextureDescription.create()
                .type(TextureType.Texture2D)
                .format(format)
                .size(width, height)
                .usages(TextureUsages.create().sampler().transferDestination())
                .filterMode(filterMode)
                .wrapMode(wrapMode)
                .mipmapSettings((imageFlags & NVG_IMAGE_GENERATE_MIPMAPS) != 0
                        ? TextureMipmapSettings.auto()
                        : TextureMipmapSettings.disabled())
                .label("NanoVGImage-" + imageId)
                .build();

        ITexture texture = device.createTexture(description);
        if (texture == null) {
            return false;
        }

        if (data != null && dataSize > 0) {
            ByteBuffer upload = duplicateForUpload(data, dataSize);
            ICommandBuffer commandBuffer = device.defaultCommandPool().createCommandBuffer(CommandBufferBehavior.OneTimeSubmit);
            commandBuffer.begin();
            commandBuffer.writeToTexture(texture, upload, 0, 0, width, height);
            commandBuffer.end();
            commandBuffer.submit(device);
            commandBuffer.waitForFence();
            MemoryUtil.memFree(upload);
        }

        ITexture previous = TEXTURES.put(imageId, texture);
        if (previous != null) {
            previous.destroy();
        }
        return true;
    }

    public static boolean nRegisterExternalTexture(int imageId, int externalTextureHandle, int width, int height, int imageFlags) {
        TextureFormat format = TextureFormat.RGBA8;
        TextureFilterMode filterMode = (imageFlags & NVG_IMAGE_NEAREST) != 0 ? TextureFilterMode.Nearest : TextureFilterMode.Linear;
        TextureWrapMode wrapMode = ((imageFlags & NVG_IMAGE_REPEATX) != 0 || (imageFlags & NVG_IMAGE_REPEATY) != 0)
                ? TextureWrapMode.Repeat
                : TextureWrapMode.ClampToEdge;

        ITexture external = new ExternalTextureRef(externalTextureHandle, width, height, format, filterMode, wrapMode);
        ITexture previous = TEXTURES.put(imageId, external);
        if (previous != null) {
            previous.destroy();
        }
        return true;
    }

    public static boolean nUpdateTexture(int imageId, int x, int y, int width, int height, ByteBuffer data, int dataSize) {
        ITexture texture = TEXTURES.get(imageId);
        if (texture == null) {
            return false;
        }
        if (data == null || dataSize <= 0) {
            return true;
        }
        ByteBuffer upload = duplicateForUpload(data, dataSize);
        ICommandBuffer commandBuffer = device.defaultCommandPool().createCommandBuffer(CommandBufferBehavior.OneTimeSubmit);
        commandBuffer.begin();
        commandBuffer.writeToTexture(texture, upload, x, y, width, height);
        commandBuffer.end();
        commandBuffer.submit(device);
        commandBuffer.waitForFence();
        MemoryUtil.memFree(upload);
        return true;
    }

    public static void nDeleteTexture(int imageId) {
        ITexture texture = TEXTURES.remove(imageId);
        if (texture != null) {
            texture.destroy();
        }
    }

    public static void nViewport(float width, float height, float devicePixelRatio) {
        viewportWidth = width;
        viewportHeight = height;
    }

    public static void nFlush(float viewWidth,
                              float viewHeight,
                              ByteBuffer verts,
                              int nverts,
                              ByteBuffer paths,
                              int npaths,
                              ByteBuffer calls,
                              int ncalls,
                              ByteBuffer uniforms,
                              int uniformBytes,
                              int fragSize) {
        if (ncalls <= 0 || nverts <= 0 || verts == null || calls == null || uniforms == null || fragSize <= 0) {
            return;
        }

        IDevice device = RenderSystems.current().device();

        if (targetFramebuffer == null) {
            return;
        }

        ensureRendererResources(device);

        viewportWidth = viewWidth;
        viewportHeight = viewHeight;

        ByteBuffer vertsData = verts.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer pathsData = paths == null ? null : paths.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer callsData = calls.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer uniformsData = uniforms.duplicate().order(ByteOrder.nativeOrder());

        int maxVertCount = Math.min(nverts, vertsData.remaining() / VERTEX_STRIDE_BYTES);
        int maxPathCount = pathsData == null ? 0 : Math.min(npaths, pathsData.remaining() / PATH_STRIDE_BYTES);
        int maxUniformBytes = Math.min(uniformBytes, uniformsData.remaining());
        uniformsData.limit(uniformsData.position() + maxUniformBytes);
        if (maxVertCount <= 0) {
            return;
        }

        fragUniformSourceStride = fragSize;
        fragUniformBindingStride = resolveFragUniformBindingStride(device, fragSize);
        if (fragUniformBindingStride <= 0) {
            return;
        }

        int callStride = resolveCallStride(callsData, ncalls);
        if (callStride == 0) {
            return;
        }

        int requiredVertexBytes = estimateRequiredVertexBytes(
                pathsData,
                maxPathCount,
                callsData,
                ncalls,
                callStride,
                maxVertCount,
                uniformsData.limit(),
                fragSize
        );
        if (requiredVertexBytes <= 0) {
            return;
        }

        ByteBuffer packedUniforms = packFragUniforms(uniformsData, maxUniformBytes, fragUniformSourceStride, fragUniformBindingStride);
        int packedUniformBytes = packedUniforms.remaining();

        ensureActiveBatch(device, viewWidth, viewHeight, requiredVertexBytes, packedUniformBytes);
        int uniformBaseOffset = appendFragUniforms(packedUniforms);

        ICommandBuffer commandBuffer = activeCommandBuffer;
        FlushBatchState batchState = activeFlushBatchState;
        for (int i = 0; i < ncalls; i++) {
            int callBase = i * callStride;
            if (callBase < 0 || callBase + CALL_STRIDE_BYTES_LEGACY > callsData.limit()) {
                continue;
            }

            int type = callsData.getInt(callBase);
            int image = callsData.getInt(callBase + 4);
            int pathOffset = callsData.getInt(callBase + 8);
            int pathCount = callsData.getInt(callBase + 12);
            int triangleOffset = callsData.getInt(callBase + 16);
            int triangleCount = callsData.getInt(callBase + 20);
            int uniformOffset = callsData.getInt(callBase + 24);
            int uniformCount;
            int blendSrcRgb;
            int blendDstRgb;
            int blendSrcAlpha;
            int blendDstAlpha;
            if (callStride >= CALL_STRIDE_BYTES) {
                if (callBase + CALL_STRIDE_BYTES > callsData.limit()) {
                    continue;
                }
                uniformCount = callsData.getInt(callBase + 28);
                blendSrcRgb = callsData.getInt(callBase + 32);
                blendDstRgb = callsData.getInt(callBase + 36);
                blendSrcAlpha = callsData.getInt(callBase + 40);
                blendDstAlpha = callsData.getInt(callBase + 44);
            } else {
                uniformCount = inferLegacyUniformCount(type);
                blendSrcRgb = callsData.getInt(callBase + 28);
                blendDstRgb = callsData.getInt(callBase + 32);
                blendSrcAlpha = callsData.getInt(callBase + 36);
                blendDstAlpha = callsData.getInt(callBase + 40);
            }

            if (!isCallRangeValid(pathOffset, pathCount, maxPathCount)) {
                continue;
            }
            if (!isVertexRangeValid(triangleOffset, triangleCount, maxVertCount)) {
                continue;
            }
            if (!isUniformRangeValid(uniformOffset, fragSize, uniformCount, uniformsData.limit())) {
                continue;
            }

            switch (type) {
                case GLNVG_FILL -> drawFill(
                        device,
                        commandBuffer,
                        batchState,
                        vertsData,
                        pathsData,
                        uniformsData,
                        uniformBaseOffset,
                        fragSize,
                        image,
                        pathOffset,
                        pathCount,
                        triangleOffset,
                        triangleCount,
                        uniformOffset,
                        uniformCount,
                        maxVertCount,
                        maxPathCount,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                case GLNVG_CONVEXFILL -> drawConvexFill(
                        device,
                        commandBuffer,
                        batchState,
                        vertsData,
                        pathsData,
                        uniformsData,
                        uniformBaseOffset,
                        fragSize,
                        image,
                        pathOffset,
                        pathCount,
                        maxVertCount,
                        maxPathCount,
                        uniformOffset,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                case GLNVG_STROKE -> drawStroke(
                        device,
                        commandBuffer,
                        batchState,
                        vertsData,
                        pathsData,
                        uniformsData,
                        uniformBaseOffset,
                        fragSize,
                        image,
                        pathOffset,
                        pathCount,
                        maxVertCount,
                        maxPathCount,
                        uniformOffset,
                        uniformCount,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                case GLNVG_TRIANGLES -> drawTrianglesRange(
                        device,
                        commandBuffer,
                        batchState,
                        vertsData,
                        uniformsData,
                        uniformBaseOffset,
                        fragSize,
                        image,
                        triangleOffset,
                        triangleCount,
                        maxVertCount,
                        uniformOffset,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                default -> {
                }
            }
        }
        if (!explicitBatchActive) {
            closeActiveBatch(true);
        }
    }

    public static void nDestroy() {
        explicitBatchActive = false;
        closeActiveBatch(false);

        for (ITexture texture : TEXTURES.values()) {
            texture.destroy();
        }
        TEXTURES.clear();

        GRAPHICS_PIPELINE_CACHE.values().forEach(GraphicsPipeline::destroy);
        GRAPHICS_PIPELINE_CACHE.clear();
        PASS_CACHE.values().forEach(RenderPass::destroy);
        PASS_CACHE.clear();

        if (dynamicVertexBuffer != null) {
            dynamicVertexBuffer.destroy();
            dynamicVertexBuffer = null;
            dynamicVertexCapacity = 0;
        }
        frameVertexWriteOffset = 0;
        fragUniformWriteOffset = 0;
        fragUniformSourceStride = 0;
        fragUniformBindingStride = 0;
        stagedFragUniforms = null;
        if (frameUniformBuffer != null) {
            frameUniformBuffer.destroy();
            frameUniformBuffer = null;
        }
        if (fragUniformBuffer != null) {
            fragUniformBuffer.destroy();
            fragUniformBuffer = null;
        }
        if (dummyTexture != null) {
            dummyTexture.destroy();
            dummyTexture = null;
        }
        if (shader != null) {
            shader.destroy();
            shader = null;
        }
    }

    private static ByteBuffer duplicateForUpload(ByteBuffer src, int size) {
        ByteBuffer copy = MemoryUtil.memAlloc(size);
        ByteBuffer temp = src.duplicate();
        temp.clear();
        int oldLimit = temp.limit();
        if (size < oldLimit) {
            temp.limit(size);
        }
        copy.put(temp);
        copy.flip();
        return copy;
    }

    private static void ensureRendererResources(IDevice device) {
        if (shader == null) {
            shader = device.createShaderProgram(
                    ShaderDescription.graphics(
                                    ShaderSource.file(ShaderType.Fragment, "/shader/nanovg/nanovg_rhi.frag.glsl"),
                                    ShaderSource.file(ShaderType.Vertex, "/shader/nanovg/nanovg_rhi.vert.glsl")
                            )
                            .uniformBuffer("frame", 0, 16)
                            .uniformBuffer("frag", 1, 256)
                            .uniformSamplerTexture("tex", 2)
                            .build()
            );
            shader.compile();
        }

        if (frameUniformBuffer == null) {
            frameUniformBuffer = device.createBuffer(
                    BufferDescription.create()
                            .size(16)
                            .usages(BufferUsages.create().ubo().transferDst())
                            .build()
            );
        }

        if (fragUniformBuffer == null) {
            fragUniformBuffer = device.createBuffer(
                    BufferDescription.create()
                            .size(1024)
                            .usages(BufferUsages.create().ubo().transferDst())
                            .build()
            );
        }

        if (dummyTexture == null) {
            TextureDescription description = TextureDescription.create()
                    .type(TextureType.Texture2D)
                    .format(TextureFormat.RGBA8)
                    .size(1, 1)
                    .usages(TextureUsages.create().sampler().transferDestination())
                    .filterMode(TextureFilterMode.Linear)
                    .wrapMode(TextureWrapMode.ClampToEdge)
                    .mipmapSettings(TextureMipmapSettings.disabled())
                    .label("NanoVgDummy")
                    .build();
            dummyTexture = device.createTexture(description);
            ByteBuffer white = MemoryUtil.memAlloc(4);
            white.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
            ICommandBuffer commandBuffer = device.defaultCommandPool().createCommandBuffer(CommandBufferBehavior.OneTimeSubmit);
            commandBuffer.begin();
            commandBuffer.writeToTexture(dummyTexture, white, 0, 0, 1, 1);
            commandBuffer.end();
            commandBuffer.submit(device);
            commandBuffer.waitForFence();
            MemoryUtil.memFree(white);
        }
    }

    private static void drawFill(IDevice device,
                                 ICommandBuffer commandBuffer,
                                 FlushBatchState batchState,
                                 ByteBuffer verts,
                                 ByteBuffer paths,
                                 ByteBuffer uniforms,
                                 int uniformBaseOffset,
                                 int fragSize,
                                 int image,
                                 int pathOffset,
                                 int pathCount,
                                 int triangleOffset,
                                 int triangleCount,
                                 int uniformOffset,
                                 int uniformCount,
                                 int maxVertCount,
                                 int maxPathCount,
                                 int blendSrcRgb,
                                 int blendDstRgb,
                                 int blendSrcAlpha,
                                 int blendDstAlpha) {
        if (paths == null) {
            return;
        }

        for (int i = 0; i < pathCount; i++) {
            int pathIndex = pathOffset + i;
            if (pathIndex < 0 || pathIndex >= maxPathCount) {
                continue;
            }
            int pathBase = pathIndex * PATH_STRIDE_BYTES;
            int fillOffset = paths.getInt(pathBase);
            int fillCount = paths.getInt(pathBase + 4);
            if (fillCount >= 3 && isVertexRangeValid(fillOffset, fillCount, maxVertCount)) {
                ByteBuffer fanTriangles = buildTriangleFan(verts, fillOffset, fillCount);
                uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.Triangle, fanTriangles, uniforms, uniformBaseOffset, uniformOffset,
                        fragSize, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_FILL_WRITE, 0);
            }
        }

        if (uniformCount >= 2) {
            int paintUniformOffset = uniformOffset + fragSize;

            for (int i = 0; i < pathCount; i++) {
                int pathIndex = pathOffset + i;
                if (pathIndex < 0 || pathIndex >= maxPathCount) {
                    continue;
                }
                int pathBase = pathIndex * PATH_STRIDE_BYTES;
                int strokeOffset = paths.getInt(pathBase + 8);
                int strokeCount = paths.getInt(pathBase + 12);
                if (strokeCount > 0 && isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                    ByteBuffer strip = buildRange(verts, strokeOffset, strokeCount);
                    uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.TriangleStrip, strip, uniforms, uniformBaseOffset, paintUniformOffset,
                            fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                            STENCIL_MODE_FILL_AA, ColorComponentFlags.ALL);
                }
            }

            // Pass 3: draw bounding quad where stencil is not zero and clear stencil back to zero.
            if (triangleCount > 0 && isVertexRangeValid(triangleOffset, triangleCount, maxVertCount)) {
                ByteBuffer fillQuad = buildRange(verts, triangleOffset, triangleCount);
                uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.TriangleStrip, fillQuad, uniforms, uniformBaseOffset, paintUniformOffset,
                        fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_FILL_CLEAR, ColorComponentFlags.ALL);
            }
        }
    }

    private static void drawConvexFill(IDevice device,
                                       ICommandBuffer commandBuffer,
                                       FlushBatchState batchState,
                                       ByteBuffer verts,
                                       ByteBuffer paths,
                                       ByteBuffer uniforms,
                                       int uniformBaseOffset,
                                       int fragSize,
                                       int image,
                                       int pathOffset,
                                       int pathCount,
                                       int maxVertCount,
                                       int maxPathCount,
                                       int uniformOffset,
                                       int blendSrcRgb,
                                       int blendDstRgb,
                                       int blendSrcAlpha,
                                       int blendDstAlpha) {
        if (paths == null) {
            return;
        }
        for (int i = 0; i < pathCount; i++) {
            int pathIndex = pathOffset + i;
            if (pathIndex < 0 || pathIndex >= maxPathCount) {
                continue;
            }
            int pathBase = pathIndex * PATH_STRIDE_BYTES;
            int fillOffset = paths.getInt(pathBase);
            int fillCount = paths.getInt(pathBase + 4);
            int strokeOffset = paths.getInt(pathBase + 8);
            int strokeCount = paths.getInt(pathBase + 12);

            if (fillCount >= 3 && isVertexRangeValid(fillOffset, fillCount, maxVertCount)) {
                ByteBuffer fanTriangles = buildTriangleFan(verts, fillOffset, fillCount);
                uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.Triangle, fanTriangles, uniforms, uniformBaseOffset, uniformOffset,
                        fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_DISABLED, ColorComponentFlags.ALL);
            }
            if (strokeCount > 0 && isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                ByteBuffer strip = buildRange(verts, strokeOffset, strokeCount);
                uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.TriangleStrip, strip, uniforms, uniformBaseOffset, uniformOffset,
                        fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_DISABLED, ColorComponentFlags.ALL);
            }
        }
    }

    private static void drawStroke(IDevice device,
                                   ICommandBuffer commandBuffer,
                                   FlushBatchState batchState,
                                   ByteBuffer verts,
                                   ByteBuffer paths,
                                   ByteBuffer uniforms,
                                   int uniformBaseOffset,
                                   int fragSize,
                                   int image,
                                   int pathOffset,
                                   int pathCount,
                                   int maxVertCount,
                                   int maxPathCount,
                                   int uniformOffset,
                                   int uniformCount,
                                   int blendSrcRgb,
                                   int blendDstRgb,
                                   int blendSrcAlpha,
                                   int blendDstAlpha) {
        if (paths == null) {
            return;
        }

        if (uniformCount >= 2) {
            int baseUniformOffset = uniformOffset + fragSize;

            for (int i = 0; i < pathCount; i++) {
                int pathIndex = pathOffset + i;
                if (pathIndex < 0 || pathIndex >= maxPathCount) {
                    continue;
                }
                int pathBase = pathIndex * PATH_STRIDE_BYTES;
                int strokeOffset = paths.getInt(pathBase + 8);
                int strokeCount = paths.getInt(pathBase + 12);
                if (strokeCount <= 0 || !isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                    continue;
                }
                ByteBuffer strip = buildRange(verts, strokeOffset, strokeCount);
                uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.TriangleStrip, strip, uniforms, uniformBaseOffset, baseUniformOffset,
                        fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_STROKE_BASE, ColorComponentFlags.ALL);
            }

            for (int i = 0; i < pathCount; i++) {
                int pathIndex = pathOffset + i;
                if (pathIndex < 0 || pathIndex >= maxPathCount) {
                    continue;
                }
                int pathBase = pathIndex * PATH_STRIDE_BYTES;
                int strokeOffset = paths.getInt(pathBase + 8);
                int strokeCount = paths.getInt(pathBase + 12);
                if (strokeCount <= 0 || !isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                    continue;
                }
                ByteBuffer strip = buildRange(verts, strokeOffset, strokeCount);
                uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.TriangleStrip, strip, uniforms, uniformBaseOffset, uniformOffset,
                        fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_STROKE_AA, ColorComponentFlags.ALL);
            }

            for (int i = 0; i < pathCount; i++) {
                int pathIndex = pathOffset + i;
                if (pathIndex < 0 || pathIndex >= maxPathCount) {
                    continue;
                }
                int pathBase = pathIndex * PATH_STRIDE_BYTES;
                int strokeOffset = paths.getInt(pathBase + 8);
                int strokeCount = paths.getInt(pathBase + 12);
                if (strokeCount <= 0 || !isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                    continue;
                }
                ByteBuffer strip = buildRange(verts, strokeOffset, strokeCount);
                uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.TriangleStrip, strip, uniforms, uniformBaseOffset, uniformOffset,
                        fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_STROKE_CLEAR, 0);
            }
            return;
        }

        for (int i = 0; i < pathCount; i++) {
            int pathIndex = pathOffset + i;
            if (pathIndex < 0 || pathIndex >= maxPathCount) {
                continue;
            }
            int pathBase = pathIndex * PATH_STRIDE_BYTES;
            int strokeOffset = paths.getInt(pathBase + 8);
            int strokeCount = paths.getInt(pathBase + 12);
            if (strokeCount <= 0 || !isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                continue;
            }
            ByteBuffer strip = buildRange(verts, strokeOffset, strokeCount);
            uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.TriangleStrip, strip, uniforms, uniformBaseOffset, uniformOffset,
                    fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                    STENCIL_MODE_DISABLED, ColorComponentFlags.ALL);
        }
    }

    private static void drawTrianglesRange(IDevice device,
                                           ICommandBuffer commandBuffer,
                                           FlushBatchState batchState,
                                           ByteBuffer verts,
                                           ByteBuffer uniforms,
                                           int uniformBaseOffset,
                                           int fragSize,
                                           int image,
                                           int triangleOffset,
                                           int triangleCount,
                                           int maxVertCount,
                                           int uniformOffset,
                                           int blendSrcRgb,
                                           int blendDstRgb,
                                           int blendSrcAlpha,
                                           int blendDstAlpha) {
        if (triangleCount <= 0 || !isVertexRangeValid(triangleOffset, triangleCount, maxVertCount)) {
            return;
        }
        ByteBuffer tri = buildRange(verts, triangleOffset, triangleCount);
        uploadAndDraw(device, commandBuffer, batchState, PrimitiveType.Triangle, tri, uniforms, uniformBaseOffset, uniformOffset,
                fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                STENCIL_MODE_DISABLED, ColorComponentFlags.ALL);
    }

    private static void uploadAndDraw(IDevice device,
                                      ICommandBuffer commandBuffer,
                                      FlushBatchState batchState,
                                      PrimitiveType primitive,
                                      ByteBuffer vertices,
                                      ByteBuffer uniforms,
                                      int uniformBaseOffset,
                                      int uniformOffset,
                                      int fragSize,
                                      int image,
                                      int blendSrcRgb,
                                      int blendDstRgb,
                                      int blendSrcAlpha,
                                      int blendDstAlpha,
                                      int stencilMode,
                                      int colorWriteMask) {
        if (vertices == null || vertices.remaining() <= 0) {
            return;
        }

        ByteBuffer vertexData = vertices.duplicate().order(ByteOrder.nativeOrder());
        int vertexBytes = vertexData.remaining();
        if (frameVertexWriteOffset < 0 || frameVertexWriteOffset + vertexBytes > dynamicVertexCapacity) {
            return;
        }
        int firstVertex = frameVertexWriteOffset / VERTEX_STRIDE_BYTES;
        dynamicVertexBuffer.updateData(vertexData, frameVertexWriteOffset);
        frameVertexWriteOffset += vertexBytes;

        if (!isUniformRangeValid(uniformOffset, fragSize, 1, uniforms.limit())) {
            return;
        }
        int translatedUniformOffset = translateFragUniformOffset(uniformOffset);
        if (translatedUniformOffset < 0) {
            return;
        }
        long boundUniformOffsetLong = uniformBaseOffset + (long) translatedUniformOffset;
        if (boundUniformOffsetLong > Integer.MAX_VALUE) {
            return;
        }
        int boundUniformOffset = (int) boundUniformOffsetLong;

        GraphicsPipeline pipeline = getOrCreatePipeline(
                device,
                batchState.renderPass,
                primitive,
                blendSrcRgb,
                blendDstRgb,
                blendSrcAlpha,
                blendDstAlpha,
                stencilMode,
                colorWriteMask
        );

        ITexture texture = TEXTURES.getOrDefault(image, dummyTexture);

        pipeline.descriptorSet()
                .uniformBuffer("frame", 0, frameUniformBuffer)
                .uniformBufferRange("frag", 1, fragUniformBuffer, boundUniformOffset, fragSize)
                .samplerTexture("tex", 2, texture)
                .update();

        if (batchState.markViewportPrepared(pipeline)) {
            pipeline.setViewport(0.0f, 0.0f, viewportWidth, viewportHeight);
        }

        int vertexCount = vertexBytes / VERTEX_STRIDE_BYTES;
        batchState.beginIfNeeded(commandBuffer);
        commandBuffer.bindPipeline(pipeline);
        commandBuffer.draw(dynamicVertexBuffer, vertexCount, firstVertex);
    }

    private static void ensureVertexCapacity(IDevice device, int requiredBytes) {
        if (dynamicVertexBuffer != null && dynamicVertexCapacity >= requiredBytes) {
            return;
        }
        if (dynamicVertexBuffer != null) {
            dynamicVertexBuffer.destroy();
        }
        dynamicVertexCapacity = Math.max(requiredBytes, 8192);
        dynamicVertexBuffer = device.createVertexBuffer(
                VertexBufferDescription.create(dynamicVertexCapacity, true, getVertexFormat())
        );
    }

    private static void ensureFragUniformCapacity(IDevice device, int requiredBytes) {
        if (requiredBytes <= 0) {
            return;
        }
        if (fragUniformBuffer != null && fragUniformBuffer.getSize() >= requiredBytes) {
            return;
        }
        if (fragUniformBuffer != null) {
            fragUniformBuffer.destroy();
        }
        fragUniformBuffer = device.createBuffer(
                BufferDescription.create()
                        .size(Math.max(requiredBytes, 1024))
                        .usages(BufferUsages.create().ubo().transferDst())
                        .build()
        );
    }

    private static void ensureActiveBatch(IDevice device,
                                          float viewWidth,
                                          float viewHeight,
                                          int requiredVertexBytes,
                                          int requiredUniformBytes) {
        if (activeCommandBuffer != null && !isActiveBatchCompatible(device, viewWidth, viewHeight)) {
            closeActiveBatch(true);
        }

        int totalVertexBytes = frameVertexWriteOffset + requiredVertexBytes;
        int totalUniformBytes = fragUniformWriteOffset + requiredUniformBytes;
        if (activeCommandBuffer != null
                && (totalVertexBytes > dynamicVertexCapacity || totalUniformBytes > fragUniformBuffer.getSize())) {
            closeActiveBatch(true);
            totalVertexBytes = requiredVertexBytes;
            totalUniformBytes = requiredUniformBytes;
        }

        ensureVertexCapacity(device, totalVertexBytes);
        ensureFragUniformCapacity(device, totalUniformBytes);

        if (activeCommandBuffer != null) {
            return;
        }

        activeCommandBuffer = device.defaultCommandPool().createCommandBuffer();
        activeCommandBuffer.begin();
        activeFlushBatchState = new FlushBatchState(getOrCreateSharedPass(device));
        activeBatchDevice = device;
        activeBatchFramebuffer = targetFramebuffer;
        activeBatchViewWidth = viewWidth;
        activeBatchViewHeight = viewHeight;
        activeBatchFragUniformSourceStride = fragUniformSourceStride;
        activeBatchFragUniformBindingStride = fragUniformBindingStride;
        frameVertexWriteOffset = 0;
        fragUniformWriteOffset = 0;
        stagedFragUniforms = BufferUtils.createByteBuffer(Math.toIntExact(fragUniformBuffer.getSize())).order(ByteOrder.nativeOrder());
        uploadFrameUniform();
    }

    private static boolean isActiveBatchCompatible(IDevice device, float viewWidth, float viewHeight) {
        return activeBatchDevice == device
                && activeBatchFramebuffer == targetFramebuffer
                && Float.compare(activeBatchViewWidth, viewWidth) == 0
                && Float.compare(activeBatchViewHeight, viewHeight) == 0
                && activeBatchFragUniformSourceStride == fragUniformSourceStride
                && activeBatchFragUniformBindingStride == fragUniformBindingStride;
    }

    private static int appendFragUniforms(ByteBuffer uniforms) {
        if (uniforms == null || uniforms.remaining() <= 0) {
            return fragUniformWriteOffset;
        }
        if (stagedFragUniforms == null) {
            return fragUniformWriteOffset;
        }

        int baseOffset = fragUniformWriteOffset;
        ByteBuffer src = uniforms.duplicate().order(ByteOrder.nativeOrder());
        int bytesToAppend = src.remaining();
        stagedFragUniforms.position(baseOffset);
        stagedFragUniforms.put(src);
        fragUniformWriteOffset += bytesToAppend;
        uploadFragUniforms(stagedFragUniforms, fragUniformWriteOffset);
        return baseOffset;
    }

    private static void closeActiveBatch(boolean submit) {
        if (activeCommandBuffer == null) {
            return;
        }

        activeFlushBatchState.endIfNeeded(activeCommandBuffer);
        activeCommandBuffer.end();
        if (submit) {
            activeBatchDevice.submitCommandBuffer(activeCommandBuffer);
        } else {
            activeCommandBuffer.destroy();
        }

        activeCommandBuffer = null;
        activeFlushBatchState = null;
        activeBatchDevice = null;
        activeBatchFramebuffer = null;
        activeBatchViewWidth = 0.0f;
        activeBatchViewHeight = 0.0f;
        activeBatchFragUniformSourceStride = 0;
        activeBatchFragUniformBindingStride = 0;
        frameVertexWriteOffset = 0;
        fragUniformWriteOffset = 0;
        fragUniformSourceStride = 0;
        fragUniformBindingStride = 0;
        stagedFragUniforms = null;
    }

    private static void uploadFrameUniform() {
        if (activeCommandBuffer == null || activeBatchDevice == null) {
            throw new IllegalStateException("No active command buffer for frame uniform upload");
        }
        ByteBuffer frame = BufferUtils.createByteBuffer(16).order(ByteOrder.nativeOrder());
        frame.putFloat(viewportWidth);
        frame.putFloat(viewportHeight);
        frame.putFloat(0.0f);
        frame.putFloat(0.0f);
        frame.flip();
        activeCommandBuffer.writeToBuffer(frameUniformBuffer, 0, frame);
    }

    private static void uploadFragUniforms(ByteBuffer uniforms, int size) {
        if (uniforms == null || size <= 0) {
            return;
        }
        if (activeCommandBuffer == null || activeBatchDevice == null) {
            throw new IllegalStateException("No active command buffer for frag uniform upload");
        }
        ByteBuffer src = uniforms.duplicate().order(ByteOrder.nativeOrder());
        src.position(0);
        src.limit(size);

        ByteBuffer frag = BufferUtils.createByteBuffer(size).order(ByteOrder.nativeOrder());
        frag.put(src);
        frag.flip();
        activeCommandBuffer.writeToBuffer(fragUniformBuffer, 0, frag);
    }

    private static ByteBuffer packFragUniforms(ByteBuffer uniforms, int uniformBytes, int sourceStride, int targetStride) {
        if (uniformBytes <= 0) {
            return BufferUtils.createByteBuffer(1).limit(0);
        }

        ByteBuffer src = uniforms.duplicate().order(ByteOrder.nativeOrder());
        src.position(0);
        src.limit(uniformBytes);

        if (sourceStride <= 0 || targetStride <= 0 || sourceStride == targetStride) {
            ByteBuffer copy = BufferUtils.createByteBuffer(uniformBytes).order(ByteOrder.nativeOrder());
            copy.put(src);
            copy.flip();
            return copy;
        }

        int chunkCount = (uniformBytes + sourceStride - 1) / sourceStride;
        int lastChunkSize = uniformBytes - (chunkCount - 1) * sourceStride;
        int packedBytes = (chunkCount - 1) * targetStride + lastChunkSize;
        ByteBuffer packed = BufferUtils.createByteBuffer(packedBytes).order(ByteOrder.nativeOrder());

        for (int srcOffset = 0; srcOffset < uniformBytes; srcOffset += sourceStride) {
            int copySize = Math.min(sourceStride, uniformBytes - srcOffset);
            int dstOffset = (srcOffset / sourceStride) * targetStride;
            ByteBuffer slice = src.duplicate().order(ByteOrder.nativeOrder());
            slice.position(srcOffset);
            slice.limit(srcOffset + copySize);
            packed.position(dstOffset);
            packed.put(slice);
        }

        packed.position(0);
        packed.limit(packedBytes);
        return packed;
    }

    private static int resolveFragUniformBindingStride(IDevice device, int fragSize) {
        if (fragSize <= 0) {
            return 0;
        }
        int alignment = getUniformBufferOffsetAlignment(device);
        return alignTo(fragSize, alignment);
    }

    private static int getUniformBufferOffsetAlignment(IDevice device) {
        if (device instanceof GlDevice) {
            return Math.max(1, glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));
        }
        if (device instanceof VulkanDevice vkDevice) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.malloc(stack);
                vkGetPhysicalDeviceProperties(vkDevice.getPhysicalDevice(), properties);
                long alignment = properties.limits().minUniformBufferOffsetAlignment();
                return (int) Math.max(1L, alignment);
            }
        }
        return 1;
    }

    private static int alignTo(int value, int alignment) {
        if (alignment <= 1) {
            return value;
        }
        long aligned = ((long) value + alignment - 1L) / alignment * alignment;
        if (aligned > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Aligned uniform stride is too large: " + aligned);
        }
        return (int) aligned;
    }

    private static int translateFragUniformOffset(int uniformOffset) {
        if (uniformOffset < 0) {
            return -1;
        }
        if (fragUniformSourceStride <= 0 || fragUniformBindingStride <= 0 || fragUniformSourceStride == fragUniformBindingStride) {
            return uniformOffset;
        }
        if (uniformOffset % fragUniformSourceStride != 0) {
            return -1;
        }
        long index = uniformOffset / (long) fragUniformSourceStride;
        long translated = index * fragUniformBindingStride;
        return translated > Integer.MAX_VALUE ? -1 : (int) translated;
    }

    private static int resolveCallStride(ByteBuffer callsData, int ncalls) {
        if (ncalls <= 0) {
            return 0;
        }
        int bytes = callsData.remaining();
        if (bytes < ncalls * CALL_STRIDE_BYTES_LEGACY) {
            return 0;
        }
        int inferred = bytes / ncalls;
        if (inferred >= CALL_STRIDE_BYTES) {
            return CALL_STRIDE_BYTES;
        }
        return CALL_STRIDE_BYTES_LEGACY;
    }

    private static int estimateRequiredVertexBytes(ByteBuffer pathsData,
                                                   int maxPathCount,
                                                   ByteBuffer callsData,
                                                   int ncalls,
                                                   int callStride,
                                                   int maxVertCount,
                                                   int uniformLimit,
                                                   int fragSize) {
        long totalBytes = 0L;

        for (int i = 0; i < ncalls; i++) {
            int callBase = i * callStride;
            if (callBase < 0 || callBase + CALL_STRIDE_BYTES_LEGACY > callsData.limit()) {
                continue;
            }

            int type = callsData.getInt(callBase);
            int pathOffset = callsData.getInt(callBase + 8);
            int pathCount = callsData.getInt(callBase + 12);
            int triangleOffset = callsData.getInt(callBase + 16);
            int triangleCount = callsData.getInt(callBase + 20);
            int uniformOffset = callsData.getInt(callBase + 24);

            int uniformCount;
            if (callStride >= CALL_STRIDE_BYTES && callBase + CALL_STRIDE_BYTES <= callsData.limit()) {
                uniformCount = callsData.getInt(callBase + 28);
            } else {
                uniformCount = inferLegacyUniformCount(type);
            }

            if (!isCallRangeValid(pathOffset, pathCount, maxPathCount)) {
                continue;
            }
            if (!isVertexRangeValid(triangleOffset, triangleCount, maxVertCount)) {
                continue;
            }
            if (!isUniformRangeValid(uniformOffset, fragSize, uniformCount, uniformLimit)) {
                continue;
            }

            totalBytes += estimateCallVertexBytes(
                    type,
                    pathsData,
                    pathOffset,
                    pathCount,
                    triangleOffset,
                    triangleCount,
                    uniformCount,
                    maxVertCount,
                    maxPathCount
            );
        }

        if (totalBytes <= 0L) {
            return 0;
        }
        return totalBytes >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalBytes;
    }

    private static long estimateCallVertexBytes(int type,
                                                ByteBuffer pathsData,
                                                int pathOffset,
                                                int pathCount,
                                                int triangleOffset,
                                                int triangleCount,
                                                int uniformCount,
                                                int maxVertCount,
                                                int maxPathCount) {
        long totalBytes = 0L;

        if (type == GLNVG_TRIANGLES) {
            return isVertexRangeValid(triangleOffset, triangleCount, maxVertCount)
                    ? (long) triangleCount * VERTEX_STRIDE_BYTES
                    : 0L;
        }

        if (pathsData == null) {
            return 0L;
        }

        for (int i = 0; i < pathCount; i++) {
            int pathIndex = pathOffset + i;
            if (pathIndex < 0 || pathIndex >= maxPathCount) {
                continue;
            }

            int pathBase = pathIndex * PATH_STRIDE_BYTES;
            int fillOffset = pathsData.getInt(pathBase);
            int fillCount = pathsData.getInt(pathBase + 4);
            int strokeOffset = pathsData.getInt(pathBase + 8);
            int strokeCount = pathsData.getInt(pathBase + 12);

            switch (type) {
                case GLNVG_FILL -> {
                    if (fillCount >= 3 && isVertexRangeValid(fillOffset, fillCount, maxVertCount)) {
                        totalBytes += (long) (fillCount - 2) * 3L * VERTEX_STRIDE_BYTES;
                    }
                    if (uniformCount >= 2) {
                        if (strokeCount > 0 && isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                            totalBytes += (long) strokeCount * VERTEX_STRIDE_BYTES;
                        }
                        if (triangleCount > 0 && isVertexRangeValid(triangleOffset, triangleCount, maxVertCount)) {
                            totalBytes += (long) triangleCount * VERTEX_STRIDE_BYTES;
                        }
                    }
                }
                case GLNVG_CONVEXFILL -> {
                    if (fillCount >= 3 && isVertexRangeValid(fillOffset, fillCount, maxVertCount)) {
                        totalBytes += (long) (fillCount - 2) * 3L * VERTEX_STRIDE_BYTES;
                    }
                    if (strokeCount > 0 && isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                        totalBytes += (long) strokeCount * VERTEX_STRIDE_BYTES;
                    }
                }
                case GLNVG_STROKE -> {
                    if (strokeCount > 0 && isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                        totalBytes += (long) strokeCount * VERTEX_STRIDE_BYTES * (uniformCount >= 2 ? 3L : 1L);
                    }
                }
                default -> {
                }
            }
        }

        return totalBytes;
    }

    private static int inferLegacyUniformCount(int type) {
        return switch (type) {
            case GLNVG_FILL -> 2;
            case GLNVG_CONVEXFILL -> 1;
            case GLNVG_TRIANGLES -> 1;
            case GLNVG_STROKE -> 1;
            default -> 1;
        };
    }

    private static boolean isCallRangeValid(int offset, int count, int totalCount) {
        if (offset < 0 || count < 0) {
            return false;
        }
        return offset + count <= totalCount;
    }

    private static boolean isVertexRangeValid(int first, int count, int totalCount) {
        if (count == 0) {
            return true;
        }
        if (first < 0 || count < 0) {
            return false;
        }
        return first + count <= totalCount;
    }

    private static boolean isUniformRangeValid(int uniformOffset, int fragSize, int uniformCount, int uniformLimit) {
        if (uniformOffset < 0 || fragSize <= 0 || uniformCount <= 0) {
            return false;
        }
        return uniformOffset + fragSize * uniformCount <= uniformLimit;
    }

    private static long makePipelineKey(PrimitiveType primitive,
                                        int blendSrcRgb,
                                        int blendDstRgb,
                                        int blendSrcAlpha,
                                        int blendDstAlpha,
                                        int stencilMode,
                                        int colorWriteMask) {
        return ((long) (primitive.ordinal() & 0xF) << 56)
                | ((long) (stencilMode & 0xF) << 52)
                | ((long) (colorWriteMask & 0xF) << 48)
                | ((long) (blendSrcRgb & 0xFFF) << 36)
                | ((long) (blendDstRgb & 0xFFF) << 24)
                | ((long) (blendSrcAlpha & 0xFFF) << 12)
                | (long) (blendDstAlpha & 0xFFF);
    }

    private static RenderPass getOrCreateSharedPass(IDevice device) {
        return PASS_CACHE.computeIfAbsent(SHARED_RENDER_PASS_KEY, ignored ->
                RenderPass.builder()
                        .frameBuffer(targetFramebuffer)
                        .build(device)
        );
    }

    private static GraphicsPipeline getOrCreatePipeline(IDevice device,
                                                        RenderPass renderPass,
                                                        PrimitiveType primitive,
                                                        int blendSrcRgb,
                                                        int blendDstRgb,
                                                        int blendSrcAlpha,
                                                        int blendDstAlpha,
                                                        int stencilMode,
                                                        int colorWriteMask) {
        long key = makePipelineKey(primitive, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                stencilMode, colorWriteMask);

        return GRAPHICS_PIPELINE_CACHE.computeIfAbsent(key, ignored -> {
            BlendFactor srcColor = mapBlendFactor(blendSrcRgb);
            BlendFactor dstColor = mapBlendFactor(blendDstRgb);
            BlendFactor srcAlpha = mapBlendFactor(blendSrcAlpha);
            BlendFactor dstAlpha = mapBlendFactor(blendDstAlpha);

            ColorBlendAttachment attachment = new ColorBlendAttachment(
                    true,
                    srcColor,
                    dstColor,
                    BlendOp.Add,
                    srcAlpha,
                    dstAlpha,
                    BlendOp.Add,
                    colorWriteMask
            );

            return GraphicsPipeline.builder()
                    .shader(shader)
                    .renderPass(renderPass)
                    .primitiveType(primitive)
                    .rasterization(r -> r.cullMode(CullMode.None))
                    .depthStencil(d -> {
                        d.depthTestEnable(false).depthWriteEnable(false);
                        configureStencilState(d, stencilMode);
                    })
                    .dynamicStates(DynamicStateFlags.Viewport)
                    .colorBlend(cb -> cb.addAttachment(attachment))
                    .vertexFormat(getVertexFormat())
                    .build(device);
        });
    }

    private static void configureStencilState(DepthStencilState.Builder builder, int stencilMode) {
        switch (stencilMode) {
            case STENCIL_MODE_FILL_WRITE -> builder
                    .stencilTestEnable(true)
                    .stencilCompareMask(0xFF)
                    .stencilWriteMask(0xFF)
                    .stencilReference(0)
                    .stencilCompareOpFront(CompareOp.Always)
                    .stencilCompareOpBack(CompareOp.Always)
                    .stencilFailOpFront(StencilOp.Keep)
                    .stencilDepthFailOpFront(StencilOp.Keep)
                    .stencilPassOpFront(StencilOp.IncrementAndWrap)
                    .stencilFailOpBack(StencilOp.Keep)
                    .stencilDepthFailOpBack(StencilOp.Keep)
                    .stencilPassOpBack(StencilOp.DecrementAndWrap);
            case STENCIL_MODE_FILL_AA, STENCIL_MODE_STROKE_AA -> builder
                    .stencilTestEnable(true)
                    .stencilCompareMask(0xFF)
                    .stencilWriteMask(0x00)
                    .stencilReference(0)
                    .stencilCompareOp(CompareOp.Equal)
                    .stencilFailOp(StencilOp.Keep)
                    .stencilDepthFailOp(StencilOp.Keep)
                    .stencilPassOp(StencilOp.Keep);
            case STENCIL_MODE_FILL_CLEAR -> builder
                    .stencilTestEnable(true)
                    .stencilCompareMask(0xFF)
                    .stencilWriteMask(0xFF)
                    .stencilReference(0)
                    .stencilCompareOp(CompareOp.NotEqual)
                    .stencilFailOp(StencilOp.Zero)
                    .stencilDepthFailOp(StencilOp.Zero)
                    .stencilPassOp(StencilOp.Zero);
            case STENCIL_MODE_STROKE_BASE -> builder
                    .stencilTestEnable(true)
                    .stencilCompareMask(0xFF)
                    .stencilWriteMask(0xFF)
                    .stencilReference(0)
                    .stencilCompareOp(CompareOp.Equal)
                    .stencilFailOp(StencilOp.Keep)
                    .stencilDepthFailOp(StencilOp.Keep)
                    .stencilPassOp(StencilOp.IncrementAndClamp);
            case STENCIL_MODE_STROKE_CLEAR -> builder
                    .stencilTestEnable(true)
                    .stencilCompareMask(0xFF)
                    .stencilWriteMask(0xFF)
                    .stencilReference(0)
                    .stencilCompareOp(CompareOp.Always)
                    .stencilFailOp(StencilOp.Zero)
                    .stencilDepthFailOp(StencilOp.Zero)
                    .stencilPassOp(StencilOp.Zero);
            default -> builder.stencilTestEnable(false);
        }
    }

    private static BlendFactor mapBlendFactor(int glFactor) {
        return switch (glFactor) {
            case NVG_BF_ZERO -> BlendFactor.Zero;
            case NVG_BF_ONE -> BlendFactor.One;
            case NVG_BF_SRC_COLOR -> BlendFactor.SrcColor;
            case NVG_BF_ONE_MINUS_SRC_COLOR -> BlendFactor.OneMinusSrcColor;
            case NVG_BF_DST_COLOR -> BlendFactor.DstColor;
            case NVG_BF_ONE_MINUS_DST_COLOR -> BlendFactor.OneMinusDstColor;
            case NVG_BF_SRC_ALPHA -> BlendFactor.SrcAlpha;
            case NVG_BF_ONE_MINUS_SRC_ALPHA -> BlendFactor.OneMinusSrcAlpha;
            case NVG_BF_DST_ALPHA -> BlendFactor.DstAlpha;
            case NVG_BF_ONE_MINUS_DST_ALPHA -> BlendFactor.OneMinusDstAlpha;
            case NVG_BF_SRC_ALPHA_SATURATE -> BlendFactor.SrcAlphaSaturate;
            default -> BlendFactor.One;
        };
    }

    private static ByteBuffer buildRange(ByteBuffer verts, int firstVertex, int vertexCount) {
        ByteBuffer out = BufferUtils.createByteBuffer(vertexCount * VERTEX_STRIDE_BYTES).order(ByteOrder.nativeOrder());
        for (int i = 0; i < vertexCount; i++) {
            putVertex(out, verts, firstVertex + i);
        }
        out.flip();
        return out;
    }

    private static ByteBuffer buildTriangleFan(ByteBuffer verts, int firstVertex, int vertexCount) {
        int triangleCount = vertexCount - 2;
        ByteBuffer out = BufferUtils.createByteBuffer(triangleCount * 3 * VERTEX_STRIDE_BYTES).order(ByteOrder.nativeOrder());
        for (int i = 0; i < triangleCount; i++) {
            putVertex(out, verts, firstVertex);
            putVertex(out, verts, firstVertex + i + 1);
            putVertex(out, verts, firstVertex + i + 2);
        }
        out.flip();
        return out;
    }

    private static void putVertex(ByteBuffer dst, ByteBuffer src, int vertexIndex) {
        int base = vertexIndex * VERTEX_STRIDE_BYTES;
        dst.putFloat(src.getFloat(base));
        dst.putFloat(src.getFloat(base + 4));
        dst.putFloat(src.getFloat(base + 8));
        dst.putFloat(src.getFloat(base + 12));
    }

    private static VertexFormat getVertexFormat() {
        return VertexFormat.builder()
                .addAttribute(0, "vertex", VertexAttributeFormat.FLOAT2)
                .addAttribute(1, "tcoord", VertexAttributeFormat.FLOAT2)
                .build();
    }

    private static final class FlushBatchState {
        private final RenderPass renderPass;
        private final Map<GraphicsPipeline, Boolean> viewportPrepared = new IdentityHashMap<>();
        private boolean renderPassActive;

        private FlushBatchState(RenderPass renderPass) {
            this.renderPass = renderPass;
        }

        private void beginIfNeeded(ICommandBuffer commandBuffer) {
            if (renderPassActive) {
                return;
            }
            commandBuffer.beginRenderPass(renderPass);
            renderPassActive = true;
        }

        private void endIfNeeded(ICommandBuffer commandBuffer) {
            if (!renderPassActive) {
                return;
            }
            commandBuffer.endRenderPass();
            renderPassActive = false;
        }

        private boolean markViewportPrepared(GraphicsPipeline pipeline) {
            return viewportPrepared.put(pipeline, Boolean.TRUE) == null;
        }
    }

    private static final class ExternalTextureRef implements ITexture {
        private final long handle;
        private final int width;
        private final int height;
        private final TextureFormat format;
        private final TextureFilterMode filterMode;
        private final TextureWrapMode wrapMode;

        private ExternalTextureRef(long handle,
                                   int width,
                                   int height,
                                   TextureFormat format,
                                   TextureFilterMode filterMode,
                                   TextureWrapMode wrapMode) {
            this.handle = handle;
            this.width = width;
            this.height = height;
            this.format = format;
            this.filterMode = filterMode;
            this.wrapMode = wrapMode;
        }

        @Override
        public TextureFormat getTextureFormat() {
            return format;
        }

        @Override
        public TextureUsages getTextureUsages() {
            return TextureUsages.create().sampler();
        }

        @Override
        public TextureType getTextureType() {
            return TextureType.Texture2D;
        }

        @Override
        public TextureFilterMode getTextureFilterMode() {
            return filterMode;
        }

        @Override
        public TextureWrapMode getTextureWrapMode() {
            return wrapMode;
        }

        @Override
        public TextureMipmapSettings getMipmapSettings() {
            return TextureMipmapSettings.disabled();
        }

        @Override
        public TextureDescription getTextureDescription() {
            return TextureDescription.create()
                    .type(TextureType.Texture2D)
                    .format(format)
                    .size(width, height)
                    .usages(TextureUsages.create().sampler())
                    .filterMode(filterMode)
                    .wrapMode(wrapMode)
                    .mipmapSettings(TextureMipmapSettings.disabled())
                    .label("NanoVGExternalTexture-" + handle)
                    .build();
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void destroy() {
        }

        @Override
        public long handle() {
            return handle;
        }
    }
}
