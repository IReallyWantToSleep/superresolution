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

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsages;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferBehavior;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.CommandBufferState;
import io.homo.superresolution.core.graphics.impl.command.MemoryBarrierType;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.state.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.impl.vertex.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final int GLNVG_LIQUID_GLASS = 5;

    private static final int STENCIL_MODE_DISABLED = 0;
    private static final int STENCIL_MODE_FILL_WRITE = 1;
    private static final int STENCIL_MODE_FILL_AA = 2;
    private static final int STENCIL_MODE_FILL_CLEAR = 3;
    private static final int STENCIL_MODE_STROKE_BASE = 4;
    private static final int STENCIL_MODE_STROKE_AA = 5;
    private static final int STENCIL_MODE_STROKE_CLEAR = 6;

    private static final int VERTEX_STRIDE_BYTES = 16;
    private static final int PATH_STRIDE_BYTES = 16;
    private static final int CALL_STRIDE_BYTES = 52;
    private static final long LOAD_RENDER_PASS_KEY = 0L;
    private static final long DEPTH_STENCIL_CLEAR_PASS_KEY = 1L;
    private static final int LIQUID_GLASS_UNIFORM_SIZE = 160;
    private static final int LIQUID_GLASS_KERNEL_UNIFORM_SIZE = 64;
    private static final int LIQUID_GLASS_FROST_LOCAL_SIZE = 16;
    private static final IShaderProgram[] LIQUID_GLASS_FROST_SHADERS = new IShaderProgram[3];

    private static final Map<Integer, ITexture> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<Integer, ITexture> PENDING_EXTERNAL_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> OWNED_TEXTURES = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_EXTERNAL_TEXTURE_HANDLE = new AtomicInteger(1_000_000);
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
    private static final List<ITexture> RETIRED_TEXTURES = new ArrayList<>();
    private static IFrameBuffer targetFramebuffer;
    private static int targetWidth;
    private static int targetHeight;
    private static TextureFormat targetColorFormat;
    private static ITexture dummyTexture;
    private static ITexture liquidGlassBackdrop;
    private static LiquidGlassFrostResources liquidGlassFrostResources;
    private static IShaderProgram shader;
    private static IShaderProgram liquidGlassShader;
    private static GraphicsPipeline liquidGlassPipeline;
    private static IBuffer frameUniformBuffer;
    private static IBuffer fragUniformBuffer;
    private static IVertexBuffer dynamicVertexBuffer;
    private static IDevice device;
    private static Runnable waitForIdle;
    private static int uniformBufferAlignment;
    private static int dynamicVertexCapacity;
    private static int fragUniformSourceStride;
    private static int fragUniformBindingStride;
    private static ByteBuffer stagedFragUniforms;
    private static ICommandBuffer recordingCommandBuffer;
    private static boolean clearDepthStencilOnFlush;
    private static boolean frameFlushed;
    private static float viewportWidth;
    private static float viewportHeight;

    private NanoVGRhiBridge() {
    }

    public static void setTargetFramebuffer(IFrameBuffer framebuffer) {
        int width = framebuffer == null ? 0 : framebuffer.getWidth();
        int height = framebuffer == null ? 0 : framebuffer.getHeight();
        ITexture color = framebuffer == null ? null : framebuffer.getTexture(FrameBufferAttachmentType.Color);
        TextureFormat format = color == null ? null : color.getTextureFormat();
        if (targetFramebuffer != framebuffer || targetWidth != width || targetHeight != height || targetColorFormat != format) {
            if (recordingCommandBuffer != null) {
                throw new IllegalStateException("Cannot replace the NanoVG target during recording");
            }
            if (targetFramebuffer != null) {
                waitForIdle.run();
            }
            destroyLiquidGlassResources();
            GRAPHICS_PIPELINE_CACHE.values().forEach(GraphicsPipeline::destroy);
            GRAPHICS_PIPELINE_CACHE.clear();
            PASS_CACHE.values().forEach(RenderPass::destroy);
            PASS_CACHE.clear();
            targetFramebuffer = framebuffer;
            targetWidth = width;
            targetHeight = height;
            targetColorFormat = format;
        }
    }

    public static void setDevice(IDevice device, Runnable waitForIdle) {
        NanoVGRhiBridge.device = Objects.requireNonNull(device);
        NanoVGRhiBridge.waitForIdle = Objects.requireNonNull(waitForIdle);
        uniformBufferAlignment = Math.max(1, device.uniformBufferOffsetAlignment());
    }

    private static void destroyLiquidGlassResources() {
        if (liquidGlassPipeline != null) {
            liquidGlassPipeline.destroy();
            liquidGlassPipeline = null;
        }
        if (liquidGlassFrostResources != null) {
            liquidGlassFrostResources.destroy();
            liquidGlassFrostResources = null;
        }
        if (liquidGlassBackdrop != null) {
            liquidGlassBackdrop.destroy();
            liquidGlassBackdrop = null;
        }
    }

    public static void beginFrame(IFrameBuffer framebuffer, ICommandBuffer commandBuffer, boolean clearDepthStencil) {
        if (recordingCommandBuffer != null) {
            throw new IllegalStateException("A NanoVG frame is already recording");
        }
        if (commandBuffer == null || commandBuffer.getDevice() != device
                || commandBuffer.state() != CommandBufferState.Recording) {
            throw new IllegalArgumentException("NanoVG requires a recording command buffer from its device");
        }
        setTargetFramebuffer(Objects.requireNonNull(framebuffer));
        destroyRetiredTextures();
        recordingCommandBuffer = commandBuffer;
        clearDepthStencilOnFlush = clearDepthStencil;
        frameFlushed = false;
    }

    public static void endFrame() {
        if (recordingCommandBuffer == null) {
            throw new IllegalStateException("No NanoVG frame is recording");
        }
        try {
            if (!frameFlushed) {
                recordingCommandBuffer.memoryBarrier(MemoryBarrierType.ALL);
                clearDepthStencilIfNeeded(recordingCommandBuffer);
            }
        } finally {
            cancelFrame();
        }
    }

    public static void cancelFrame() {
        recordingCommandBuffer = null;
        clearDepthStencilOnFlush = false;
        frameFlushed = false;
    }

    private static void clearDepthStencilIfNeeded(ICommandBuffer commandBuffer) {
        if (clearDepthStencilOnFlush) {
            commandBuffer.beginRenderPass(getOrCreateDepthStencilClearPass(device));
            commandBuffer.endRenderPass();
            commandBuffer.memoryBarrier(MemoryBarrierType.ALL);
            clearDepthStencilOnFlush = false;
        }
    }

    private static void destroyRetiredTextures() {
        if (!RETIRED_TEXTURES.isEmpty()) {
            waitForIdle.run();
            RETIRED_TEXTURES.forEach(ITexture::destroy);
            RETIRED_TEXTURES.clear();
        }
    }

    public static boolean nCreateTexture(int imageId, int type, int width, int height, int imageFlags, ByteBuffer data, int dataSize) {
        if (device == null) {
            throw new IllegalStateException("NanoVG RHI device is not initialized");
        }
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
            try {
                uploadTexture(texture, upload, 0, 0, width, height);
            } finally {
                MemoryUtil.memFree(upload);
            }
        }

        replaceTexture(imageId, texture, true);
        return true;
    }

    public static boolean nRegisterExternalTexture(int imageId, int externalTextureHandle, int width, int height, int imageFlags) {
        ITexture pendingTexture = PENDING_EXTERNAL_TEXTURES.remove(externalTextureHandle);
        if (pendingTexture != null) {
            replaceTexture(imageId, pendingTexture, false);
            return true;
        }

        TextureFormat format = TextureFormat.RGBA8;
        TextureFilterMode filterMode = (imageFlags & NVG_IMAGE_NEAREST) != 0 ? TextureFilterMode.Nearest : TextureFilterMode.Linear;
        TextureWrapMode wrapMode = ((imageFlags & NVG_IMAGE_REPEATX) != 0 || (imageFlags & NVG_IMAGE_REPEATY) != 0)
                ? TextureWrapMode.Repeat
                : TextureWrapMode.ClampToEdge;

        ITexture texture = new ExternalTextureRef(externalTextureHandle, width, height, format, filterMode, wrapMode);
        replaceTexture(imageId, texture, true);
        return true;
    }

    public static int prepareExternalTexture(ITexture texture) {
        if (texture == null) {
            throw new IllegalArgumentException("External NanoVG texture is null");
        }
        int handle = NEXT_EXTERNAL_TEXTURE_HANDLE.getAndIncrement();
        PENDING_EXTERNAL_TEXTURES.put(handle, texture);
        return handle;
    }

    public static void cancelPreparedExternalTexture(int handle) {
        PENDING_EXTERNAL_TEXTURES.remove(handle);
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
        try {
            uploadTexture(texture, upload, x, y, width, height);
        } finally {
            MemoryUtil.memFree(upload);
        }
        return true;
    }

    private static void uploadTexture(ITexture texture, ByteBuffer data, int x, int y, int width, int height) {
        if (recordingCommandBuffer != null) {
            recordingCommandBuffer.memoryBarrier(MemoryBarrierType.ALL);
            recordingCommandBuffer.writeToTexture(texture, data, x, y, width, height);
            return;
        }
        ICommandBuffer commandBuffer = device.defaultCommandPool().createCommandBuffer(CommandBufferBehavior.OneTimeSubmit);
        try {
            commandBuffer.begin();
            commandBuffer.memoryBarrier(MemoryBarrierType.ALL);
            commandBuffer.writeToTexture(texture, data, x, y, width, height);
            commandBuffer.end();
            commandBuffer.submit(device);
        } finally {
            commandBuffer.destroy();
        }
    }

    public static void nDeleteTexture(int imageId) {
        ITexture texture = TEXTURES.remove(imageId);
        boolean owned = Boolean.TRUE.equals(OWNED_TEXTURES.remove(imageId));
        if (texture != null && owned) {
            RETIRED_TEXTURES.add(texture);
        }
    }

    private static void replaceTexture(int imageId, ITexture texture, boolean owned) {
        ITexture previous = TEXTURES.put(imageId, texture);
        boolean previousOwned = Boolean.TRUE.equals(OWNED_TEXTURES.remove(imageId));
        OWNED_TEXTURES.put(imageId, owned);
        if (previous != null && previous != texture && previousOwned) {
            RETIRED_TEXTURES.add(previous);
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
                              int fragSize,
                              int callStride) {
        if (ncalls <= 0 || nverts <= 0 || verts == null || calls == null || uniforms == null || fragSize <= 0 || callStride != CALL_STRIDE_BYTES) {
            return;
        }

        if (targetFramebuffer == null) {
            return;
        }
        if (recordingCommandBuffer != null && frameFlushed) {
            throw new IllegalStateException("NanoVG must flush once per frame");
        }

        ensureRendererResources(device);

        viewportWidth = viewWidth;
        viewportHeight = viewHeight;

        ByteBuffer vertsData = verts.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer pathsData = paths == null ? null : paths.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer callsData = calls.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer uniformsData = uniforms.duplicate().order(ByteOrder.nativeOrder());

        int requiredCallBytes;
        try {
            requiredCallBytes = Math.multiplyExact(ncalls, callStride);
        } catch (ArithmeticException ignored) {
            return;
        }
        if (callsData.remaining() < requiredCallBytes) {
            return;
        }

        int maxVertCount = Math.min(nverts, vertsData.remaining() / VERTEX_STRIDE_BYTES);
        int maxPathCount = pathsData == null ? 0 : Math.min(npaths, pathsData.remaining() / PATH_STRIDE_BYTES);
        int maxUniformBytes = Math.min(uniformBytes, uniformsData.remaining());
        uniformsData.limit(uniformsData.position() + maxUniformBytes);
        if (maxVertCount <= 0) {
            return;
        }

        fragUniformSourceStride = fragSize;
        fragUniformBindingStride = alignTo(fragSize, uniformBufferAlignment);
        if (fragUniformBindingStride <= 0) {
            return;
        }

        int requiredVertexBytes = maxVertCount * VERTEX_STRIDE_BYTES;
        if (requiredVertexBytes <= 0) {
            return;
        }

        List<DrawCommand> drawCommands = new ArrayList<>(ncalls * 2);
        for (int i = 0; i < ncalls; i++) {
            int callBase = i * callStride;
            int type = callsData.getInt(callBase);
            int image = callsData.getInt(callBase + 4);
            int pathOffset = callsData.getInt(callBase + 8);
            int pathCount = callsData.getInt(callBase + 12);
            int triangleOffset = callsData.getInt(callBase + 16);
            int triangleCount = callsData.getInt(callBase + 20);
            int uniformOffset = callsData.getInt(callBase + 24);
            int uniformCount = callsData.getInt(callBase + 28);
            int blendSrcRgb = callsData.getInt(callBase + 32);
            int blendDstRgb = callsData.getInt(callBase + 36);
            int blendSrcAlpha = callsData.getInt(callBase + 40);
            int blendDstAlpha = callsData.getInt(callBase + 44);
            int fontImage = callsData.getInt(callBase + 48);

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
                        drawCommands,
                        pathsData,
                        uniformsData,
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
                        drawCommands,
                        pathsData,
                        uniformsData,
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
                        drawCommands,
                        pathsData,
                        uniformsData,
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
                        drawCommands,
                        uniformsData,
                        fragSize,
                        image,
                        fontImage,
                        triangleOffset,
                        triangleCount,
                        maxVertCount,
                        uniformOffset,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                case GLNVG_LIQUID_GLASS -> drawLiquidGlassRange(
                        drawCommands,
                        vertsData,
                        uniformsData,
                        fragSize,
                        triangleOffset,
                        triangleCount,
                        maxVertCount,
                        uniformOffset,
                        uniformCount);
                default -> {
                }
            }
        }
        if (drawCommands.isEmpty()) {
            return;
        }

        prepareLiquidGlassResources(device, drawCommands);
        ByteBuffer packedUniforms = packFragUniforms(uniformsData, maxUniformBytes, fragUniformSourceStride, fragUniformBindingStride);
        packedUniforms = appendLiquidGlassKernels(packedUniforms, drawCommands);
        ensureBufferCapacity(requiredVertexBytes, packedUniforms.remaining());
        if (recordingCommandBuffer == null) {
            destroyRetiredTextures();
        }

        boolean ownedCommandBuffer = recordingCommandBuffer == null;
        ICommandBuffer commandBuffer = ownedCommandBuffer
                ? device.defaultCommandPool().createCommandBuffer(CommandBufferBehavior.OneTimeSubmit)
                : recordingCommandBuffer;
        RenderState renderState = new RenderState(getOrCreateLoadPass(device));
        try {
            if (ownedCommandBuffer) {
                commandBuffer.begin();
            } else {
                frameFlushed = true;
            }
            commandBuffer.memoryBarrier(MemoryBarrierType.ALL);
            clearDepthStencilIfNeeded(commandBuffer);
            vertsData.position(0);
            vertsData.limit(requiredVertexBytes);
            commandBuffer.writeToBuffer(dynamicVertexBuffer, 0, vertsData);
            commandBuffer.writeToBuffer(fragUniformBuffer, 0, packedUniforms);
            uploadFrameUniform(commandBuffer);
            commandBuffer.memoryBarrier(MemoryBarrierType.ALL);
            for (DrawCommand drawCommand : drawCommands) {
                executeDrawCommand(device, commandBuffer, renderState, drawCommand);
            }
            commandBuffer.memoryBarrier(MemoryBarrierType.ALL);
            renderState.endIfNeeded(commandBuffer);
            if (ownedCommandBuffer) {
                commandBuffer.end();
                commandBuffer.submit(device);
            }
        } finally {
            try {
                renderState.endIfNeeded(commandBuffer);
            } finally {
                if (ownedCommandBuffer) {
                    commandBuffer.destroy();
                }
            }
        }
    }

    public static void nDestroy() {
        if (recordingCommandBuffer != null) {
            throw new IllegalStateException("Cannot destroy NanoVG during recording");
        }
        if (waitForIdle != null) {
            waitForIdle.run();
        }
        RETIRED_TEXTURES.forEach(ITexture::destroy);
        RETIRED_TEXTURES.clear();
        destroyLiquidGlassResources();

        for (Map.Entry<Integer, ITexture> entry : TEXTURES.entrySet()) {
            if (Boolean.TRUE.equals(OWNED_TEXTURES.get(entry.getKey()))) {
                entry.getValue().destroy();
            }
        }
        TEXTURES.clear();
        OWNED_TEXTURES.clear();
        PENDING_EXTERNAL_TEXTURES.clear();

        GRAPHICS_PIPELINE_CACHE.values().forEach(GraphicsPipeline::destroy);
        GRAPHICS_PIPELINE_CACHE.clear();
        PASS_CACHE.values().forEach(RenderPass::destroy);
        PASS_CACHE.clear();

        if (dynamicVertexBuffer != null) {
            dynamicVertexBuffer.destroy();
            dynamicVertexBuffer = null;
            dynamicVertexCapacity = 0;
        }
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
        if (liquidGlassShader != null) {
            liquidGlassShader.destroy();
            liquidGlassShader = null;
        }
        for (int i = 0; i < LIQUID_GLASS_FROST_SHADERS.length; i++) {
            if (LIQUID_GLASS_FROST_SHADERS[i] != null) {
                LIQUID_GLASS_FROST_SHADERS[i].destroy();
                LIQUID_GLASS_FROST_SHADERS[i] = null;
            }
        }
        targetFramebuffer = null;
        targetWidth = 0;
        targetHeight = 0;
        targetColorFormat = null;
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
                            .uniformSamplerTexture("fontTex", 3)
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

        ensureDummyTexture(device);
    }

    private static void ensureDummyTexture(IDevice device) {
        if (dummyTexture != null) {
            return;
        }
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
        try {
            uploadTexture(dummyTexture, white, 0, 0, 1, 1);
        } finally {
            MemoryUtil.memFree(white);
        }
    }

    private static void drawFill(List<DrawCommand> drawCommands,
                                 ByteBuffer paths,
                                 ByteBuffer uniforms,
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
                emitDrawCommand(drawCommands, PrimitiveType.TriangleFan, fillOffset, fillCount, uniforms, uniformOffset,
                        fragSize, 0, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
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
                    emitDrawCommand(drawCommands, PrimitiveType.TriangleStrip, strokeOffset, strokeCount, uniforms, paintUniformOffset,
                            fragSize, image, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                            STENCIL_MODE_FILL_AA, ColorComponentFlags.ALL);
                }
            }

            // Pass 3: draw bounding quad where stencil is not zero and clear stencil back to zero.
            if (triangleCount > 0 && isVertexRangeValid(triangleOffset, triangleCount, maxVertCount)) {
                emitDrawCommand(drawCommands, PrimitiveType.TriangleStrip, triangleOffset, triangleCount, uniforms, paintUniformOffset,
                        fragSize, image, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_FILL_CLEAR, ColorComponentFlags.ALL);
            }
        }
    }

    private static void drawConvexFill(List<DrawCommand> drawCommands,
                                       ByteBuffer paths,
                                       ByteBuffer uniforms,
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
                emitDrawCommand(drawCommands, PrimitiveType.TriangleFan, fillOffset, fillCount, uniforms, uniformOffset,
                        fragSize, image, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_DISABLED, ColorComponentFlags.ALL);
            }
            if (strokeCount > 0 && isVertexRangeValid(strokeOffset, strokeCount, maxVertCount)) {
                emitDrawCommand(drawCommands, PrimitiveType.TriangleStrip, strokeOffset, strokeCount, uniforms, uniformOffset,
                        fragSize, image, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                        STENCIL_MODE_DISABLED, ColorComponentFlags.ALL);
            }
        }
    }

    private static void drawStroke(List<DrawCommand> drawCommands,
                                   ByteBuffer paths,
                                   ByteBuffer uniforms,
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
                emitDrawCommand(drawCommands, PrimitiveType.TriangleStrip, strokeOffset, strokeCount, uniforms, baseUniformOffset,
                        fragSize, image, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
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
                emitDrawCommand(drawCommands, PrimitiveType.TriangleStrip, strokeOffset, strokeCount, uniforms, uniformOffset,
                        fragSize, image, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
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
                emitDrawCommand(drawCommands, PrimitiveType.TriangleStrip, strokeOffset, strokeCount, uniforms, uniformOffset,
                        fragSize, image, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
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
            emitDrawCommand(drawCommands, PrimitiveType.TriangleStrip, strokeOffset, strokeCount, uniforms, uniformOffset,
                    fragSize, image, 0, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                    STENCIL_MODE_DISABLED, ColorComponentFlags.ALL);
        }
    }

    private static void drawTrianglesRange(List<DrawCommand> drawCommands,
                                           ByteBuffer uniforms,
                                           int fragSize,
                                           int image,
                                           int fontImage,
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
        emitDrawCommand(drawCommands, PrimitiveType.Triangle, triangleOffset, triangleCount, uniforms, uniformOffset,
                fragSize, image, fontImage, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                STENCIL_MODE_DISABLED, ColorComponentFlags.ALL);
    }

    private static void drawLiquidGlassRange(List<DrawCommand> drawCommands,
                                            ByteBuffer vertices,
                                            ByteBuffer uniforms,
                                            int fragSize,
                                            int triangleOffset,
                                            int triangleCount,
                                            int maxVertCount,
                                            int uniformOffset,
                                            int uniformCount) {
        if (fragSize < LIQUID_GLASS_UNIFORM_SIZE || triangleCount != 6
                || !isVertexRangeValid(triangleOffset, triangleCount, maxVertCount)
                || uniformCount != 1 || !isUniformRangeValid(uniformOffset, fragSize, 1, uniforms.limit())) {
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < triangleCount; i++) {
            int vertexBase = (triangleOffset + i) * VERTEX_STRIDE_BYTES;
            float x = vertices.getFloat(vertexBase);
            float y = vertices.getFloat(vertexBase + Float.BYTES);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        int targetWidth = Math.max(1, targetFramebuffer.getWidth());
        int targetHeight = Math.max(1, targetFramebuffer.getHeight());
        double thickness = uniforms.getFloat(uniformOffset + 80);
        double displacement = uniforms.getFloat(uniformOffset + 84);
        double spread = uniforms.getFloat(uniformOffset + 88);
        double frostSigma = uniforms.getFloat(uniformOffset + 92);
        int frostScale = frostSigma < 0.3 ? 1 : Math.max(2, Math.min(24, (int) Math.ceil(frostSigma / 4.0)));
        double bezel = uniforms.getFloat(uniformOffset + 108);
        double ior = uniforms.getFloat(uniformOffset + 64);
        // Cover refraction, RGB separation, reduction footprint and Gaussian support.
        double padding = Math.ceil(2.0 * (thickness + bezel * 0.5) * displacement
                * Math.sqrt(Math.max(ior * ior - 1.0, 0.0)) * (1.0 + spread)
                + 3.0 * frostSigma + 2.0 * frostScale + 4.0);
        int copyX0 = (int) Math.max(0.0, Math.floor(minX - padding));
        int copyY0 = (int) Math.max(0.0, Math.floor(minY - padding));
        int copyX1 = (int) Math.min(targetWidth, Math.ceil(maxX + padding));
        int copyY1 = (int) Math.min(targetHeight, Math.ceil(maxY + padding));
        if (copyX1 <= copyX0 || copyY1 <= copyY0) {
            return;
        }

        uniforms.putFloat(uniformOffset, copyX0);
        uniforms.putFloat(uniformOffset + 4, copyY0);
        uniforms.putFloat(uniformOffset + 8, copyX1 - copyX0);
        uniforms.putFloat(uniformOffset + 12, copyY1 - copyY0);
        uniforms.putFloat(uniformOffset + 16, targetWidth);
        uniforms.putFloat(uniformOffset + 20, targetHeight);
        uniforms.putFloat(uniformOffset + 72, frostScale);
        uniforms.putFloat(uniformOffset + 76, 1.0f);

        int translatedUniformOffset = translateFragUniformOffset(uniformOffset);
        if (translatedUniformOffset < 0) {
            return;
        }
        drawCommands.add(new DrawCommand(
                PrimitiveType.Triangle,
                triangleOffset,
                triangleCount,
                translatedUniformOffset,
                fragSize,
                0,
                0,
                NVG_BF_ONE,
                NVG_BF_ONE_MINUS_SRC_ALPHA,
                NVG_BF_ONE,
                NVG_BF_ONE_MINUS_SRC_ALPHA,
                STENCIL_MODE_DISABLED,
                ColorComponentFlags.ALL,
                true,
                copyX0,
                copyY0,
                copyX1,
                copyY1,
                frostScale,
                frostSigma / frostScale >= 0.3
        ));
    }

    private static void emitDrawCommand(List<DrawCommand> drawCommands,
                                        PrimitiveType primitive,
                                        int firstVertex,
                                        int vertexCount,
                                        ByteBuffer uniforms,
                                        int uniformOffset,
                                        int fragSize,
                                        int image,
                                        int fontImage,
                                        int blendSrcRgb,
                                        int blendDstRgb,
                                        int blendSrcAlpha,
                                        int blendDstAlpha,
                                        int stencilMode,
                                        int colorWriteMask) {
        if (vertexCount <= 0) {
            return;
        }

        if (!isUniformRangeValid(uniformOffset, fragSize, 1, uniforms.limit())) {
            return;
        }
        int translatedUniformOffset = translateFragUniformOffset(uniformOffset);
        if (translatedUniformOffset < 0) {
            return;
        }

        drawCommands.add(new DrawCommand(
                primitive,
                firstVertex,
                vertexCount,
                translatedUniformOffset,
                fragSize,
                image,
                fontImage,
                blendSrcRgb,
                blendDstRgb,
                blendSrcAlpha,
                blendDstAlpha,
                stencilMode,
                colorWriteMask,
                false,
                0,
                0,
                0,
                0,
                1,
                false
        ));
    }

    private static void executeDrawCommand(IDevice device,
                                           ICommandBuffer commandBuffer,
                                           RenderState renderState,
                                           DrawCommand drawCommand) {
        if (drawCommand.liquidGlass) {
            executeLiquidGlassCommand(device, commandBuffer, renderState, drawCommand);
            return;
        }

        GraphicsPipeline pipeline = getOrCreatePipeline(
                device,
                renderState.renderPass,
                drawCommand.primitive,
                drawCommand.blendSrcRgb,
                drawCommand.blendDstRgb,
                drawCommand.blendSrcAlpha,
                drawCommand.blendDstAlpha,
                drawCommand.stencilMode,
                drawCommand.colorWriteMask
        );

        ITexture texture = TEXTURES.getOrDefault(drawCommand.image, dummyTexture);
        ITexture fontTexture = TEXTURES.getOrDefault(drawCommand.fontImage, dummyTexture);

        pipeline.descriptorSet()
                .uniformBuffer("frame", 0, frameUniformBuffer)
                .uniformBufferRange("frag", 1, fragUniformBuffer, drawCommand.uniformOffset, drawCommand.uniformRange)
                .samplerTexture("tex", 2, texture)
                .samplerTexture("fontTex", 3, fontTexture)
                .update();

        renderState.beginIfNeeded(commandBuffer);
        pipeline.setViewport(0.0f, 0.0f, viewportWidth, viewportHeight);
        pipeline.setScissor(0, 0, Math.max(1, (int) viewportWidth), Math.max(1, (int) viewportHeight));
        commandBuffer.bindPipeline(pipeline);
        commandBuffer.draw(dynamicVertexBuffer, drawCommand.vertexCount, drawCommand.firstVertex);
    }

    private static void executeLiquidGlassCommand(IDevice device,
                                                  ICommandBuffer commandBuffer,
                                                  RenderState renderState,
                                                  DrawCommand drawCommand) {
        renderState.endIfNeeded(commandBuffer);

        ITexture colorTarget = targetFramebuffer.getTexture(FrameBufferAttachmentType.Color);
        ITexture backdrop = liquidGlassBackdrop;
        int sourceY0 = targetFramebuffer.getHeight() - drawCommand.sampleY1;
        int sourceY1 = targetFramebuffer.getHeight() - drawCommand.sampleY0;
        commandBuffer.copyTexture(
                colorTarget,
                backdrop,
                drawCommand.sampleX0,
                sourceY0,
                drawCommand.sampleX1,
                sourceY1,
                0,
                drawCommand.sampleX0,
                sourceY0,
                drawCommand.sampleX1,
                sourceY1,
                0
        );
        if (drawCommand.frostScale > 1) {
            backdrop = executeLiquidGlassFrost(device, commandBuffer, drawCommand, drawCommand.uniformOffset, backdrop);
        }

        GraphicsPipeline pipeline = getOrCreateLiquidGlassPipeline(device, renderState.renderPass);
        pipeline.descriptorSet()
                .uniformBuffer("frame", 0, frameUniformBuffer)
                .uniformBufferRange("glass", 1, fragUniformBuffer, drawCommand.uniformOffset, LIQUID_GLASS_UNIFORM_SIZE)
                .samplerTexture("backdrop", 2, backdrop)
                .update();

        renderState.beginIfNeeded(commandBuffer);
        pipeline.setViewport(0.0f, 0.0f, viewportWidth, viewportHeight);
        pipeline.setScissor(0, 0, Math.max(1, (int) viewportWidth), Math.max(1, (int) viewportHeight));
        commandBuffer.bindPipeline(pipeline);
        commandBuffer.draw(dynamicVertexBuffer, drawCommand.vertexCount, drawCommand.firstVertex);
        renderState.endIfNeeded(commandBuffer);
    }

    private static void prepareLiquidGlassResources(IDevice device, List<DrawCommand> drawCommands) {
        boolean needsBackdrop = false;
        boolean needsFrost = false;
        for (DrawCommand drawCommand : drawCommands) {
            if (drawCommand.liquidGlass) {
                needsBackdrop = true;
                needsFrost |= drawCommand.frostScale > 1;
            }
        }
        if (!needsBackdrop) {
            return;
        }
        ITexture backdrop = ensureLiquidGlassBackdrop(device,
                targetFramebuffer.getTexture(FrameBufferAttachmentType.Color));
        if (needsFrost && liquidGlassFrostResources == null) {
            liquidGlassFrostResources = createLiquidGlassFrostResources(device, backdrop);
        }
    }

    private static ITexture ensureLiquidGlassBackdrop(IDevice device, ITexture colorTarget) {
        if (!colorTarget.getTextureUsages().getUsages().contains(TextureUsage.TransferSource)) {
            throw new IllegalStateException("NanoVG color target must support transfer-source copies for liquid glass");
        }
        if (liquidGlassBackdrop != null
                && liquidGlassBackdrop.getWidth() == colorTarget.getWidth()
                && liquidGlassBackdrop.getHeight() == colorTarget.getHeight()
                && liquidGlassBackdrop.getTextureFormat() == colorTarget.getTextureFormat()) {
            return liquidGlassBackdrop;
        }
        if (liquidGlassBackdrop != null) {
            waitForIdle.run();
            if (liquidGlassFrostResources != null) {
                liquidGlassFrostResources.destroy();
                liquidGlassFrostResources = null;
            }
            liquidGlassBackdrop.destroy();
        }
        liquidGlassBackdrop = device.createTexture(
                TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .format(colorTarget.getTextureFormat())
                        .size(colorTarget.getWidth(), colorTarget.getHeight())
                        .usages(TextureUsages.create().sampler().transferDestination())
                        .filterMode(TextureFilterMode.Linear)
                        .wrapMode(TextureWrapMode.ClampToEdge)
                        .mipmapSettings(TextureMipmapSettings.disabled())
                        .label("NanoVG Liquid Glass Backdrop")
                        .build()
        );
        if (liquidGlassBackdrop == null) {
            throw new IllegalStateException("Failed to allocate NanoVG liquid glass backdrop");
        }
        return liquidGlassBackdrop;
    }

    private static ITexture executeLiquidGlassFrost(IDevice device,
                                                   ICommandBuffer commandBuffer,
                                                   DrawCommand drawCommand,
                                                   int uniformOffset,
                                                   ITexture backdrop) {
        int scale = drawCommand.frostScale;
        LiquidGlassFrostResources resources = liquidGlassFrostResources;
        int x0 = drawCommand.sampleX0 / scale;
        int y0 = drawCommand.sampleY0 / scale;
        int x1 = (drawCommand.sampleX1 + scale - 1) / scale;
        int y1 = (drawCommand.sampleY1 + scale - 1) / scale;
        ITexture source = backdrop;
        int passCount = drawCommand.frostGaussian ? 3 : 1;
        for (int pass = 0; pass < passCount; pass++) {
            int target = pass & 1;
            ComputePipeline pipeline = getOrCreateLiquidGlassFrostPipeline(device, resources, pass);
            pipeline.descriptorSet()
                    .uniformBufferRange("glass", 1, fragUniformBuffer, uniformOffset, LIQUID_GLASS_UNIFORM_SIZE)
                    .samplerTexture("sourceTexture", 2, source);
            pipeline.descriptorSet().storageImage("outputImage", 4, resources.textures[target]);
            if (pass > 0) {
                pipeline.descriptorSet().uniformBufferRange("kernel", 3, fragUniformBuffer,
                        drawCommand.kernelOffset, LIQUID_GLASS_KERNEL_UNIFORM_SIZE);
            }
            pipeline.descriptorSet().update();
            commandBuffer.bindPipeline(pipeline);
            commandBuffer.dispatch(
                    divideRoundUp(x1 - x0, LIQUID_GLASS_FROST_LOCAL_SIZE),
                    divideRoundUp(y1 - y0, LIQUID_GLASS_FROST_LOCAL_SIZE),
                    1
            );
            source = resources.textures[target];
        }
        commandBuffer.memoryBarrier(MemoryBarrierType.ALL);
        return source;
    }

    private static int divideRoundUp(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static LiquidGlassFrostResources createLiquidGlassFrostResources(IDevice device,
                                                                            ITexture backdrop) {
        LiquidGlassFrostResources resources = new LiquidGlassFrostResources();
        // Every frost scale is at least two; one half-size workspace serves them all.
        int width = (backdrop.getWidth() + 1) / 2;
        int height = (backdrop.getHeight() + 1) / 2;
        try {
            for (int i = 0; i < resources.textures.length; i++) {
                resources.textures[i] = device.createTexture(TextureDescription.create()
                        .type(TextureType.Texture2D)
                        .format(TextureFormat.RGBA8)
                        .size(width, height)
                        .usages(TextureUsages.create().sampler().storage())
                        .filterMode(TextureFilterMode.Linear)
                        .wrapMode(TextureWrapMode.ClampToEdge)
                        .mipmapSettings(TextureMipmapSettings.disabled())
                        .label("NanoVG Frost Workspace " + i)
                        .build());
            }
            return resources;
        } catch (RuntimeException exception) {
            resources.destroy();
            throw exception;
        }
    }

    private static ComputePipeline getOrCreateLiquidGlassFrostPipeline(IDevice device,
                                                                       LiquidGlassFrostResources resources,
                                                                       int pass) {
        if (LIQUID_GLASS_FROST_SHADERS[pass] == null) {
            ShaderDescription.Builder description = ShaderDescription.compute(
                            ShaderSource.file(ShaderType.Compute, "/shader/nanovg/liquid_glass_frost.comp.glsl"))
                    .addDefine("FROST_PASS", Integer.toString(pass))
                    .uniformBuffer("glass", 1, LIQUID_GLASS_UNIFORM_SIZE)
                    .uniformSamplerTexture("sourceTexture", 2)
                    .uniformStorageTexture("outputImage", ShaderResourceAccess.Write, 4);
            if (pass > 0) {
                description.uniformBuffer("kernel", 3, LIQUID_GLASS_KERNEL_UNIFORM_SIZE);
            }
            IShaderProgram frostShader = device.createShaderProgram(description.build());
            frostShader.compile();
            LIQUID_GLASS_FROST_SHADERS[pass] = frostShader;
        }
        if (resources.pipelines[pass] == null) {
            resources.pipelines[pass] = ComputePipeline.builder()
                    .shader(LIQUID_GLASS_FROST_SHADERS[pass])
                    .build(device);
        }
        return resources.pipelines[pass];
    }

    private static GraphicsPipeline getOrCreateLiquidGlassPipeline(IDevice device, RenderPass renderPass) {
        if (liquidGlassShader == null) {
            liquidGlassShader = device.createShaderProgram(
                    ShaderDescription.graphics(
                                    ShaderSource.file(ShaderType.Fragment, "/shader/nanovg/liquid_glass.frag.glsl"),
                                    ShaderSource.file(ShaderType.Vertex, "/shader/nanovg/nanovg_rhi.vert.glsl")
                            )
                            .uniformBuffer("frame", 0, 16)
                            .uniformBuffer("glass", 1, LIQUID_GLASS_UNIFORM_SIZE)
                            .uniformSamplerTexture("backdrop", 2)
                            .build()
            );
            liquidGlassShader.compile();
        }
        if (liquidGlassPipeline == null) {
            ColorBlendAttachment blendAttachment = new ColorBlendAttachment(
                    true,
                    BlendFactor.One,
                    BlendFactor.OneMinusSrcAlpha,
                    BlendOp.Add,
                    BlendFactor.One,
                    BlendFactor.OneMinusSrcAlpha,
                    BlendOp.Add,
                    ColorComponentFlags.ALL
            );
            liquidGlassPipeline = GraphicsPipeline.builder()
                    .shader(liquidGlassShader)
                    .renderPass(renderPass)
                    .primitiveType(PrimitiveType.Triangle)
                    .rasterization(r -> r.cullMode(CullMode.None))
                    .depthStencil(d -> d.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                    .dynamicStates(DynamicStateFlags.ViewportScissor)
                    .colorBlend(cb -> cb.addAttachment(blendAttachment))
                    .vertexFormat(getVertexFormat())
                    .build(device);
        }
        return liquidGlassPipeline;
    }

    private static void ensureBufferCapacity(int vertexBytes, int uniformBytes) {
        boolean growVertexBuffer = dynamicVertexBuffer == null || dynamicVertexCapacity < vertexBytes;
        boolean growUniformBuffer = fragUniformBuffer == null || fragUniformBuffer.getSize() < uniformBytes;
        if ((growVertexBuffer && dynamicVertexBuffer != null) || (growUniformBuffer && fragUniformBuffer != null)) {
            waitForIdle.run();
        }
        if (growVertexBuffer) {
            int capacity = growCapacity(dynamicVertexCapacity, vertexBytes, 8192);
            IVertexBuffer replacement = device.createVertexBuffer(VertexBufferDescription.create(capacity, true, getVertexFormat()));
            if (dynamicVertexBuffer != null) {
                dynamicVertexBuffer.destroy();
            }
            dynamicVertexBuffer = replacement;
            dynamicVertexCapacity = capacity;
        }
        if (growUniformBuffer) {
            int capacity = growCapacity(fragUniformBuffer == null ? 0 : Math.toIntExact(fragUniformBuffer.getSize()), uniformBytes, 1024);
            IBuffer replacement = device.createBuffer(BufferDescription.create()
                    .size(capacity)
                    .usages(BufferUsages.create().ubo().transferDst())
                    .build());
            if (fragUniformBuffer != null) {
                fragUniformBuffer.destroy();
            }
            fragUniformBuffer = replacement;
        }
    }

    private static int growCapacity(int current, int required, int minimum) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(Math.max(required, minimum), (long) current * 2L));
    }

    private static void uploadFrameUniform(ICommandBuffer commandBuffer) {
        ByteBuffer frame = BufferUtils.createByteBuffer(16).order(ByteOrder.nativeOrder());
        frame.putFloat(viewportWidth);
        frame.putFloat(viewportHeight);
        frame.putFloat(0.0f);
        frame.putFloat(0.0f);
        frame.flip();
        commandBuffer.writeToBuffer(frameUniformBuffer, 0, frame);
    }

    private static ByteBuffer packFragUniforms(ByteBuffer uniforms, int uniformBytes, int sourceStride, int targetStride) {
        if (uniformBytes <= 0) {
            return BufferUtils.createByteBuffer(1).limit(0);
        }

        ByteBuffer src = uniforms.duplicate().order(ByteOrder.nativeOrder());
        src.position(0);
        src.limit(uniformBytes);

        int chunkCount = (uniformBytes + sourceStride - 1) / sourceStride;
        int lastChunkSize = uniformBytes - (chunkCount - 1) * sourceStride;
        int packedBytes = Math.toIntExact((long) (chunkCount - 1) * targetStride + lastChunkSize);
        if (stagedFragUniforms == null || stagedFragUniforms.capacity() < packedBytes) {
            stagedFragUniforms = BufferUtils.createByteBuffer(growCapacity(
                    stagedFragUniforms == null ? 0 : stagedFragUniforms.capacity(), packedBytes, 1024))
                    .order(ByteOrder.nativeOrder());
        }
        ByteBuffer packed = stagedFragUniforms;
        packed.clear();

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

    private static ByteBuffer appendLiquidGlassKernels(ByteBuffer packed, List<DrawCommand> drawCommands) {
        Map<Integer, Integer> offsets = new HashMap<>();
        List<float[]> kernels = new ArrayList<>();
        int firstOffset = alignTo(packed.limit(), uniformBufferAlignment);
        int stride = alignTo(LIQUID_GLASS_KERNEL_UNIFORM_SIZE, uniformBufferAlignment);
        for (DrawCommand drawCommand : drawCommands) {
            if (!drawCommand.liquidGlass || drawCommand.frostScale <= 1 || !drawCommand.frostGaussian) {
                continue;
            }
            float sigma = packed.getFloat(drawCommand.uniformOffset + 92) / (float) drawCommand.frostScale;
            int key = Float.floatToIntBits(sigma);
            Integer offset = offsets.get(key);
            if (offset == null) {
                offset = Math.toIntExact((long) firstOffset + (long) kernels.size() * stride);
                kernels.add(createLiquidGlassKernel(sigma));
                offsets.put(key, offset);
            }
            drawCommand.kernelOffset = offset;
        }
        if (kernels.isEmpty()) {
            return packed;
        }

        int packedBytes = Math.toIntExact((long) firstOffset
                + (long) (kernels.size() - 1) * stride + LIQUID_GLASS_KERNEL_UNIFORM_SIZE);
        if (packed.capacity() < packedBytes) {
            ByteBuffer replacement = BufferUtils.createByteBuffer(growCapacity(packed.capacity(), packedBytes, 1024))
                    .order(ByteOrder.nativeOrder());
            replacement.put(packed);
            stagedFragUniforms = replacement;
            packed = replacement;
        }
        packed.limit(packedBytes);
        for (int i = 0; i < kernels.size(); i++) {
            packed.position(firstOffset + i * stride);
            for (float value : kernels.get(i)) {
                packed.putFloat(value);
            }
        }
        packed.position(0);
        return packed;
    }

    private static float[] createLiquidGlassKernel(float sigma) {
        int span = Math.min((int) Math.ceil(sigma * 3.0f), 12);
        float[] weights = new float[span + 1];
        for (int i = 0; i <= span; i++) {
            float offset = i;
            weights[i] = (float) Math.exp(-0.5f * offset * offset / (sigma * sigma));
        }
        float total = 0.0f;
        for (int i = -span; i <= span; i++) {
            total += weights[Math.abs(i)];
        }
        float[] kernel = new float[LIQUID_GLASS_KERNEL_UNIFORM_SIZE / Float.BYTES];
        kernel[0] = weights[0] / total;
        kernel[1] = (span + 1) / 2;
        for (int i = 1, pair = 0; i <= span; i += 2, pair++) {
            float nextWeight = i < span ? weights[i + 1] : 0.0f;
            float weight = weights[i] + nextWeight;
            kernel[4 + pair * 2] = weight / total;
            kernel[5 + pair * 2] = i + nextWeight / weight;
        }
        return kernel;
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

    private static RenderPass getOrCreateLoadPass(IDevice device) {
        return PASS_CACHE.computeIfAbsent(LOAD_RENDER_PASS_KEY, ignored ->
                RenderPass.builder().frameBuffer(targetFramebuffer).build(device));
    }

    private static RenderPass getOrCreateDepthStencilClearPass(IDevice device) {
        return PASS_CACHE.computeIfAbsent(DEPTH_STENCIL_CLEAR_PASS_KEY, ignored ->
                RenderPass.builder().frameBuffer(targetFramebuffer)
                        .clearDepthOnBegin(1.0f)
                        .clearStencilOnBegin(0)
                        .build(device));
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
                    .dynamicStates(DynamicStateFlags.ViewportScissor)
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

    private static VertexFormat getVertexFormat() {
        return VertexFormat.builder()
                .addAttribute(0, "vertex", VertexAttributeFormat.FLOAT2)
                .addAttribute(1, "tcoord", VertexAttributeFormat.FLOAT2)
                .build();
    }

    private static final class LiquidGlassFrostResources {
        private final ITexture[] textures = new ITexture[2];
        private final ComputePipeline[] pipelines = new ComputePipeline[3];

        private void destroy() {
            for (ComputePipeline pipeline : pipelines) {
                if (pipeline != null) pipeline.destroy();
            }
            for (ITexture texture : textures) {
                if (texture != null) texture.destroy();
            }
        }
    }

    private static final class DrawCommand {
        private final PrimitiveType primitive;
        private final int firstVertex;
        private final int vertexCount;
        private final int uniformOffset;
        private final int uniformRange;
        private final int image;
        private final int fontImage;
        private final int blendSrcRgb;
        private final int blendDstRgb;
        private final int blendSrcAlpha;
        private final int blendDstAlpha;
        private final int stencilMode;
        private final int colorWriteMask;
        private final boolean liquidGlass;
        private final int sampleX0;
        private final int sampleY0;
        private final int sampleX1;
        private final int sampleY1;
        private final int frostScale;
        private final boolean frostGaussian;
        private int kernelOffset = -1;

        private DrawCommand(PrimitiveType primitive,
                            int firstVertex,
                            int vertexCount,
                            int uniformOffset,
                            int uniformRange,
                            int image,
                            int fontImage,
                            int blendSrcRgb,
                            int blendDstRgb,
                            int blendSrcAlpha,
                            int blendDstAlpha,
                            int stencilMode,
                            int colorWriteMask,
                            boolean liquidGlass,
                            int sampleX0,
                            int sampleY0,
                            int sampleX1,
                            int sampleY1,
                            int frostScale,
                            boolean frostGaussian) {
            this.primitive = primitive;
            this.firstVertex = firstVertex;
            this.vertexCount = vertexCount;
            this.uniformOffset = uniformOffset;
            this.uniformRange = uniformRange;
            this.image = image;
            this.fontImage = fontImage;
            this.blendSrcRgb = blendSrcRgb;
            this.blendDstRgb = blendDstRgb;
            this.blendSrcAlpha = blendSrcAlpha;
            this.blendDstAlpha = blendDstAlpha;
            this.stencilMode = stencilMode;
            this.colorWriteMask = colorWriteMask;
            this.liquidGlass = liquidGlass;
            this.sampleX0 = sampleX0;
            this.sampleY0 = sampleY0;
            this.sampleX1 = sampleX1;
            this.sampleY1 = sampleY1;
            this.frostScale = frostScale;
            this.frostGaussian = frostGaussian;
        }
    }

    private static final class RenderState {
        private final RenderPass renderPass;
        private boolean renderPassActive;

        private RenderState(RenderPass renderPass) {
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
            commandBuffer.memoryBarrier(MemoryBarrierType.ALL);
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
