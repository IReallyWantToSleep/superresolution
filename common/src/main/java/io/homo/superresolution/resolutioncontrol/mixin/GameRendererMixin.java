package io.homo.superresolution.resolutioncontrol.mixin;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class)
public abstract class GameRendererMixin {

	@Inject(at = @At(value = "HEAD"), method = "renderLevel")
	private void onRenderBegin(CallbackInfo callbackInfo) {
		if (Minecraft.getInstance().level != null){
			ResolutionControl.getInstance().setShouldScale(true);
		}
	}
	@Inject(at = @At(value = "RETURN"), method = "renderLevel")
	private void onRenderEnd(CallbackInfo callbackInfo) {
		if (Minecraft.getInstance().level != null){
			ResolutionControl.getInstance().setShouldScale(false);
		}

	}
}
