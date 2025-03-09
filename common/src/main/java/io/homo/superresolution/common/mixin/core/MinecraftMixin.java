package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

}
