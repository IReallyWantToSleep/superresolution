package io.homo.superresolution.forge.mixin.core;

import com.mojang.blaze3d.pipeline.MainTarget;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.forge.SuperResolutionForge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(at = @At(value = "RETURN"), method = "<init>")
    private void onInitDone(CallbackInfo ci) {
        SuperResolution.initRendering();
        SuperResolution.mainTarget = (MainTarget) Minecraft.getInstance().getMainRenderTarget();
    }

    @Inject(at = @At(value = "TAIL"), method = "onGameLoadFinished")
    private void onLoadFinished(CallbackInfo ci) {
        SuperResolution.createAlgo();
        SuperResolutionForge.mod.init();
    }
}
