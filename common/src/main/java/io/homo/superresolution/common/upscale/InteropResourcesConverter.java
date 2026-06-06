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


public class InteropResourcesConverter {
    private static final Map<String, ComputePipeline> flipYPipelineCache = new HashMap<>();
    private static final Map<String, ComputePipeline> processInputPipelineCache = new HashMap<>();
    private static ComputePipeline flipMotionVectorYPipeline;
    private static IShaderProgram flipMotionVectorYShader;

    private static GraphicsPipeline depthPreprocessPipeline;
    private static IShaderProgram depthPreprocessShader;
    private static RenderPass depthPreprocessRenderPass;
    private static IFrameBuffer depthPreprocessFrameBuffer;

    private static boolean isInit = false;

    private static String toComputeShaderFormatQualifier(TextureFormat format) {
        return switch (format) {
            case RGBA8 -> "rgba8";
            case RGBA16F -> "rgba16f";
            case RGBA16 -> "rgba16";
            case RGB8, RGB16F -> null;
            case RG16F -> "rg16f";
            case RG32F -> "rg32f";
            case RG8 -> "rg8";
            case R16F -> "r16f";
            case R8 -> "r8";
            case R32F -> "r32f";
            case R32UI -> "r32ui";
            case R16_SNORM -> "r16_snorm";
            case R11G11B10F -> "r11f_g11f_b10f";
            default -> null;
        };
    }

    private static ComputePipeline getOrCreateFlipYPipeline(TextureFormat format) {
        String formatQualifier = toComputeShaderFormatQualifier(format);
        if (formatQualifier == null) {
            throw new IllegalArgumentException("Unsupported texture format for flipY: " + format);
        }

        String key = "flipY_" + format.name();
        if (flipYPipelineCache.containsKey(key)) {
            return flipYPipelineCache.get(key);
        }

        ShaderDescription.Builder builder = ShaderDescription.create()
                .compute(new ShaderSource(ShaderType.Compute, "/shader/interop/flip_y.comp.glsl", true))
                .name("interop_flip_y_" + format.name())
                .uniformSamplerTexture("inputTexture", 0)
                .uniformStorageTexture("outputTexture", 1);

        builder.addDefine("OUTPUT_FORMAT", formatQualifier);

        IShaderProgram shader = RenderSystems.current().device().createShaderProgram(builder.build());
        shader.compile();
        ComputePipeline computePipeline = ComputePipeline.builder()
                .shader(shader)
                .build(RenderSystems.opengl().device());

        flipYPipelineCache.put(key, computePipeline);
        return computePipeline;
    }

    private static void initShaders() {
        flipMotionVectorYShader = RenderSystems.current().device().createShaderProgram(
                ShaderDescription.create()
                        .compute(
                                ShaderSource.file(ShaderType.Compute, "/shader/interop/flip_motion_vector_y.comp.glsl"))
                        .name("interop_flip_motion_vector_y")
                        .uniformSamplerTexture("inputMotionVector", 0)
                        .uniformStorageTexture("outputMotionVector", 1)
                        .build());
        flipMotionVectorYShader.compile();
        flipMotionVectorYPipeline = ComputePipeline.builder()
                .shader(flipMotionVectorYShader)
                .build(RenderSystems.opengl().device());

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

        TextureFormat outputFormat = output.getTextureFormat();
        ComputePipeline computePipeline = getOrCreateFlipYPipeline(outputFormat);

        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        computePipeline.descriptorSet().samplerTexture("inputTexture", input);
        computePipeline.descriptorSet().storageImage("outputTexture", output);
        computePipeline.descriptorSet().update();
        commandBuffer.begin();
        commandBuffer.bindPipeline(computePipeline);
        commandBuffer.dispatch(
                (input.getWidth() + 15) / 16,
                (input.getHeight() + 15) / 16,
                1
        );
        commandBuffer.end();
        RenderSystems.current().device().submitCommandBuffer(commandBuffer);
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
        if (!isInit) {
            init();
        }

        boolean hasDepth = inputDepth != null && outputDepth != null;
        boolean hasMV = inputMotionVectors != null && outputMotionVectors != null;
        boolean hasExposure = inputExposure != null && outputExposure != null;
        boolean hasMVPreprocessing = hasMV && motionVectorPreprocessingFunction != null;

        String colorFormatQualifier = toComputeShaderFormatQualifier(outputColor.getTextureFormat());
        if (colorFormatQualifier == null) {
            throw new IllegalArgumentException("Unsupported color format for processInputTextures: " + outputColor.getTextureFormat());
        }

        String key = "processInput_" + outputColor.getTextureFormat().name()
                + (hasDepth ? "_D" : "")
                + (hasMV ? "_MV" : "")
                + (hasExposure ? "_E" : "")
                + (hasMVPreprocessing ? "_MVP" : "")
                + (motionVectorPreprocessingFunction == null ? "" : motionVectorPreprocessingFunction.hashCode());

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
                    .name("interop_" + key)
                    .uniformSamplerTexture("inputColor", 0)
                    .uniformStorageTexture("outputColor", 1);
            builder.addDefine("COLOR_FORMAT", colorFormatQualifier);

            if (hasDepth) {
                builder.uniformSamplerTexture("inputDepth", 2)
                        .uniformStorageTexture("outputDepth", 3);
                builder.addDefine("HAS_DEPTH", "1");
            }
            if (hasMV) {
                builder.uniformSamplerTexture("inputMotionVectors", 4)
                        .uniformStorageTexture("outputMotionVectors", 5);
                builder.addDefine("HAS_MOTION_VECTOR", "1");
            }
            if (hasMVPreprocessing) {
                builder.addDefine("MOTION_VECTOR_PREPROCESSING_FUNCTION_INJECTED", "1");
            }
            if (hasExposure) {
                builder.uniformSamplerTexture("inputExposure", 6)
                        .uniformStorageTexture("outputExposure", 7);
                builder.addDefine("HAS_EXPOSURE", "1");
            }

            IShaderProgram shader = RenderSystems.current().device().createShaderProgram(builder.build());
            shader.compile();
            pipeline = ComputePipeline.builder()
                    .shader(shader)
                    .build(RenderSystems.opengl().device());
            processInputPipelineCache.put(key, pipeline);
        }

        pipeline.descriptorSet().samplerTexture("inputColor", inputColor);
        pipeline.descriptorSet().storageImage("outputColor", outputColor);
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

        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        commandBuffer.begin();
        commandBuffer.bindPipeline(pipeline);
        commandBuffer.dispatch(
                (outputColor.getWidth() + 15) / 16,
                (outputColor.getHeight() + 15) / 16,
                1
        );
        commandBuffer.end();
        RenderSystems.current().device().submitCommandBuffer(commandBuffer);
    }

    public static void flipMotionVectorY(ITexture input, ITexture output) {
        if (!isInit) {
            init();
        }

        ICommandBuffer commandBuffer = RenderSystems.current().device().defaultCommandPool().createCommandBuffer();
        flipMotionVectorYPipeline.descriptorSet().samplerTexture("inputMotionVector", input);
        flipMotionVectorYPipeline.descriptorSet().storageImage("outputMotionVector", output);
        flipMotionVectorYPipeline.descriptorSet().update();
        commandBuffer.begin();
        commandBuffer.bindPipeline(flipMotionVectorYPipeline);
        commandBuffer.dispatch(
                (input.getWidth() + 15) / 16,
                (input.getHeight() + 15) / 16,
                1
        );
        commandBuffer.end();
        RenderSystems.current().device().submitCommandBuffer(commandBuffer);
    }
}
