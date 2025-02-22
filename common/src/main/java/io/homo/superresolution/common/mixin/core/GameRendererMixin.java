package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.debug.DebugInfo;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

#if MC_VER > MC_1_20_1
import net.minecraft.client.DeltaTracker;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @Inject(method = "resize", at = @At(value = "HEAD"))
    private void onResize(int i, int j, CallbackInfo ci) {
        if (SuperResolution.isInit && SuperResolution.gameIsLoad) {
            SuperResolution.getInstance().resize(i, j);
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

    @Inject(at = @At(value = "HEAD"), method = "renderLevel")
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldBegin();
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldEnd();
        }
    }
}
