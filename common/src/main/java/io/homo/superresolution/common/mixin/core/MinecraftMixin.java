package io.homo.superresolution.common.mixin.core;

import dev.architectury.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.render.renderdoc.RenderDoc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(at = @At(value = "RETURN"), method = "onGameLoadFinished")
    private void onLoadDone(CallbackInfo ci) {
        SuperResolution.check();
        SuperResolution.gameIsLoad = true;
    }


    @Inject(at = @At(value = "HEAD"), method = "destroy")
    private void onExit(CallbackInfo ci) {
        SuperResolution.getInstance().destroy();
    }

    //@Inject(at = @At(value = "HEAD"), method = "getMainRenderTarget", cancellable = true)
    //private void replaceMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
    //    if (Minecraft.getInstance().level == null) return;
    //    if (SuperResolution.isRenderingWorld && Config.isEnableUpscale()) {
    //        cir.setReturnValue(MinecraftRenderingStates.getRenderTarget());
    //    } else {
    //        cir.setReturnValue(MinecraftRenderingStates.getOriginRenderTarget());
    //    }
    //}
}
