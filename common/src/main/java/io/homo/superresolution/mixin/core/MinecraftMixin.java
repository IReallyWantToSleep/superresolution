package io.homo.superresolution.mixin.core;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.debug.DebugInfo;
import io.homo.superresolution.resolutioncontrol.ResolutionControl;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow @Final private RenderTarget mainRenderTarget;
    @Unique
    public float super_resolution$frameTimeDelta = 16.6f;
    @Unique
    public float super_resolution$lastRenderTime = -1;
    @Inject(at=@At(value = "RETURN"),method = "onGameLoadFinished")
    private void onLoadDone(CallbackInfo ci){
        if (SuperResolution.notSupportFSR2){
            Minecraft.getInstance().getToasts().addToast(
                    SystemToast.multiline(
                            Minecraft.getInstance(),
                            SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                            Component.literal("警告"),
                            Component.literal("你的显卡不支持使用GL_KHR_shader_subgroup扩展，FSR2仅支持NVIDIA和AMD显卡，不支持核显，将自动禁用FSR2相关功能。")
                    )
            );
        }
        SuperResolution.gameIsLoad = true;
    }

    @Inject(at=@At(value = "HEAD"),method = "close")
    private void onExit(CallbackInfo ci){
        SuperResolution.getInstance().destroy();
    }
    @Inject(at= @At(value = "INVOKE", target ="Lnet/minecraft/client/sounds/SoundManager;updateSource(Lnet/minecraft/client/Camera;)V", shift = At.Shift.AFTER),method = "runTick")
    private void onRenderStart(CallbackInfo ci){
        super_resolution$lastRenderTime = Util.getMillis();
    }
    @Inject(at= @At(value = "INVOKE", target ="Lcom/mojang/blaze3d/platform/Window;updateDisplay()V"),method = "runTick")
    private void onRenderEnd(CallbackInfo ci){
        if (super_resolution$lastRenderTime != -1){
            super_resolution$frameTimeDelta = Util.getMillis()- super_resolution$lastRenderTime;
            SuperResolution.setFrameTimeDelta(super_resolution$frameTimeDelta);
            DebugInfo.setFrameTimeDelta(super_resolution$frameTimeDelta);
        }else{
            SuperResolution.setFrameTimeDelta(16.6f);
            DebugInfo.setFrameTimeDelta(16.6f);
        }
    }
    @Inject(at=@At(value = "HEAD"),method = "getMainRenderTarget", cancellable = true)
    private void replaceMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir){
        if (SuperResolution.isRenderingWorld && Minecraft.getInstance().level != null) {
            cir.setReturnValue(ResolutionControl.getInstance().getFramebuffer());
        }else{
            cir.setReturnValue(this.mainRenderTarget);
        }

    }
    //@Redirect(method = "<init>",at= @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/MainTarget;<init>(II)V"))
    //private void createMainTarget(MainTarget instance, int width, int height){
    //}
}
