package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.debug.DebugInfo;
import io.homo.superresolution.common.render.MinecraftRenderingStates;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    Minecraft minecraft;
    @Unique
    private boolean super_resolution$shouldResize = true;

    @Inject(method = "resize", at = @At(value = "HEAD"))
    private void onResize(int i, int j, CallbackInfo ci) {
        if (SuperResolution.isInit && SuperResolution.gameIsLoad) {
            SuperResolution.getInstance().resize(i, j);
            MinecraftRenderingStates.onResolutionChanged();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "render")
    private void onRenderStart(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        SuperResolution.setFrameTimeDelta(16.6f);
        DebugInfo.setFrameTimeDelta(16.6f);
        if (renderLevel && this.minecraft.level != null) {
            if (super_resolution$shouldResize) {
                super_resolution$shouldResize = false;
                Minecraft.getInstance().resizeDisplay();
            }
        } else {
            super_resolution$shouldResize = true;
        }
    }
}
