package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.debug.DebugInfo;
import io.homo.superresolution.common.render.MinecraftRenderingStates;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

#if MC_VER > MC_1_20_1
import net.minecraft.client.DeltaTracker;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

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
    #if MC_VER > MC_1_20_1
    private void onRenderStart(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci)
    #else
    private void onRenderStart(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci)
    #endif {
        SuperResolution.setFrameTimeDelta(16.6f);
        DebugInfo.setFrameTimeDelta(16.6f);
    }
}
