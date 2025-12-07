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

package io.homo.superresolution.shadercompat.mixin.core;

import net.irisshaders.iris.pipeline.CompositeRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CompositeRenderer.class)
public class CompositeRendererMixin {
    /*
    @Shadow(remap = false)
    @Final
    private ImmutableList<Object> passes;

    #if MC_VER < MC_1_21_1
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void superresolution$assignPassNames(
            net.irisshaders.iris.pipeline.WorldRenderingPipeline pipeline,
            net.irisshaders.iris.shaderpack.properties.PackDirectives packDirectives,
            net.irisshaders.iris.shaderpack.programs.ProgramSource[] sources,
            net.irisshaders.iris.shaderpack.programs.ComputeSource[][] computes,
            net.irisshaders.iris.targets.RenderTargets renderTargets,
            net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder holder,
            net.irisshaders.iris.gl.texture.TextureAccess noiseTexture,
            net.irisshaders.iris.uniforms.FrameUpdateNotifier updateNotifier,
            net.irisshaders.iris.pathways.CenterDepthSampler centerDepthSampler,
            net.irisshaders.iris.targets.BufferFlipper bufferFlipper,
            java.util.function.Supplier<?> shadowTargetsSupplier,
            net.irisshaders.iris.shaderpack.texture.TextureStage textureStage,
            it.unimi.dsi.fastutil.objects.Object2ObjectMap<String, net.irisshaders.iris.gl.texture.TextureAccess> customTextureIds,
            it.unimi.dsi.fastutil.objects.Object2ObjectMap<String, net.irisshaders.iris.gl.texture.TextureAccess> irisCustomTextures,
            java.util.Set<net.irisshaders.iris.gl.image.GlImage> customImages,
            com.google.common.collect.ImmutableMap<Integer, Boolean> explicitPreFlips,
            net.irisshaders.iris.uniforms.custom.CustomUniforms customUniforms,
            CallbackInfo ci
    ) {
        CompositeRenderer self = (CompositeRenderer) (Object) this;
        java.util.List<?> passes = IrisShaderPipelineHandle.getCompositeRendererPasses(self);

        for (int i = 0; i < sources.length; ++i) {
            ProgramSource source = sources[i];
            if (source != null && source.isValid()) {
                if (i < passes.size()) {
                    ((NamedCompositePass) passes.get(i)).superresolution$setName(source.getName());
                    continue;
                }
            } else if (computes[i] != null && computes[i][0] != null) {
                if (i < passes.size()) {
                    ((NamedCompositePass) passes.get(i)).superresolution$setName(computes[i][0].getName());
                    continue;
                }
            }
            if (i < passes.size()) {
                ((NamedCompositePass) passes.get(i)).superresolution$setName("composite%s".formatted(i));
            }
        }

    }
    #endif

    #if MC_VER < MC_1_21_1
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/blending/BlendModeOverride;restore()V",
            shift = At.Shift.BY
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false
    )
    #else
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/GLDebug;popGroup()V",
            ordinal = 1,
            shift = At.Shift.BY
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false
    )
    #endif

    #if MC_VER < MC_1_21_1
    private void dispatchUpscale(
            CallbackInfo ci,
            RenderTarget main,
            com.google.common.collect.UnmodifiableIterator<?> var2
    ) {
        IrisShaderPipelineHandle.onCompositeRendererRender(
                (CompositeRenderer) (Object) this,
                (NamedCompositePass) this.passes.get(
                        Math.max(((ListIterator<?>) var2).previousIndex(), 0)
                )
        );
    }
    #else
    #if MC_VER > MC_1_21_4
    public void dispatchUpscale(
            CallbackInfo ci,
            RenderTarget main,
            com.mojang.blaze3d.buffers.GpuBuffer indices,
            com.mojang.blaze3d.vertex.VertexFormat.IndexType type,
            com.mojang.blaze3d.systems.RenderPass renderPass,
            int passIndex,
            int passesSize
    )
    #else
    public void dispatchUpscale(CallbackInfo ci, RenderTarget main, int passIndex)
    #endif {
        IrisShaderPipelineHandle.onCompositeRendererRender(
                (CompositeRenderer) (Object) this,
                (NamedCompositePass) this.passes.get(passIndex)

        );
    }
    #endif
    */
}
