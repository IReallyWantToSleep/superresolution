package io.homo.superresolution.common.mixin.core.parts;

import io.homo.superresolution.common.minecraft.CallType;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixinCommon {
    @Unique
    private static boolean superresolution$windowIsHide = false;

    #if MC_VER < MC_1_21_4
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PostChain;resize(II)V"), method = "resize")
    private void onResizePostChain(PostChain instance, int w, int h) {
        if (ShaderCompatHandler.dontHackMinecraftRenderingPipeline()) return;
        instance.resize(RenderHandlerManager.getRenderWidth(), RenderHandlerManager.getRenderHeight());
    }
    #endif

    @Inject(at = @At(value = "HEAD"), method = "renderLevel", cancellable = true)
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            RenderHandlerManager.onRenderWorldBegin(CallType.LEVEL_RENDERER);
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            RenderHandlerManager.onRenderWorldEnd(CallType.LEVEL_RENDERER);
        }
    }
}