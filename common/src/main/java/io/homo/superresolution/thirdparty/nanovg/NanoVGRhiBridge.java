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
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.buffer.StaticBufferData;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.*;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.state.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttributeFormat;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.impl.vertex.VertexFormat;
import io.homo.superresolution.core.graphics.opengl.GlDevice;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

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

    private static final int VERTEX_STRIDE_BYTES = 16;
    private static final int PATH_STRIDE_BYTES = 16;
    private static final int CALL_STRIDE_BYTES = 44;

    private static final Map<Integer, ITexture> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<Long, RenderPass> PASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Long, GraphicsPipeline> GRAPHICS_PIPELINE_CACHE = new ConcurrentHashMap<>();
    private static final CurrentFramebuffer TARGET_FRAMEBUFFER = new CurrentFramebuffer();

    private static ITexture dummyTexture;
    private static IShaderProgram shader;
    private static IBuffer frameUniformBuffer;
    private static IBuffer fragUniformBuffer;
    private static IVertexBuffer dynamicVertexBuffer;
    private static int dynamicVertexCapacity;

    private static float viewportWidth;
    private static float viewportHeight;

    private NanoVGRhiBridge() {
    }

    public static boolean nCreateTexture(int imageId, int type, int width, int height, int imageFlags, ByteBuffer data, int dataSize) {
        IDevice device = RenderSystems.current().device();

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
            if (texture instanceof GlTexture2D glTexture2D) {
                ByteBuffer upload = duplicateForUpload(data, dataSize);
                int pixelFormat = type == NVG_TEXTURE_RGBA ? GL_RGBA : GL_RED;
                glTexture2D.uploadData(0, 0, 0, width, height, pixelFormat, GL_UNSIGNED_BYTE, upload, 1);
                if ((imageFlags & NVG_IMAGE_GENERATE_MIPMAPS) != 0) {
                    glTexture2D.generateMipmap();
                }
            } else {
                texture.destroy();
                return false;
            }
        }

        TEXTURES.put(imageId, texture);
        return true;
    }

    public static boolean nRegisterExternalTexture(int imageId, int externalTextureHandle, int width, int height, int imageFlags) {
        TextureFormat format = TextureFormat.RGBA8;
        TextureFilterMode filterMode = (imageFlags & NVG_IMAGE_NEAREST) != 0 ? TextureFilterMode.Nearest : TextureFilterMode.Linear;
        TextureWrapMode wrapMode = ((imageFlags & NVG_IMAGE_REPEATX) != 0 || (imageFlags & NVG_IMAGE_REPEATY) != 0)
                ? TextureWrapMode.Repeat
                : TextureWrapMode.ClampToEdge;

        ITexture external = new ExternalTextureRef(externalTextureHandle, width, height, format, filterMode, wrapMode);
        TEXTURES.put(imageId, external);
        return true;
    }

    public static boolean nUpdateTexture(int imageId, int x, int y, int width, int height, ByteBuffer data, int dataSize) {
        ITexture texture = TEXTURES.get(imageId);
        if (texture == null) {
            return false;
        }
        if (!(texture instanceof GlTexture2D glTexture2D)) {
            return false;
        }

        if (data == null || dataSize <= 0) {
            return true;
        }

        TextureFormat format = glTexture2D.getTextureFormat();
        int pixelFormat = format == TextureFormat.RGBA8 || format == TextureFormat.RGB8 ? GL_RGBA : GL_RED;
        ByteBuffer upload = duplicateForUpload(data, dataSize);
        glTexture2D.uploadData(0, x, y, width, height, pixelFormat, GL_UNSIGNED_BYTE, upload, 1);
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
        if (!(device instanceof GlDevice)) {
            return;
        }

        ensureRendererResources(device);

        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        TARGET_FRAMEBUFFER.update(drawFbo, Math.max(1, (int) viewWidth), Math.max(1, (int) viewHeight));

        viewportWidth = viewWidth;
        viewportHeight = viewHeight;

        ByteBuffer vertsData = verts.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer pathsData = paths == null ? null : paths.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer callsData = calls.duplicate().order(ByteOrder.nativeOrder());
        ByteBuffer uniformsData = uniforms.duplicate().order(ByteOrder.nativeOrder());

        ICommandBuffer commandBuffer = device.defaultCommandPool().createCommandBuffer();
        commandBuffer.begin();
        for (int i = 0; i < ncalls; i++) {
            int callBase = i * CALL_STRIDE_BYTES;
            int type = callsData.getInt(callBase);
            int image = callsData.getInt(callBase + 4);
            int pathOffset = callsData.getInt(callBase + 8);
            int pathCount = callsData.getInt(callBase + 12);
            int triangleOffset = callsData.getInt(callBase + 16);
            int triangleCount = callsData.getInt(callBase + 20);
            int uniformOffset = callsData.getInt(callBase + 24);
            int blendSrcRgb = callsData.getInt(callBase + 28);
            int blendDstRgb = callsData.getInt(callBase + 32);
            int blendSrcAlpha = callsData.getInt(callBase + 36);
            int blendDstAlpha = callsData.getInt(callBase + 40);

            switch (type) {
                case GLNVG_FILL -> drawFillLike(
                        device,
                        commandBuffer,
                        vertsData,
                        pathsData,
                        uniformsData,
                        fragSize,
                        image,
                        pathOffset,
                        pathCount,
                        uniformOffset + fragSize,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                case GLNVG_CONVEXFILL -> drawFillLike(
                        device,
                        commandBuffer,
                        vertsData,
                        pathsData,
                        uniformsData,
                        fragSize,
                        image,
                        pathOffset,
                        pathCount,
                        uniformOffset,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                case GLNVG_STROKE -> drawStrokeLike(
                        device,
                        commandBuffer,
                        vertsData,
                        pathsData,
                        uniformsData,
                        fragSize,
                        image,
                        pathOffset,
                        pathCount,
                        uniformOffset,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                case GLNVG_TRIANGLES -> drawTrianglesRange(
                        device,
                        commandBuffer,
                        vertsData,
                        uniformsData,
                        fragSize,
                        image,
                        triangleOffset,
                        triangleCount,
                        uniformOffset,
                        blendSrcRgb,
                        blendDstRgb,
                        blendSrcAlpha,
                        blendDstAlpha);
                default -> {
                }
            }
        }
        commandBuffer.end();
        device.submitCommandBuffer(commandBuffer);
    }

    public static void nDestroy() {
        for (ITexture texture : TEXTURES.values()) {
            texture.destroy();
        }
        TEXTURES.clear();
    }

    private static ByteBuffer duplicateForUpload(ByteBuffer src, int size) {
        ByteBuffer copy = BufferUtils.createByteBuffer(size);
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
                            .usage(BufferUsage.Ubo)
                            .build()
            );
        }

        if (fragUniformBuffer == null) {
            fragUniformBuffer = device.createBuffer(
                    BufferDescription.create()
                            .size(1024)
                            .usage(BufferUsage.Ubo)
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
            if (dummyTexture instanceof GlTexture2D glTexture2D) {
                ByteBuffer white = BufferUtils.createByteBuffer(4);
                white.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
                glTexture2D.uploadData(0, 0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, white, 1);
            }
        }
    }

    private static void drawFillLike(IDevice device,
                                     ICommandBuffer commandBuffer,
                                     ByteBuffer verts,
                                     ByteBuffer paths,
                                     ByteBuffer uniforms,
                                     int fragSize,
                                     int image,
                                     int pathOffset,
                                     int pathCount,
                                     int uniformOffset,
                                     int blendSrcRgb,
                                     int blendDstRgb,
                                     int blendSrcAlpha,
                                     int blendDstAlpha) {
        if (paths == null) {
            return;
        }
        for (int i = 0; i < pathCount; i++) {
            int pathBase = (pathOffset + i) * PATH_STRIDE_BYTES;
            int fillOffset = paths.getInt(pathBase);
            int fillCount = paths.getInt(pathBase + 4);
            int strokeOffset = paths.getInt(pathBase + 8);
            int strokeCount = paths.getInt(pathBase + 12);

            if (fillCount >= 3) {
                ByteBuffer fanTriangles = buildTriangleFan(verts, fillOffset, fillCount);
                uploadAndDraw(device, commandBuffer, PrimitiveType.Triangle, fanTriangles, uniforms, uniformOffset,
                        fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            }
            if (strokeCount > 0) {
                ByteBuffer strip = buildRange(verts, strokeOffset, strokeCount);
                uploadAndDraw(device, commandBuffer, PrimitiveType.TriangleStrip, strip, uniforms, uniformOffset,
                        fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            }
        }
    }

    private static void drawStrokeLike(IDevice device,
                                       ICommandBuffer commandBuffer,
                                       ByteBuffer verts,
                                       ByteBuffer paths,
                                       ByteBuffer uniforms,
                                       int fragSize,
                                       int image,
                                       int pathOffset,
                                       int pathCount,
                                       int uniformOffset,
                                       int blendSrcRgb,
                                       int blendDstRgb,
                                       int blendSrcAlpha,
                                       int blendDstAlpha) {
        if (paths == null) {
            return;
        }
        for (int i = 0; i < pathCount; i++) {
            int pathBase = (pathOffset + i) * PATH_STRIDE_BYTES;
            int strokeOffset = paths.getInt(pathBase + 8);
            int strokeCount = paths.getInt(pathBase + 12);
            if (strokeCount <= 0) {
                continue;
            }
            ByteBuffer strip = buildRange(verts, strokeOffset, strokeCount);
            uploadAndDraw(device, commandBuffer, PrimitiveType.TriangleStrip, strip, uniforms, uniformOffset,
                    fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        }
    }

    private static void drawTrianglesRange(IDevice device,
                                           ICommandBuffer commandBuffer,
                                           ByteBuffer verts,
                                           ByteBuffer uniforms,
                                           int fragSize,
                                           int image,
                                           int triangleOffset,
                                           int triangleCount,
                                           int uniformOffset,
                                           int blendSrcRgb,
                                           int blendDstRgb,
                                           int blendSrcAlpha,
                                           int blendDstAlpha) {
        if (triangleCount <= 0) {
            return;
        }
        ByteBuffer tri = buildRange(verts, triangleOffset, triangleCount);
        uploadAndDraw(device, commandBuffer, PrimitiveType.Triangle, tri, uniforms, uniformOffset,
                fragSize, image, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
    }

    private static void uploadAndDraw(IDevice device,
                                      ICommandBuffer commandBuffer,
                                      PrimitiveType primitive,
                                      ByteBuffer vertices,
                                      ByteBuffer uniforms,
                                      int uniformOffset,
                                      int fragSize,
                                      int image,
                                      int blendSrcRgb,
                                      int blendDstRgb,
                                      int blendSrcAlpha,
                                      int blendDstAlpha) {
        if (vertices == null || vertices.remaining() <= 0) {
            return;
        }

        ensureVertexCapacity(device, vertices.remaining());

        ByteBuffer vertexData = vertices.duplicate().order(ByteOrder.nativeOrder());
        dynamicVertexBuffer.updateData(vertexData, 0);

        uploadFrameUniform();
        uploadFragUniform(uniforms, uniformOffset, fragSize);

        RenderPass pass = getOrCreatePass(device, primitive, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        long key = makePassKey(primitive, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        GraphicsPipeline pipeline = GRAPHICS_PIPELINE_CACHE.get(key);
        if (pipeline == null) {
            throw new IllegalStateException("Graphics pipeline cache mismatch for key: " + key);
        }

        ITexture texture = TEXTURES.getOrDefault(image, dummyTexture);

        pipeline.descriptorSet()
                .uniformBuffer("frame", 0, frameUniformBuffer)
                .uniformBuffer("frag", 1, fragUniformBuffer)
                .samplerTexture("tex", 2, texture)
                .update();

        pipeline.setViewport(0.0f, 0.0f, viewportWidth, viewportHeight);

        int vertexCount = vertices.remaining() / VERTEX_STRIDE_BYTES;
        device.commandDecoder().beginRenderPass(commandBuffer, pass);
        device.commandDecoder().bindPipeline(commandBuffer, pipeline);
        device.commandDecoder().draw(commandBuffer, dynamicVertexBuffer, vertexCount, 0);
        device.commandDecoder().endRenderPass(commandBuffer);
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

    private static void uploadFrameUniform() {
        ByteBuffer frame = BufferUtils.createByteBuffer(16).order(ByteOrder.nativeOrder());
        frame.putFloat(viewportWidth);
        frame.putFloat(viewportHeight);
        frame.putFloat(0.0f);
        frame.putFloat(0.0f);
        frame.flip();

        StaticBufferData frameData = StaticBufferData.copy(frame);
        frameUniformBuffer.setBufferData(frameData);
        frameUniformBuffer.upload();
        frameData.free();
    }

    private static void uploadFragUniform(ByteBuffer uniforms, int uniformOffset, int fragSize) {
        ByteBuffer frag = BufferUtils.createByteBuffer(fragSize).order(ByteOrder.nativeOrder());
        ByteBuffer src = uniforms.duplicate().order(ByteOrder.nativeOrder());
        src.position(uniformOffset);
        src.limit(uniformOffset + fragSize);
        frag.put(src);
        frag.flip();

        StaticBufferData fragData = StaticBufferData.copy(frag);
        fragUniformBuffer.setBufferData(fragData);
        fragUniformBuffer.upload();
        fragData.free();
    }

    private static long makePassKey(PrimitiveType primitive,
                                    int blendSrcRgb,
                                    int blendDstRgb,
                                    int blendSrcAlpha,
                                    int blendDstAlpha) {
        return ((((long) primitive.ordinal()) & 0xFFL) << 56)
                | ((((long) blendSrcRgb) & 0xFFFFFFFFL))
                | ((((long) blendDstRgb) & 0xFFFFFFFFL) << 32)
                ^ ((((long) blendSrcAlpha) & 0xFFFFFFFFL) << 17)
                ^ ((((long) blendDstAlpha) & 0xFFFFFFFFL) << 49);
    }

    private static RenderPass getOrCreatePass(IDevice device,
                              PrimitiveType primitive,
                                              int blendSrcRgb,
                                              int blendDstRgb,
                                              int blendSrcAlpha,
                                              int blendDstAlpha) {
        long key = makePassKey(primitive, blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);

        return PASS_CACHE.computeIfAbsent(key, ignored -> {
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
                    ColorComponentFlags.ALL
            );

                RenderPass pass = RenderPass.builder()
                    .frameBuffer(TARGET_FRAMEBUFFER)
                    .build(device);

                GraphicsPipeline pipeline = GraphicsPipeline.builder()
                    .shader(shader)
                    .renderPass(pass)
                    .primitiveType(primitive)
                    .rasterization(r -> r.cullMode(CullMode.None))
                    .depthStencil(d -> d.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                    .dynamicStates(DynamicStateFlags.Viewport)
                    .colorBlend(cb -> cb.addAttachment(attachment))
                    .vertexFormat(getVertexFormat())
                    .build(device);
                GRAPHICS_PIPELINE_CACHE.put(key, pipeline);
                return pass;
        });
    }

    private static BlendFactor mapBlendFactor(int glFactor) {
        return switch (glFactor) {
            case GL11.GL_ZERO -> BlendFactor.Zero;
            case GL11.GL_ONE -> BlendFactor.One;
            case GL11.GL_SRC_COLOR -> BlendFactor.SrcColor;
            case GL11.GL_ONE_MINUS_SRC_COLOR -> BlendFactor.OneMinusSrcColor;
            case GL11.GL_DST_COLOR -> BlendFactor.DstColor;
            case GL11.GL_ONE_MINUS_DST_COLOR -> BlendFactor.OneMinusDstColor;
            case GL11.GL_SRC_ALPHA -> BlendFactor.SrcAlpha;
            case GL11.GL_ONE_MINUS_SRC_ALPHA -> BlendFactor.OneMinusSrcAlpha;
            case GL11.GL_DST_ALPHA -> BlendFactor.DstAlpha;
            case GL11.GL_ONE_MINUS_DST_ALPHA -> BlendFactor.OneMinusDstAlpha;
            case GL11.GL_SRC_ALPHA_SATURATE -> BlendFactor.SrcAlphaSaturate;
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

    private static final class CurrentFramebuffer implements IBindableFrameBuffer {
        private int fboHandle;
        private int width = 1;
        private int height = 1;

        private void update(int fboHandle, int width, int height) {
            this.fboHandle = fboHandle;
            this.width = width;
            this.height = height;
        }

        @Override
        public void bind(FrameBufferBindPoint bindPoint, boolean setViewport) {
            bind(bindPoint);
            if (setViewport) {
                GL11.glViewport(0, 0, width, height);
            }
        }

        @Override
        public void bind(FrameBufferBindPoint bindPoint) {
            int target = switch (bindPoint) {
                case Read -> GL30.GL_READ_FRAMEBUFFER;
                case Write -> GL30.GL_DRAW_FRAMEBUFFER;
                case All -> GL30.GL_FRAMEBUFFER;
            };
            GL30.glBindFramebuffer(target, fboHandle);
        }

        @Override
        public void unbind(FrameBufferBindPoint bindPoint) {
            int target = switch (bindPoint) {
                case Read -> GL30.GL_READ_FRAMEBUFFER;
                case Write -> GL30.GL_DRAW_FRAMEBUFFER;
                case All -> GL30.GL_FRAMEBUFFER;
            };
            GL30.glBindFramebuffer(target, 0);
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
        public void clearFrameBuffer() {
        }

        @Override
        public java.util.List<ColorAttachment> getColorAttachments() {
            return java.util.List.of();
        }

        @Override
        public DepthStencilAttachment getDepthStencilAttachment() {
            return null;
        }

        @Override
        public int getTextureId(FrameBufferAttachmentType attachmentType) {
            return 0;
        }

        @Override
        public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
            return null;
        }

        @Override
        public void setClearColorRGBA(float red, float green, float blue, float alpha) {
        }

        @Override
        public TextureFormat getColorTextureFormat() {
            return TextureFormat.RGBA8;
        }

        @Override
        public TextureFormat getDepthTextureFormat() {
            return null;
        }

        @Override
        public void destroy() {
        }

        @Override
        public long handle() {
            return fboHandle;
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
            // External texture handle lifecycle is managed by owner.
        }

        @Override
        public long handle() {
            return handle;
        }
    }
}
