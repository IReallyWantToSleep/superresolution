package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.platform.Window;
import io.homo.superresolution.common.render.MinecraftRenderingStates;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(at = @At("RETURN"), method = "getWidth", cancellable = true)
    private void getFramebufferWidth(CallbackInfoReturnable<Integer> ci) {
        ci.setReturnValue(super_resolution$scale(ci.getReturnValueI()));
    }

    @Inject(at = @At("RETURN"), method = "getHeight", cancellable = true)
    private void getFramebufferHeight(CallbackInfoReturnable<Integer> ci) {
        ci.setReturnValue(super_resolution$scale(ci.getReturnValueI()));
    }

    @Unique
    private int super_resolution$scale(int value) {
        double scaleFactor = MinecraftRenderingStates.getCurrentScaleFactor();
        return Math.max(Mth.ceil((double) value * scaleFactor), 1);
    }

    @Inject(at = @At("RETURN"), method = "getGuiScale", cancellable = true)
    private void getScaleFactor(CallbackInfoReturnable<Double> ci) {
        ci.setReturnValue(ci.getReturnValueD() * MinecraftRenderingStates.getCurrentScaleFactor());
    }

    @Inject(at = @At("RETURN"), method = "onResize")
    private void onFramebufferSizeChanged(CallbackInfo ci) {
        MinecraftRenderingStates.onResolutionChanged();
    }

    @Inject(at = @At("RETURN"), method = "onFramebufferResize")
    private void onUpdateFramebufferSize(CallbackInfo ci) {
        MinecraftRenderingStates.onResolutionChanged();
    }
}