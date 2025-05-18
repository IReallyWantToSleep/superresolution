package io.homo.superresolution.common.mixin.debug;

import io.homo.superresolution.common.debug.imgui.ImguiMain;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ImguiMixin {
    #if MC_VER < MC_1_21_5
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;unbindWrite()V"), method = "runTick")
    private void onRender(CallbackInfo ci) {
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().render();
        }
    }
    #else
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"), method = "runTick")
    private void onRender(CallbackInfo ci) {
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().render();
        }
    }
    #endif

    @Inject(at = @At(value = "HEAD"), method = "close")
    private void onExit(CallbackInfo ci) {
        if (ImguiMain.getInstance() != null) {
            ImguiMain.getInstance().destroy();
        }
    }
}
