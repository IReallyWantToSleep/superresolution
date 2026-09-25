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

package io.homo.superresolution.common.upscale;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.FullscreenQuad;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.state.CompareOp;
import io.homo.superresolution.core.graphics.impl.pipeline.state.CullMode;
import io.homo.superresolution.core.graphics.impl.pipeline.state.DynamicStateFlags;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;

import io.homo.superresolution.core.utils.FileReadHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class InteropResourcesPreprocessor {
    private static final Map<String, ComputePipeline> textureCopyPipelineCache = new HashMap<>();
    private static final Map<ProcessInputKey, ComputePipeline> processInputPipelineCache = new HashMap<>();
    private static final Map<TextureFormat, ComputePipeline> flipMotionVectorYPipelineCache = new HashMap<>();

    private static GraphicsPipeline depthPreprocessPipeline;
    private static IShaderProgram depthPreprocessShader;
    private static RenderPass depthPreprocessRenderPass;
    private static IFrameBuffer depthPreprocessFrameBuffer;

    private static boolean isInit = false;

    private static void destroyPipeline(ComputePipeline pipeline) {
        if (pipeline == null) {
            return;
        }
        IShaderProgram shader = pipeline.shader();
        pipeline.destroy();
        shader.destroy();
    }

    private static void destroyPipeline(GraphicsPipeline pipeline) {
        if (pipeline == null) {
            return;
        }
        IShaderProgram shader = pipeline.shader();
        pipeline.destroy();
        shader.destroy();
    }

    private static ComputePipeline getOrCreateTextureCopyPipeline(TextureFormat format, boolean flipY) {
        String formatQualifier = format.getGlslFormatQualifier();
        if (formatQualifier == null) {
            throw new IllegalArgumentException("Unsupported texture format for texture copy: " + format);
        }

        String key = (flipY ? "flipY_" : "copy_") + format.name();
        if (textureCopyPipelineCache.containsKey(key)) {
            return textureCopyPipelineCache.get(key);
        }

        ShaderDescription.Builder builder = ShaderDescription.create()
                .compute(new ShaderSource(ShaderType.Compute, "/shader/interop/flip_y.comp.glsl", true))
                .name("interop_" + key)
                .uniformSamplerTexture("inputTexture", 0)
                .uniformStorageTexture("outputTexture", 1);

        builder.addDefine("OUTPUT_FORMAT", formatQualifier);
        if (flipY) {
            builder.addDefine("FLIP_Y", "1");
        }

        IShaderProgram shader = RenderSystems.current().device().createShaderProgram(builder.build());
        shader.compile();
        ComputePipeline computePipeline = ComputePipeline.builder()
                .shader(shader)
                .build(RenderSystems.current().device());

        textureCopyPipelineCache.put(key, computePipeline);
        return computePipeline;
    }

    private static void initShaders() {
        depthPreprocessShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .fragment(
                                ShaderSource.file(
                                        ShaderType.Fragment,
                                        "/shader/interop/depth_preprocess.frag.glsl"
                                )
                        )
                        .vertex(
                                ShaderSource.file(
                                        ShaderType.Vertex,
                                        "/shader/blit.vert.glsl"
                                )
                        )
                        .name("depth_preprocess")
                        .uniformSamplerTexture("inputDepth", 0)
                        .build()
        );
        depthPreprocessShader.compile();
        depthPreprocessRenderPass = RenderSystems.current().device().createRenderPass(
                RenderPass.builder()
                        .frameBuffer(depthPreprocessFrameBuffer)
                        .clearDepthOnBegin(1.0f)
        );
        depthPreprocessPipeline = RenderSystems.current().device().createGraphicsPipeline(
                GraphicsPipeline.builder()
                        .shader(depthPreprocessShader)
                        .renderPass(depthPreprocessRenderPass)
                        .primitiveType(PrimitiveType.TriangleStrip)
                        .rasterization((r) -> r.cullMode(CullMode.None))
                        .depthStencil((r) -> r.depthCompareOp(CompareOp.Always).depthTestEnable(true).depthWriteEnable(true))
                        .dynamicStates(DynamicStateFlags.Viewport)
                        .vertexFormat(FullscreenQuad.getVertexFormat())
        );
    }


    public static void flipY(ITexture input, ITexture output) {
        if (!isInit) {
            init();
        }

        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        try {
            commandBuffer.begin();
            flipY(commandBuffer, input, output);
            commandBuffer.end();
            RenderSystems.current().device().submitCommandBuffer(commandBuffer);
            commandBuffer.waitForFence();
        } finally {
            commandBuffer.destroy();
        }
    }

    public static void flipY(ICommandBuffer commandBuffer, ITexture input, ITexture output) {
        copyTexture(commandBuffer, input, output, true);
    }

    public static void copyTexture(ICommandBuffer commandBuffer, ITexture input, ITexture output) {
        copyTexture(commandBuffer, input, output, false);
    }

    private static void copyTexture(
            ICommandBuffer commandBuffer,
            ITexture input,
            ITexture output,
            boolean flipY) {
        if (!isInit) {
            init();
        }

        TextureFormat outputFormat = output.getTextureFormat();
        ComputePipeline computePipeline = getOrCreateTextureCopyPipeline(outputFormat, flipY);
        computePipeline.descriptorSet().samplerTexture("inputTexture", input);
        computePipeline.descriptorSet().storageImage("outputTexture", output);
        computePipeline.descriptorSet().update();
        commandBuffer.bindPipeline(computePipeline);
        commandBuffer.dispatch(
                (input.getWidth() + 15) / 16,
                (input.getHeight() + 15) / 16,
                1
        );
    }

    private static void init() {
        if (isInit) {
            return;
        }
        depthPreprocessFrameBuffer = RenderSystems.current().device().createFramebuffer(
                FramebufferDescription.create()
                        .depthFormat(TextureFormat.DEPTH32F)
                        .size(16, 16)
                        .build());
        initShaders();
        isInit = true;
    }

    public static void destroy() {
        for (ComputePipeline pipeline : textureCopyPipelineCache.values()) {
            destroyPipeline(pipeline);
        }
        textureCopyPipelineCache.clear();

        for (ComputePipeline pipeline : processInputPipelineCache.values()) {
            destroyPipeline(pipeline);
        }
        processInputPipelineCache.clear();

        for (ComputePipeline pipeline : flipMotionVectorYPipelineCache.values()) {
            destroyPipeline(pipeline);
        }
        flipMotionVectorYPipelineCache.clear();

        destroyPipeline(depthPreprocessPipeline);
        depthPreprocessPipeline = null;
        depthPreprocessShader = null;

        if (depthPreprocessRenderPass != null) {
            depthPreprocessRenderPass.destroy();
            depthPreprocessRenderPass = null;
        }

        if (depthPreprocessFrameBuffer != null) {
            ITexture depthTexture = depthPreprocessFrameBuffer.getTexture(io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType.Depth);
            depthPreprocessFrameBuffer.destroy();
            if (depthTexture != null) {
                depthTexture.destroy();
            }
            depthPreprocessFrameBuffer = null;
        }

        isInit = false;
    }

    public static void processInputTextures(
            ITexture inputColor, ITexture outputColor,
            ITexture inputDepth, ITexture outputDepth,
            ITexture inputMotionVectors, ITexture outputMotionVectors,
            ITexture inputExposure, ITexture outputExposure) {
        processInputTextures(inputColor, outputColor, inputDepth, outputDepth,
                inputMotionVectors, outputMotionVectors, inputExposure, outputExposure, null);
    }

    public static void processInputTextures(
            ITexture inputColor, ITexture outputColor,
            ITexture inputDepth, ITexture outputDepth,
            ITexture inputMotionVectors, ITexture outputMotionVectors,
            ITexture inputExposure, ITexture outputExposure,
            @Nullable String motionVectorPreprocessingFunction) {
        processInputTextures(inputColor, outputColor, inputDepth, outputDepth,
                inputMotionVectors, outputMotionVectors, inputExposure, outputExposure,
                motionVectorPreprocessingFunction, true);
    }

    public static void processInputTextures(
            ITexture inputColor, ITexture outputColor,
            ITexture inputDepth, ITexture outputDepth,
            ITexture inputMotionVectors, ITexture outputMotionVectors,
            ITexture inputExposure, ITexture outputExposure,
            @Nullable String motionVectorPreprocessingFunction,
            boolean flipY) {
        if (!isInit) {
            init();
        }

        boolean hasColor = inputColor != null && outputColor != null;
        boolean hasDepth = inputDepth != null && outputDepth != null;
        boolean hasMV = inputMotionVectors != null && outputMotionVectors != null;
        boolean hasExposure = inputExposure != null && outputExposure != null;
        boolean hasMVPreprocessing = hasMV && motionVectorPreprocessingFunction != null;
        if (!hasColor && !hasDepth && !hasMV && !hasExposure) {
            return;
        }

        TextureFormat colorFormat = hasColor ? outputColor.getTextureFormat() : null;
        String colorFormatQualifier = hasColor ? colorFormat.getGlslFormatQualifier() : null;
        if (hasColor && colorFormatQualifier == null) {
            throw new IllegalArgumentException("Unsupported color format for processInputTextures: " + outputColor.getTextureFormat());
        }
        TextureFormat motionVectorFormat = hasMV ? outputMotionVectors.getTextureFormat() : null;
        String motionVectorFormatQualifier = hasMV ? motionVectorFormat.getGlslFormatQualifier() : null;
        if (hasMV && motionVectorFormatQualifier == null) {
            throw new IllegalArgumentException("Unsupported motion-vector format: " + motionVectorFormat);
        }
        TextureFormat exposureFormat = hasExposure ? outputExposure.getTextureFormat() : null;
        String exposureFormatQualifier = hasExposure ? exposureFormat.getGlslFormatQualifier() : null;
        if (hasExposure && exposureFormatQualifier == null) {
            throw new IllegalArgumentException("Unsupported exposure format: " + exposureFormat);
        }

        // Runs every frame, so the lookup key is a record rather than a rebuilt string.
        // Holding the injected function itself also removes the hashCode() collision the
        // old string key had, where two different functions could share a pipeline.
        ProcessInputKey key = new ProcessInputKey(
                colorFormat,
                motionVectorFormat,
                exposureFormat,
                hasColor,
                hasDepth,
                hasMV,
                hasExposure,
                hasMVPreprocessing ? motionVectorPreprocessingFunction : null,
                flipY
        );

        ComputePipeline pipeline = processInputPipelineCache.get(key);
        if (pipeline == null) {
            ShaderSource computeSource;
            if (hasMVPreprocessing) {
                ArrayList<String> sourceLines = FileReadHelper.readText("/shader/interop/process_input_textures.comp.glsl");
                String originalSource = String.join("\n", sourceLines);
                String modifiedSource = originalSource.replace("void main()",
                        motionVectorPreprocessingFunction + "\nvoid main()");
                computeSource = new ShaderSource(ShaderType.Compute, modifiedSource, false);
            } else {
                computeSource = new ShaderSource(ShaderType.Compute, "/shader/interop/process_input_textures.comp.glsl", true);
            }

            ShaderDescription.Builder builder = ShaderDescription.create()
                    .compute(computeSource)
                    .name("interop_" + key.shaderName());
            if (hasColor) {
                builder.uniformSamplerTexture("inputColor", 0)
                        .uniformStorageTexture("outputColor", 1)
                        .addDefine("HAS_COLOR", "1")
                        .addDefine("COLOR_FORMAT", colorFormatQualifier);
            }
            if (flipY) {
                builder.addDefine("FLIP_Y", "1");
            }

            if (hasDepth) {
                builder.uniformSamplerTexture("inputDepth", 2)
                        .uniformStorageTexture("outputDepth", 3);
                builder.addDefine("HAS_DEPTH", "1");
            }
            if (hasMV) {
                builder.uniformSamplerTexture("inputMotionVectors", 4)
                        .uniformStorageTexture("outputMotionVectors", 5);
                builder.addDefine("HAS_MOTION_VECTOR", "1");
                builder.addDefine("MOTION_VECTOR_FORMAT", motionVectorFormatQualifier);
            }
            if (hasMVPreprocessing) {
                builder.addDefine("MOTION_VECTOR_PREPROCESSING_FUNCTION_INJECTED", "1");
            }
            if (hasExposure) {
                builder.uniformSamplerTexture("inputExposure", 6)
                        .uniformStorageTexture("outputExposure", 7);
                builder.addDefine("HAS_EXPOSURE", "1");
                builder.addDefine("EXPOSURE_FORMAT", exposureFormatQualifier);
            }

            IShaderProgram shader = RenderSystems.current().device().createShaderProgram(builder.build());
            shader.compile();
            pipeline = ComputePipeline.builder()
                    .shader(shader)
                    .build(RenderSystems.current().device());
            processInputPipelineCache.put(key, pipeline);
        }

        if (hasColor) {
            pipeline.descriptorSet().samplerTexture("inputColor", inputColor);
            pipeline.descriptorSet().storageImage("outputColor", outputColor);
        }
        if (hasDepth) {
            pipeline.descriptorSet().samplerTexture("inputDepth", inputDepth);
            pipeline.descriptorSet().storageImage("outputDepth", outputDepth);
        }
        if (hasMV) {
            pipeline.descriptorSet().samplerTexture("inputMotionVectors", inputMotionVectors);
            pipeline.descriptorSet().storageImage("outputMotionVectors", outputMotionVectors);
        }
        if (hasExposure) {
            pipeline.descriptorSet().samplerTexture("inputExposure", inputExposure);
            pipeline.descriptorSet().storageImage("outputExposure", outputExposure);
        }
        pipeline.descriptorSet().update();

        int dispatchWidth = Math.max(Math.max(hasColor ? outputColor.getWidth() : 0,
                        hasDepth ? outputDepth.getWidth() : 0),
                Math.max(hasMV ? outputMotionVectors.getWidth() : 0, hasExposure ? 1 : 0));
        int dispatchHeight = Math.max(Math.max(hasColor ? outputColor.getHeight() : 0,
                        hasDepth ? outputDepth.getHeight() : 0),
                Math.max(hasMV ? outputMotionVectors.getHeight() : 0, hasExposure ? 1 : 0));
        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        try {
            commandBuffer.begin();
            commandBuffer.bindPipeline(pipeline);
            commandBuffer.dispatch(
                    (dispatchWidth + 15) / 16,
                    (dispatchHeight + 15) / 16,
                    1
            );
            commandBuffer.end();
            RenderSystems.current().device().submitCommandBuffer(commandBuffer);
            commandBuffer.waitForFence();
        } finally {
            commandBuffer.destroy();
        }
    }

    private record ProcessInputKey(
            @Nullable TextureFormat colorFormat,
            @Nullable TextureFormat motionVectorFormat,
            @Nullable TextureFormat exposureFormat,
            boolean hasColor,
            boolean hasDepth,
            boolean hasMotionVectors,
            boolean hasExposure,
            @Nullable String motionVectorPreprocessingFunction,
            boolean flipY
    ) {
        private String shaderName() {
            return "processInput_"
                    + (hasColor ? "_C" + colorFormat.name() : "")
                    + (hasDepth ? "_D" : "")
                    + (hasMotionVectors ? "_MV" + motionVectorFormat.name() : "")
                    + (hasExposure ? "_E" + exposureFormat.name() : "")
                    + (flipY ? "_FlipY" : "_NoFlipY")
                    + (motionVectorPreprocessingFunction == null
                            ? ""
                            : "_MVP" + motionVectorPreprocessingFunction.hashCode());
        }
    }

    public static void flipMotionVectorY(ITexture input, ITexture output) {
        if (!isInit) {
            init();
        }

        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        try {
            commandBuffer.begin();
            flipMotionVectorY(commandBuffer, input, output);
            commandBuffer.end();
            RenderSystems.current().device().submitCommandBuffer(commandBuffer);
            commandBuffer.waitForFence();
        } finally {
            commandBuffer.destroy();
        }
    }

    public static void flipMotionVectorY(ICommandBuffer commandBuffer, ITexture input, ITexture output) {
        if (!isInit) {
            init();
        }

        TextureFormat format = output.getTextureFormat();
        ComputePipeline pipeline = flipMotionVectorYPipelineCache.get(format);
        if (pipeline == null) {
            String qualifier = format.getGlslFormatQualifier();
            if (qualifier == null) {
                throw new IllegalArgumentException("Unsupported motion-vector format: " + format);
            }
            IShaderProgram shader = RenderSystems.current().device().createShaderProgram(
                    ShaderDescription.create()
                            .compute(ShaderSource.file(
                                    ShaderType.Compute, "/shader/interop/flip_motion_vector_y.comp.glsl"))
                            .name("interop_flip_motion_vector_y_" + format.name())
                            .addDefine("MOTION_VECTOR_FORMAT", qualifier)
                            .uniformSamplerTexture("inputMotionVector", 0)
                            .uniformStorageTexture("outputMotionVector", 1)
                            .build());
            shader.compile();
            pipeline = ComputePipeline.builder().shader(shader).build(RenderSystems.current().device());
            flipMotionVectorYPipelineCache.put(format, pipeline);
        }
        pipeline.descriptorSet().samplerTexture("inputMotionVector", input);
        pipeline.descriptorSet().storageImage("outputMotionVector", output);
        pipeline.descriptorSet().update();
        commandBuffer.bindPipeline(pipeline);
        commandBuffer.dispatch(
                (input.getWidth() + 15) / 16,
                (input.getHeight() + 15) / 16,
                1
        );
    }
}
