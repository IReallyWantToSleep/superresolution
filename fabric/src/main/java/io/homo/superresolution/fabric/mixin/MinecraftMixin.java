package io.homo.superresolution.fabric.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.fabric.SuperResolutionFabric;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(at= @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setOverlay(Lnet/minecraft/client/gui/screens/Overlay;)V"),method = "<init>")
    private void onStart(CallbackInfo ci){
        RenderSystem.assertOnRenderThread();
        SuperResolutionFabric.mod.init();
    }
}
