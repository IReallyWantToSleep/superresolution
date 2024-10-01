package io.homo.superresolution.mixin;

import com.mojang.blaze3d.pipeline.MainTarget;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.debug.DebugInfo;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique
    public float super_resolution$frameTimeDelta = 16.6f;
    @Unique
    public float super_resolution$lastRenderTime = -1;
    @Inject(at=@At(value = "RETURN"),method = "onGameLoadFinished")
    private void onLoadDone(CallbackInfo ci){
        //SuperResolution.LOGGER.info("done");
        SuperResolution.gameIsLoad = true;
    }
    @Inject(at=@At(value = "TAIL"),method = "doWorldLoad")
    private void onResize(CallbackInfo ci){
        SuperResolution.getInstance().resize(SuperResolution.getMinecraftWidth(),SuperResolution.getMinecraftHeight());
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
    //@Redirect(method = "<init>",at= @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/MainTarget;<init>(II)V"))
    //private void createMainTarget(MainTarget instance, int width, int height){
    //}
}
