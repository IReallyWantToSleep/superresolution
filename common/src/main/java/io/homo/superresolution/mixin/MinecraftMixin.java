package io.homo.superresolution.mixin;

import io.homo.superresolution.SuperResolution;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(at=@At(value = "HEAD"),method = "onGameLoadFinished")
    private void onLoadDone(CallbackInfo ci){
        SuperResolution.gameIsLoad = true;
        SuperResolution.getInstance().resize(SuperResolution.getMinecraftWidth(),SuperResolution.getMinecraftHeight());
    }
    @Inject(at=@At(value = "HEAD"),method = "close")
    private void onExit(CallbackInfo ci){
        SuperResolution.getInstance().destroy();
    }
}
