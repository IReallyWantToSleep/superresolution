package io.homo.superresolution.shadercompat.mixin.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.shadercompat.IrisShaderPipelineHandle;
import io.homo.superresolution.shadercompat.NamedCompositePass;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.program.Program;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Arrays;
import java.util.ListIterator;
import java.util.function.Supplier;

@Mixin(CompositeRenderer.class)
public class CompositeRendererMixin {
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
}
