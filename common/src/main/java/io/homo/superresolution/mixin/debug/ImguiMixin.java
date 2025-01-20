package io.homo.superresolution.mixin.debug;

import io.homo.superresolution.debug.imgui.ImguiMain;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ImguiMixin {
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;unbindWrite()V"), method = "runTick")
    private void onRender(CallbackInfo ci) {
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().render();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "close")
    private void onExit(CallbackInfo ci) {
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().destroy();

        }
    }
}
