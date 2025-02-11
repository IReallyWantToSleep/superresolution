package io.homo.superresolution.fabric.mixin.compat.distanthorizons;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.render.MinecraftRenderingStates;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftRenderWrapper.class)
public class MinecraftRenderWrapperMixin {
    @Inject(method = "getRenderTarget",at =@At(value = "HEAD"), cancellable = true)
    private void getRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
        if (Config.isEnableUpscale()) cir.setReturnValue(MinecraftRenderingStates.getRenderTarget());
    }
}
