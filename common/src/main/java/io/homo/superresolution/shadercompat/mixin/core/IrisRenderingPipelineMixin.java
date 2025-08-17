package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IrisRenderingPipeline.class)
public class IrisRenderingPipelineMixin {
    @Inject(method = "beginLevelRendering", at = @At("HEAD"), remap = false)
    private void beginLevelRendering(CallbackInfo ci) {
        AlgorithmManager.setMatrixVanilla(
                (Matrix4f) CapturedRenderingState.INSTANCE.getGbufferProjection(),
                (Matrix4f) CapturedRenderingState.INSTANCE.getGbufferModelView()
        );
    }
}
