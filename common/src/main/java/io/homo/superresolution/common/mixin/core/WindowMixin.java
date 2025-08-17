package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.platform.Window;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {
    @Shadow
    private int framebufferWidth;
    @Shadow
    private int framebufferHeight;

    @Inject(at = @At("RETURN"), method = "getScreenWidth", cancellable = true)
    private void getScreenWidth(CallbackInfoReturnable<Integer> ci) {
        if (!SuperResolution.isShaderPackCompatSuperResolution())
            ci.setReturnValue(super_resolution$clampSize((ci.getReturnValue())));
    }

    @Inject(at = @At("RETURN"), method = "getScreenHeight", cancellable = true)
    private void getScreenHeight(CallbackInfoReturnable<Integer> ci) {
        if (!SuperResolution.isShaderPackCompatSuperResolution())
            ci.setReturnValue(super_resolution$clampSize((ci.getReturnValue())));
    }

    @Inject(at = @At("RETURN"), method = "getGuiScaledWidth", cancellable = true)
    private void getGuiScaledWidth(CallbackInfoReturnable<Integer> ci) {
        if (!SuperResolution.isShaderPackCompatSuperResolution())
            ci.setReturnValue(super_resolution$clampSize((ci.getReturnValue())));
    }

    @Inject(at = @At("RETURN"), method = "getGuiScaledHeight", cancellable = true)
    private void getGuiScaledHeight(CallbackInfoReturnable<Integer> ci) {
        if (!SuperResolution.isShaderPackCompatSuperResolution())
            ci.setReturnValue(super_resolution$clampSize((ci.getReturnValue())));
    }


    @Inject(at = @At("RETURN"), method = "getWidth", cancellable = true)
    private void getFramebufferWidth(CallbackInfoReturnable<Integer> ci) {
        if (!SuperResolution.isShaderPackCompatSuperResolution())
            ci.setReturnValue(super_resolution$clampSize(super_resolution$scale(ci.getReturnValue())));
    }

    @Unique
    private int super_resolution$clampSize(int size) {
        return Math.max(size, 1);
    }

    @Inject(at = @At("RETURN"), method = "getHeight", cancellable = true)
    private void getFramebufferHeight(CallbackInfoReturnable<Integer> ci) {
        if (!SuperResolution.isShaderPackCompatSuperResolution())
            ci.setReturnValue(super_resolution$clampSize(super_resolution$scale(ci.getReturnValue())));
    }

    @Unique
    private int super_resolution$scale(int value) {
        double scaleFactor = MinecraftRenderHandle.getCurrentScaleFactor();
        return Math.max(Mth.ceil((double) value * scaleFactor), 1);
    }

    #if MC_VER < MC_1_21_6

    @Inject(at = @At("RETURN"), method = "getGuiScale", cancellable = true)
    private void getScaleFactor(CallbackInfoReturnable<Double> ci) {
        if (!SuperResolution.isShaderPackCompatSuperResolution())
            ci.setReturnValue(ci.getReturnValue() * MinecraftRenderHandle.getCurrentScaleFactor());
    }
    #else
    @Inject(at = @At("RETURN"), method = "getGuiScale", cancellable = true)
    private void getScaleFactor(CallbackInfoReturnable<Integer> ci) {
        if (!SuperResolution.isShaderPackCompatSuperResolution())
            ci.setReturnValue((int) (ci.getReturnValue() * MinecraftRenderHandle.getCurrentScaleFactor()));
    }
    #endif

    @Inject(at = @At("RETURN"), method = "onResize")
    private void onFramebufferSizeChanged(CallbackInfo ci) {
        SuperResolution.framebufferWidth = framebufferWidth;
        SuperResolution.framebufferHeight = framebufferHeight;
        MinecraftRenderHandle.resize();
    }

    @Inject(at = @At("RETURN"), method = "onFramebufferResize")
    private void onUpdateFramebufferSize(CallbackInfo ci) {
        SuperResolution.framebufferWidth = framebufferWidth;
        SuperResolution.framebufferHeight = framebufferHeight;
        MinecraftRenderHandle.resize();
    }

    @Inject(at = @At("RETURN"), method = "onFramebufferResize")
    private void onUpdateFramebuff0erSize(CallbackInfo ci) {
        SuperResolution.framebufferWidth = framebufferWidth;
        SuperResolution.framebufferHeight = framebufferHeight;
        MinecraftRenderHandle.resize();
    }
}