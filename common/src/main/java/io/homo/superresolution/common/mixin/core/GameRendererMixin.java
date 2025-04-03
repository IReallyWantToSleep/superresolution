package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.render.utils.CallType;
import io.homo.superresolution.common.render.MinecraftRenderHandle;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    #if MC_VER < MC_1_21_4
    protected abstract double getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting);

    #else
    protected abstract float getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting);

    #endif
    @Inject(method = "resize", at = @At(value = "HEAD"))
    private void onResize(int i, int j, CallbackInfo ci) {
        if (SuperResolution.isInit && SuperResolution.gameIsLoad) {
            SuperResolution.getInstance().resize(i, j);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderLevel")
    private void onRenderWorldBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldBegin(CallType.GAME_RENDERER);
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderLevel")
    private void onRenderWorldEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderWorldEnd(CallType.GAME_RENDERER);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderItemInHand")
    private void onRenderHandBegin(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderHandBegin();
        }
    }

    @Inject(at = @At(value = "RETURN"), method = "renderItemInHand")
    private void onRenderHandEnd(CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            MinecraftRenderHandle.onRenderHandEnd();
        }
    }


    #if MC_VER < MC_1_21_4
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getFov(Lnet/minecraft/client/Camera;FZ)D"), method = "renderLevel")
    private double onGetFov(GameRenderer instance, Camera d0, float fogtype, boolean b) {
        AlgorithmManager.param.verticalFov = getFov(d0, fogtype, b);
        return AlgorithmManager.param.verticalFov;
    }
    #else
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getFov(Lnet/minecraft/client/Camera;FZ)F"), method = "renderLevel")
    private float onGetFov(GameRenderer instance, Camera d0, float fogtype, boolean b) {
        AlgorithmManager.param.verticalFov = getFov(d0, fogtype, b);
        return (float) AlgorithmManager.param.verticalFov;
    }
    #endif

}
