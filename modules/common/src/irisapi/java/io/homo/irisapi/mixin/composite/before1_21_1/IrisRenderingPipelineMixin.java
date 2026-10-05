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

package io.homo.irisapi.mixin.composite.before1_21_1;

import com.google.common.collect.ImmutableMap;
import io.homo.irisapi.pipeline.mc1201.CompositePass;
import io.homo.irisapi.pipeline.mc1201.CompositeRendererWrapper;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import java.util.function.Supplier;

@Mixin(value = IrisRenderingPipeline.class,remap = false)
public class IrisRenderingPipelineMixin {
    #if MC_VER < MC_1_21_1
    //begin
    @Redirect(
            method = "<init>",
            at= @At(value = "NEW", target = "net/irisshaders/iris/pipeline/CompositeRenderer", ordinal = 0)
    )
    private CompositeRenderer redirectBeginCompositeRenderer(
            WorldRenderingPipeline pipeline,
            PackDirectives packDirectives,
            ProgramSource[] sources,
            ComputeSource[][] computes,
            RenderTargets renderTargets,
            ShaderStorageBufferHolder holder,
            TextureAccess noiseTexture,
            FrameUpdateNotifier updateNotifier,
            CenterDepthSampler centerDepthSampler,
            BufferFlipper bufferFlipper,
            Supplier<ShadowRenderTargets> shadowTargetsSupplier,
            TextureStage textureStage,
            Object2ObjectMap<String, TextureAccess> customTextureIds,
            Object2ObjectMap<String, TextureAccess> irisCustomTextures,
            Set<GlImage> customImages,
            ImmutableMap<Integer, Boolean> explicitPreFlips,
            CustomUniforms customUniforms
    ){

        return new CompositeRendererWrapper(
                pipeline,
                CompositePass.BEGIN,
                packDirectives,
                sources,
                computes,
                renderTargets,
                holder,
                noiseTexture,
                updateNotifier,
                centerDepthSampler,
                bufferFlipper,
                shadowTargetsSupplier,
                textureStage,
                customTextureIds,
                irisCustomTextures,
                customImages,
                explicitPreFlips,
                customUniforms
        );
    }

    //prepare
    @Redirect(
            method = "<init>",
            at= @At(value = "NEW", target = "net/irisshaders/iris/pipeline/CompositeRenderer", ordinal = 1)
    )
    private CompositeRenderer redirectPrepareCompositeRenderer(
            WorldRenderingPipeline pipeline,
            PackDirectives packDirectives,
            ProgramSource[] sources,
            ComputeSource[][] computes,
            RenderTargets renderTargets,
            ShaderStorageBufferHolder holder,
            TextureAccess noiseTexture,
            FrameUpdateNotifier updateNotifier,
            CenterDepthSampler centerDepthSampler,
            BufferFlipper bufferFlipper,
            Supplier<ShadowRenderTargets> shadowTargetsSupplier,
            TextureStage textureStage,
            Object2ObjectMap<String, TextureAccess> customTextureIds,
            Object2ObjectMap<String, TextureAccess> irisCustomTextures,
            Set<GlImage> customImages,
            ImmutableMap<Integer, Boolean> explicitPreFlips,
            CustomUniforms customUniforms
    ){

        return new CompositeRendererWrapper(
                pipeline,
                CompositePass.PREPARE,
                packDirectives,
                sources,
                computes,
                renderTargets,
                holder,
                noiseTexture,
                updateNotifier,
                centerDepthSampler,
                bufferFlipper,
                shadowTargetsSupplier,
                textureStage,
                customTextureIds,
                irisCustomTextures,
                customImages,
                explicitPreFlips,
                customUniforms
        );
    }

    //deferred
    @Redirect(
            method = "<init>",
            at= @At(value = "NEW", target = "net/irisshaders/iris/pipeline/CompositeRenderer", ordinal = 2)
    )
    private CompositeRenderer redirectDeferredCompositeRenderer(
            WorldRenderingPipeline pipeline,
            PackDirectives packDirectives,
            ProgramSource[] sources,
            ComputeSource[][] computes,
            RenderTargets renderTargets,
            ShaderStorageBufferHolder holder,
            TextureAccess noiseTexture,
            FrameUpdateNotifier updateNotifier,
            CenterDepthSampler centerDepthSampler,
            BufferFlipper bufferFlipper,
            Supplier<ShadowRenderTargets> shadowTargetsSupplier,
            TextureStage textureStage,
            Object2ObjectMap<String, TextureAccess> customTextureIds,
            Object2ObjectMap<String, TextureAccess> irisCustomTextures,
            Set<GlImage> customImages,
            ImmutableMap<Integer, Boolean> explicitPreFlips,
            CustomUniforms customUniforms
    ){

        return new CompositeRendererWrapper(
                pipeline,
                CompositePass.DEFERRED,
                packDirectives,
                sources,
                computes,
                renderTargets,
                holder,
                noiseTexture,
                updateNotifier,
                centerDepthSampler,
                bufferFlipper,
                shadowTargetsSupplier,
                textureStage,
                customTextureIds,
                irisCustomTextures,
                customImages,
                explicitPreFlips,
                customUniforms
        );
    }

    //composite
    @Redirect(
            method = "<init>",
            at= @At(value = "NEW", target = "net/irisshaders/iris/pipeline/CompositeRenderer", ordinal = 3)
    )
    private CompositeRenderer redirectCompositeCompositeRenderer(
            WorldRenderingPipeline pipeline,
            PackDirectives packDirectives,
            ProgramSource[] sources,
            ComputeSource[][] computes,
            RenderTargets renderTargets,
            ShaderStorageBufferHolder holder,
            TextureAccess noiseTexture,
            FrameUpdateNotifier updateNotifier,
            CenterDepthSampler centerDepthSampler,
            BufferFlipper bufferFlipper,
            Supplier<ShadowRenderTargets> shadowTargetsSupplier,
            TextureStage textureStage,
            Object2ObjectMap<String, TextureAccess> customTextureIds,
            Object2ObjectMap<String, TextureAccess> irisCustomTextures,
            Set<GlImage> customImages,
            ImmutableMap<Integer, Boolean> explicitPreFlips,
            CustomUniforms customUniforms
    ){

        return new CompositeRendererWrapper(
                pipeline,
                CompositePass.COMPOSITE,
                packDirectives,
                sources,
                computes,
                renderTargets,
                holder,
                noiseTexture,
                updateNotifier,
                centerDepthSampler,
                bufferFlipper,
                shadowTargetsSupplier,
                textureStage,
                customTextureIds,
                irisCustomTextures,
                customImages,
                explicitPreFlips,
                customUniforms
        );
    }
    #endif
}
