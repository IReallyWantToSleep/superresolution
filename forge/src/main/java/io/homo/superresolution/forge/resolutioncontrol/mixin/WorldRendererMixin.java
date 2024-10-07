package io.homo.superresolution.forge.resolutioncontrol.mixin;

import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class)
public class WorldRendererMixin {
    @Shadow
    private PostChain entityEffect;

    @Inject(at = @At("RETURN"), method = "doEntityOutline")
    private void onLoadEntityOutlineShader(CallbackInfo ci) {
        if(ResolutionControl.isInit()) {
            ResolutionControl.getInstance().resizeMinecraftFramebuffers();
        }
    }

    @Inject(at = @At("RETURN"), method = "resize")
    private void onOnResized(CallbackInfo ci) {
        if (entityEffect == null) return;
        if(ResolutionControl.isInit()) {
            ResolutionControl.getInstance().resizeMinecraftFramebuffers();
        }
    }
}
