package io.homo.superresolution.fabric.mixin.core;

import com.mojang.blaze3d.pipeline.MainTarget;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.fabric.SuperResolutionFabricClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"), method = "<init>")
    private void onStart(CallbackInfo ci) {
        if (!SuperResolution.isPreInit) return;
        SuperResolution.initRendering();
        SuperResolution.createAlgo();
        SuperResolutionFabricClient.mod.init();
    }
}
