package io.homo.superresolution.forge.mixin;

import io.homo.superresolution.forge.SuperResolutionForge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(at= @At(value = "HEAD"),method = "onGameLoadFinished")
    private void onStart(CallbackInfo ci){
        SuperResolutionForge.mod.init();
    }
}
