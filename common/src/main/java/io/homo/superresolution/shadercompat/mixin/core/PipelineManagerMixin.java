package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.shadercompat.IrisShaderCompatUpscaleDispatcher;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PipelineManager.class,remap = false)
public class PipelineManagerMixin {
    @Inject(method = "preparePipeline",at = @At(value = "INVOKE", target = "Lnet/irisshaders/iris/shaderpack/materialmap/WorldRenderingSettings;clearReloadRequired()V"))
    private void reset(NamespacedId currentDimension, CallbackInfoReturnable<WorldRenderingPipeline> cir){
        IrisShaderCompatUpscaleDispatcher.reset();
        SuperResolution.recreateAlgorithm();
    }
}
