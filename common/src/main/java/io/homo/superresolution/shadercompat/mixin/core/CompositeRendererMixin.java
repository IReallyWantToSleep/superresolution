package io.homo.superresolution.shadercompat.mixin.core;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.shadercompat.ShaderCompatUpscaleDispatcher;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CompositeRenderer.class)
public class CompositeRendererMixin {
    @Inject(method = "renderAll", at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/GLDebug;popGroup()V",
            ordinal = 1,
            shift = At.Shift.BY
    ), locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    public void dispatchUpscale(CallbackInfo ci, RenderTarget main, int passIndex) {
        if (!MinecraftRenderHandle.isShaderPackCompat()) return;
        if (Iris.getPipelineManager().getPipeline().isPresent()) {
            if (this.equals(((IrisRenderingPipelineAccessor) Iris.getPipelineManager().getPipeline().get()).getCompositeRenderer())) {
                if (ShaderCompatUpscaleDispatcher.getCurrentShaderPackConfig().isPresent()) {
                    if (ShaderCompatUpscaleDispatcher.getCurrentConfig() != null && ShaderCompatUpscaleDispatcher.getCurrentConfig().enabled) {
                        String indexStr = ShaderCompatUpscaleDispatcher.getCurrentConfig().beforeUpscaleShaderName.replace("composite", "");
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
}
