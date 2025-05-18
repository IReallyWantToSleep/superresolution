package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.core.impl.framebuffer.FrameBufferBindPoint;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
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

    @Inject(at = @At(value = "HEAD"), method = "runTick")
    private void onRenderBegin(CallbackInfo ci) {
        #if MC_VER > MC_1_21_4
        GL11.glViewport(0, 0, MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
        #endif
        PerformanceInfo.begin("runTick");
    }

    @Inject(at = @At(value = "RETURN"), method = "runTick")
    private void onRenderEnd(CallbackInfo ci) {
        PerformanceInfo.end("runTick");
    }

    @Inject(at = @At(value = "HEAD"), method = "destroy")
    private void onExit(CallbackInfo ci) {
        SuperResolution.getInstance().destroy();
    }

}
