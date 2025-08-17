package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.debug.PerformanceInfo;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique
    private int super_resolution$cacheWidth = 0;
    @Unique
    private int super_resolution$cacheHeight = 0;

    @Inject(at = @At(value = "RETURN"), method = "onGameLoadFinished")
    private void onLoadDone(CallbackInfo ci) {
        SuperResolution.check();
        SuperResolution.gameIsLoad = true;
    }

    @Inject(at = @At(value = "HEAD"), method = "runTick")
    private void onRenderBegin(CallbackInfo ci) {
        if (super_resolution$cacheWidth != MinecraftRenderHandle.getScreenWidth() || super_resolution$cacheHeight != MinecraftRenderHandle.getScreenHeight()) {
            super_resolution$cacheWidth = MinecraftRenderHandle.getScreenWidth();
            super_resolution$cacheHeight = MinecraftRenderHandle.getScreenHeight();
            Minecraft.getInstance().resizeDisplay();
        }
        org.lwjgl.opengl.GL11.glViewport(0, 0, MinecraftRenderHandle.getScreenWidth(), MinecraftRenderHandle.getScreenHeight());
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

    @Inject(method = "resizeDisplay", at = @At(value = "HEAD"), cancellable = true)
    private void onResize(CallbackInfo ci) {
        if (
                io.homo.superresolution.common.minecraft.MinecraftWindow.getWindowSourceWidth() <= 1 ||
                        io.homo.superresolution.common.minecraft.MinecraftWindow.getWindowSourceHeight() <= 1
        ) {
            ci.cancel();
        }
    }
}
