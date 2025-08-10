package io.homo.superresolution.shadercompat.mixin.core;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.shadercompat.ShaderCompatUpscaleDispatcher;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ListIterator;

@Mixin(CompositeRenderer.class)
public class CompositeRendererMixin {
    #if MC_VER == MC_1_20_1
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

    #if MC_VER == MC_1_20_1
    private void dispatchUpscale(
            CallbackInfo ci,
            RenderTarget main,
            com.google.common.collect.UnmodifiableIterator<?> var2
    ) {
        if (!MinecraftRenderHandle.isShaderPackCompat()) return;
        if (Iris.getPipelineManager().getPipeline().isPresent()) {
            if (this.equals(((IrisRenderingPipelineAccessor) Iris.getPipelineManager().getPipeline().get()).getCompositeRenderer())) {
                if (ShaderCompatUpscaleDispatcher.getCurrentShaderPackConfig().isPresent()) {
                    if (ShaderCompatUpscaleDispatcher.getCurrentConfig() != null && ShaderCompatUpscaleDispatcher.getCurrentConfig().enabled) {
                        String indexStr = ShaderCompatUpscaleDispatcher.getCurrentConfig().before_upscale_shader_name.replace("composite", "");
                        int index = 0;
                        if (!(indexStr.isBlank())) {
                            index = Integer.parseInt(indexStr);
                        }
                        if (index == ((java.util.ListIterator<?>) var2).nextIndex()) {
                            ShaderCompatUpscaleDispatcher.dispatchUpscale(((CompositeRenderer) (Object) this));
                        }
                    }
                }
            }
        }
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
        if (!MinecraftRenderHandle.isShaderPackCompat()) return;
        if (Iris.getPipelineManager().getPipeline().isPresent()) {
            if (this.equals(((IrisRenderingPipelineAccessor) Iris.getPipelineManager().getPipeline().get()).getCompositeRenderer())) {
                if (ShaderCompatUpscaleDispatcher.getCurrentShaderPackConfig().isPresent()) {
                    if (ShaderCompatUpscaleDispatcher.getCurrentConfig() != null && ShaderCompatUpscaleDispatcher.getCurrentConfig().enabled) {
                        String indexStr = ShaderCompatUpscaleDispatcher.getCurrentConfig().upscale_config.before_upscale_shader_name.replace("composite", "");
                        int index = 0;
                        if (!(indexStr.isBlank())) {
                            index = Integer.parseInt(indexStr);
                        }
                        if (index == passIndex) {
                            ShaderCompatUpscaleDispatcher.dispatchUpscale(((CompositeRenderer) (Object) this));
                        }
                    }
                }
            }
        }
    }
    #endif
}
