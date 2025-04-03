package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderTarget.class)
public abstract class RenderTargetMixin {
    @Unique
    private float super_resolution$scaleMultiplier;


    @Inject(method = "createBuffers", at = @At("HEAD"))
    #if MC_VER > MC_1_21_1
    private void onInitFbo(int width, int height, CallbackInfo ci)
    #else
    private void onInitFbo(int width, int height, boolean getError, CallbackInfo ci)
    #endif {
        super_resolution$scaleMultiplier = (float) width / Minecraft.getInstance().getWindow().getWidth();
    }
}
