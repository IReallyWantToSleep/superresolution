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

package io.homo.superresolution.common.upscale.anime4k;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.QualityPreset;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.MemoryBarrierType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.sampler.ISampler;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerDescription;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderResourceAccess;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.impl.framebuffer.FramebufferDescription;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlComputePipeline;
import net.minecraft.network.chat.Component;
import org.joml.Vector3i;

import java.util.*;

public class Anime4K extends AbstractAlgorithm {
    private static final String SHADER_DIR = "/shader/anime4k/";
    private static final String MODEL_PATH = SHADER_DIR + "model.json";

    private IFrameBuffer outputFrameBuffer;
    private ITexture outputColorTexture;

    private ISampler samplerNearest;
    private ISampler samplerLinear;

    private Anime4KModel model;

    private final Map<String, ITexture> intermediateTextures = new LinkedHashMap<>();

    private final List<IShaderProgram> shaders = new ArrayList<>();
    private final List<ComputePipeline> pipelines = new ArrayList<>();

    private record PassBinding(
            ComputePipeline pipeline,
            List<SamplerBinding> samplers,
            ImageBinding image,
            String outputTextureName
    ) {}

    private record SamplerBinding(String name, String source, String filter) {}
    private record ImageBinding(String name, String source) {}

    private final List<PassBinding> passBindings = new ArrayList<>();

    @Override
    public void initialize(InitializationDescription desc) {
        this.initDesc = desc;

        model = Anime4KModel.load(MODEL_PATH);

        int screenW = RenderHandlerManager.getScreenWidth();
        int screenH = RenderHandlerManager.getScreenHeight();

        samplerNearest = RenderSystems.current().device().createSampler(
                SamplerDescription.create()
                        .minFilter(TextureFilterMode.Nearest)
                        .magFilter(TextureFilterMode.Nearest)
                        .wrapMode(TextureWrapMode.ClampToEdge)
                        .build());
        samplerLinear = RenderSystems.current().device().createSampler(
                SamplerDescription.create()
                        .minFilter(TextureFilterMode.Linear)
                        .magFilter(TextureFilterMode.Linear)
                        .wrapMode(TextureWrapMode.ClampToEdge)
                        .build());

        for (Anime4KModel.TextureDesc td : model.intermediateTextures) {
            int tw = Math.max(1, Math.round(screenW * td.scaleX()));
            int th = Math.max(1, Math.round(screenH * td.scaleY()));

            TextureFormat format = parseFormat(td.format);
            boolean isOutput = td.name.equals(model.outputTexture);
            TextureUsages usages = TextureUsages.create().storage().sampler();
            if (isOutput) {
                usages.attachmentColor();
            }
            ITexture tex = RenderSystems.current().device().createTexture(
                    TextureDescription.create()
                            .type(TextureType.Texture2D)
                            .filterMode(TextureFilterMode.Nearest)
                            .format(format)
                            .size(tw, th)
                            .mipmapsDisabled()
                            .usages(usages)
                            .wrapMode(TextureWrapMode.ClampToEdge)
                            .label("SRAnime4k-" + td.name)
                            .build()
            );
            intermediateTextures.put(td.name, tex);
        }

        outputColorTexture = intermediateTextures.get(model.outputTexture);
        outputFrameBuffer = RenderSystems.current().device().createFramebuffer(
                FramebufferDescription.create()
                        .colorAttachment(outputColorTexture)
                        .build());

        passBindings.clear();
        for (Anime4KModel.PassDesc pass : model.passes) {
            ShaderDescription.Builder shaderBuilder = ShaderDescription
                    .compute(ShaderSource.file(ShaderType.Compute, SHADER_DIR + pass.file))
                    .name("Anime4K_" + pass.function);

            for (Anime4KModel.SamplerDesc sampler : pass.samplers) {
                shaderBuilder.uniformSamplerTexture(sampler.name, sampler.binding);
            }

            shaderBuilder.uniformStorageTexture(
                    pass.image.name,
                    ShaderResourceAccess.Write,
                    pass.image.binding
            );

            IShaderProgram shader = RenderSystems.current().device()
                    .createShaderProgram(shaderBuilder.build());
            shader.compile();
            shaders.add(shader);

            ComputePipeline pipeline = RenderSystems.current().device().createComputePipeline(ComputePipeline.builder().shader(shader));
            pipelines.add(pipeline);

            List<SamplerBinding> samplerBindings = new ArrayList<>();
            for (Anime4KModel.SamplerDesc sampler : pass.samplers) {
                samplerBindings.add(new SamplerBinding(sampler.name, sampler.source, sampler.filter));
            }
            ImageBinding imageBinding = new ImageBinding(pass.image.name, pass.image.source);

            passBindings.add(new PassBinding(pipeline, samplerBindings, imageBinding, pass.image.source));
        }
    }

    private ITexture resolveTexture(String name) {
        if ("inputColor".equals(name)) {
            return null;
        }
        return intermediateTextures.get(name);
    }

    private static TextureFormat parseFormat(String formatStr) {
        return switch (formatStr.toLowerCase(Locale.ROOT)) {
            case "rgba32f" -> TextureFormat.RGBA32F;
            case "rgba16f" -> TextureFormat.RGBA16F;
            case "rgba8" -> TextureFormat.RGBA8;
            case "r32f" -> TextureFormat.R32F;
            case "rg16f" -> TextureFormat.RG16F;
            default -> TextureFormat.RGBA32F;
        };
    }

    @Override
    public boolean dispatch(DispatchResource dispatchResource) {
        super.dispatch(dispatchResource);

        ICommandBuffer commandBuffer = RenderSystems.current().device()
                .defaultCommandPool().createCommandBuffer();
        for (PassBinding pass : passBindings) {
            for (SamplerBinding sb : pass.samplers) {
                ITexture tex = resolveTextureForDispatch(sb.source);
                if (tex != null) {
                    ISampler sampler = "LINEAR".equalsIgnoreCase(sb.filter) ? samplerLinear : samplerNearest;
                    pass.pipeline.descriptorSet().samplerTexture(sb.name, tex, sampler);
                }
            }
            ITexture imageTex = resolveTexture(pass.image.source);
            if (imageTex != null) {
                pass.pipeline.descriptorSet().storageImage(pass.image.name, imageTex);
            }
            pass.pipeline.descriptorSet().update();
        }
        commandBuffer.begin();

        for (PassBinding pass : passBindings) {
            Vector3i wg = computeWorkGroup(pass.outputTextureName);
            RenderSystems.current().device().commandDecoder().memoryBarrier(
                    commandBuffer,
                    MemoryBarrierType.ALL);
            RenderSystems.current().device().commandDecoder().dispatch(
                    commandBuffer, pass.pipeline, wg.x, wg.y, wg.z);
            RenderSystems.current().device().commandDecoder().memoryBarrier(
                    commandBuffer,
                    MemoryBarrierType.ALL);
        }

        commandBuffer.end();
        RenderSystems.current().device().submitCommandBuffer(commandBuffer);

        return true;
    }

    private ITexture resolveTextureForDispatch(String source) {
        if ("inputColor".equals(source)) {
            return getResources() != null ? getResources().colorTexture() : null;
        }
        return resolveTexture(source);
    }

    private Vector3i computeWorkGroup(String outputTextureName) {
        ITexture outTex = resolveTexture(outputTextureName);
        int outW, outH;
        if (outTex != null) {
            outW = outTex.getWidth();
            outH = outTex.getHeight();
        } else {
            outW = RenderHandlerManager.getScreenWidth();
            outH = RenderHandlerManager.getScreenHeight();
        }
        int wgX = model.workgroupSize[0];
        int wgY = model.workgroupSize[1];
        return new Vector3i(
                (outW + wgX - 1) / wgX,
                (outH + wgY - 1) / wgY,
                1
        );
    }

    @Override
    public void destroy() {
        if (outputFrameBuffer != null) {
            outputFrameBuffer.destroy();
            outputFrameBuffer = null;
        }
        outputColorTexture = null;

        for (ITexture tex : intermediateTextures.values()) {
            tex.destroy();
        }
        intermediateTextures.clear();

        for (IShaderProgram shader : shaders) {
            shader.destroy();
        }
        shaders.clear();
        pipelines.clear();
        passBindings.clear();

        if (samplerNearest != null) {
            samplerNearest.destroy();
            samplerNearest = null;
        }
        if (samplerLinear != null) {
            samplerLinear.destroy();
            samplerLinear = null;
        }
    }

    @Override
    public void resize(int width, int height) {
        destroy();
        initialize(initDesc);
    }

    @Override
    public IFrameBuffer getOutputFrameBuffer() {
        return outputFrameBuffer;
    }

    @Override
    public int getOutputTextureId() {
        return Math.toIntExact(outputColorTexture.handle());
    }

    @Override
    public boolean isSupportJitter() {
        return false;
    }

    @Override
    public List<QualityPreset> getQualityPresets() {
        return List.of(
                new QualityPreset()
                        .setUpscaleRatio(2.0f)
                        .setName(Component.literal("2x"))
                        .setCodeName("anime4k_2x")
        );
    }

    @Override
    public boolean isCustomUpscaleRatio() {
        return false;
    }
}
