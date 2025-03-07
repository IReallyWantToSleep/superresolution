package io.homo.superresolution.common.mixin.core;

import dev.architectury.platform.Platform;
import io.homo.superresolution.common.render.renderdoc.RenderDoc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.VirtualScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VirtualScreen.class)
public class VirtualScreenMixin {
    @Inject(at = @At(value = "TAIL"), method = "<init>")
    private void initRenderDoc(Minecraft minecraft, CallbackInfo ci) {
        //if (Platform.isDevelopmentEnvironment()) RenderDoc.init();
    }
}
