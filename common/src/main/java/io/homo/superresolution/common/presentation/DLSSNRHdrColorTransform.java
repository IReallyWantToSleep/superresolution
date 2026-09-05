/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.homo.superresolution.common.presentation;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;
import io.homo.superresolution.core.graphics.opengl.texture.GlSampler;
import org.lwjgl.opengl.GL43;

import java.util.HashMap;
import java.util.Map;

/** Keeps the proprietary DLSS NR network in the reversible AgX Log color domain. */
final class DLSSNRHdrColorTransform {
    private static final Map<String, GlComputePipeline> PIPELINES = new HashMap<>();
    private static GlSampler sampler;

    private DLSSNRHdrColorTransform() {
    }

    static void compress(ITexture source, ITexture destination) {
        dispatch(source, null, destination, "COMPRESS_HDR", 1.0f);
    }

    static void finish(
            ITexture original,
            ITexture neural,
            ITexture destination,
            boolean hdr,
            float colorStrength
    ) {
        dispatch(neural, original, destination, hdr ? "FINISH_HDR" : "FINISH_SDR", colorStrength);
    }

    private static void dispatch(
            ITexture source,
            ITexture original,
            ITexture destination,
            String operation,
            float colorStrength
    ) {
        if (source.getWidth() != destination.getWidth() || source.getHeight() != destination.getHeight()
                || (original != null && (original.getWidth() != destination.getWidth()
                || original.getHeight() != destination.getHeight()))) {
            throw new IllegalArgumentException("DLSS NR HDR transform requires equally sized textures");
        }
        String format = destination.getTextureFormat().getGlslFormatQualifier();
        if (format == null) {
            throw new IllegalArgumentException("Unsupported DLSS NR HDR output format: " + destination.getTextureFormat());
        }
        GlComputePipeline pipeline = PIPELINES.computeIfAbsent(operation + ':' + format,
                ignored -> createPipeline(operation, format));
        if (sampler == null) {
            sampler = GlSampler.create(GlSampler.SamplerType.NearestClamp);
        }
        pipeline.descriptorSet().samplerTexture("sourceColor", source, sampler);
        if (original != null) {
            pipeline.descriptorSet().samplerTexture("originalColor", original, sampler);
            int location = GL43.glGetUniformLocation((int) pipeline.shader().handle(), "colorStrength");
            if (location >= 0) {
                GL43.glProgramUniform1f(
                        (int) pipeline.shader().handle(),
                        location,
                        Math.max(0.0f, Math.min(2.0f, colorStrength))
                );
            }
        }
        pipeline.descriptorSet().storageImage("destinationColor", destination);
        pipeline.descriptorSet().update();
        ICommandBuffer commandBuffer = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
        commandBuffer.begin();
        commandBuffer.bindPipeline(pipeline);
        commandBuffer.dispatch((destination.getWidth() + 15) / 16, (destination.getHeight() + 15) / 16, 1);
        commandBuffer.end();
        RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
    }

    private static GlComputePipeline createPipeline(String operation, String format) {
        ShaderDescription.Builder builder = ShaderDescription.compute(
                        new ShaderSource(ShaderType.Compute, "/shader/dlssnr_hdr_color.comp.glsl", true))
                .name("dlssnr_hdr_" + operation.toLowerCase())
                .uniformSamplerTexture("sourceColor", 0)
                .uniformStorageTexture("destinationColor", ShaderResourceAccess.Write, 0);
        if (operation.startsWith("FINISH_")) {
            builder.uniformSamplerTexture("originalColor", 1);
        }
        builder.addDefine("OUTPUT_FORMAT", format);
        builder.addDefine(operation, "1");
        GlShaderProgram program = RenderSystems.opengl().device().createShaderProgram(builder.build());
        program.compile();
        return (GlComputePipeline) GlComputePipeline.builder().shader(program).build(RenderSystems.opengl().device());
    }
}
