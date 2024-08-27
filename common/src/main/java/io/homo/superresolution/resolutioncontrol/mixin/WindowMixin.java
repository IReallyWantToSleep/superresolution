package io.homo.superresolution.resolutioncontrol.mixin;

import com.mojang.blaze3d.platform.Window;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Window.class)
public abstract class WindowMixin {
	@Inject(at = @At("RETURN"), method = "getWidth", cancellable = true)
	private void getFramebufferWidth(CallbackInfoReturnable<Integer> ci) {
		if (ResolutionControl.isInit()){
			ci.setReturnValue(scale(ci.getReturnValueI()));
		}
	}
	
	@Inject(at = @At("RETURN"), method = "getHeight", cancellable = true)
	private void getFramebufferHeight(CallbackInfoReturnable<Integer> ci) {
		if (ResolutionControl.isInit()) {
			ci.setReturnValue(scale(ci.getReturnValueI()));
		}
	}
	
	private int scale(int value) {
		if(!ResolutionControl.isInit()) return value;
		double scaleFactor = ResolutionControl.getInstance().getCurrentScaleFactor();
		return Math.max(Mth.ceil((double) value * scaleFactor), 1);
	}
	
	@Inject(at = @At("RETURN"), method = "getGuiScale", cancellable = true)
	private void getScaleFactor(CallbackInfoReturnable<Double> ci) {
		if(ResolutionControl.isInit()) {
			ci.setReturnValue(ci.getReturnValueD() * ResolutionControl.getInstance().getCurrentScaleFactor());
		}else{
			ci.setReturnValue(ci.getReturnValueD());
		}
	}
	
	@Inject(at = @At("RETURN"), method = "onResize")
	private void onFramebufferSizeChanged(CallbackInfo ci) {
		if(ResolutionControl.isInit()) {
			ResolutionControl.getInstance().onResolutionChanged();
		}

	}
	
	@Inject(at = @At("RETURN"), method = "onFramebufferResize")
	private void onUpdateFramebufferSize(CallbackInfo ci) {
		if(ResolutionControl.isInit()) {
			ResolutionControl.getInstance().onResolutionChanged();
		}
	}
}
