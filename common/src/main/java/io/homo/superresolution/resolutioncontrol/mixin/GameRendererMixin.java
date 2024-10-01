package io.homo.superresolution.resolutioncontrol.mixin;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.debug.DebugInfo;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class)
public abstract class GameRendererMixin {
	@Unique
	public float super_resolution$frameTimeDelta_fsr = 16.6f;
	@Unique
	public float super_resolution$lastRenderTime_fsr = -1;

	@Inject(at = @At(value = "HEAD"), method = "renderLevel")
	private void onRenderBegin(CallbackInfo callbackInfo) {
		if (Minecraft.getInstance().level != null){
			ResolutionControl.getInstance().setShouldScale(true);
		}
	}
	@Inject(at = @At(value = "RETURN"), method = "renderLevel")
	private void onRenderEnd(CallbackInfo ci) {
		if (Minecraft.getInstance().level != null){
			super_resolution$lastRenderTime_fsr = Util.getMillis();
			SuperResolution.FSR.CallFSR2(SuperResolution.frameTimeDelta);
			super_resolution$frameTimeDelta_fsr = Util.getMillis()-super_resolution$lastRenderTime_fsr;
			DebugInfo.setFrameTimeDelta_fsr(super_resolution$frameTimeDelta_fsr);
			ResolutionControl.getInstance().setShouldScale(false);
			SuperResolution.FSR.blitToScreen();
		}
	}
}
