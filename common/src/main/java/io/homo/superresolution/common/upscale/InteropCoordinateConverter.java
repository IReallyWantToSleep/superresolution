/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobBuilders;
import io.homo.superresolution.core.graphics.impl.grape.GrapeJobResource;
import io.homo.superresolution.core.graphics.impl.grape.GrapeResourceAccess;
import io.homo.superresolution.core.graphics.impl.grape.RenderGrape;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;


public class InteropCoordinateConverter {
    private static final Map<String, ComputePipeline> flipYPipelineCache = new HashMap<>();
    private static ComputePipeline flipMotionVectorYPipeline;
    private static IShaderProgram flipMotionVectorYShader;

    private static RenderGrape pipeline;
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

        GlShaderProgram shader = RenderSystems.current().device().createShaderProgram(builder.build());
        shader.compile();
        ComputePipeline computePipeline = (ComputePipeline) GlComputePipeline.builder()
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
        flipMotionVectorYPipeline = GlComputePipeline.builder()
                .shader(flipMotionVectorYShader)
                .build(RenderSystems.opengl().device());
    }

    private static void init() {
        if (isInit)
            return;

        initShaders();
        pipeline = new RenderGrape();
        isInit = true;
    }

    public static void flipY(ITexture input, ITexture output) {
        if (!isInit)
            init();

        TextureFormat outputFormat = output.getTextureFormat();
        ComputePipeline computePipeline = getOrCreateFlipYPipeline(outputFormat);

        RenderSystems.opengl().device().commandDecoder().beginCommandBuffer();
        ICommandBuffer commandBuffer = RenderSystems.current().device().commandDecoder().currentCommandBuffer();
        pipeline.remove("flip_y");
        pipeline.add("flip_y",
                GrapeJobBuilders.compute(computePipeline)
                        .resource("inputTexture", GrapeJobResource.SamplerTexture.create(input))
                        .resource("outputTexture",
                                GrapeJobResource.StorageTexture.create(output, GrapeResourceAccess.Write))
                        .workGroup(
                                (input.getWidth() + 15) / 16,
                                (input.getHeight() + 15) / 16,
                                1)
                        .build());

        pipeline.execute(commandBuffer, "flip_y");
        RenderSystems.opengl().device().commandDecoder().endAndSubmitCommandBuffer();
    }

    public static void flipMotionVectorY(ITexture input, ITexture output) {
        if (!isInit)
            init();

        RenderSystems.opengl().device().commandDecoder().beginCommandBuffer();
        ICommandBuffer commandBuffer = RenderSystems.current().device().commandDecoder().currentCommandBuffer();
        pipeline.remove("flip_motion_vector_y");
        pipeline.add("flip_motion_vector_y",
                GrapeJobBuilders.compute(flipMotionVectorYPipeline)
                        .resource("inputMotionVector", GrapeJobResource.SamplerTexture.create(input))
                        .resource("outputMotionVector",
                                GrapeJobResource.StorageTexture.create(output, GrapeResourceAccess.Write))
                        .workGroup(
                                (input.getWidth() + 15) / 16,
                                (input.getHeight() + 15) / 16,
                                1)
                        .build());

        pipeline.execute(commandBuffer, "flip_motion_vector_y");
        RenderSystems.opengl().device().commandDecoder().endAndSubmitCommandBuffer();
    }
}
