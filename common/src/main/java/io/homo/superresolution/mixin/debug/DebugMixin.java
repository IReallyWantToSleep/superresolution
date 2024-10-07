package io.homo.superresolution.mixin.debug;

import io.homo.superresolution.debug.DebugInfo;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class DebugMixin {

    //emb用的redirect，难蚌
    @Inject(method = "getSystemInformation", at = @At(value = "RETURN"), cancellable = true)
    private void redirectRightTextEarly(CallbackInfoReturnable<List<String>> cir) {
        List<String> strings = cir.getReturnValue();
        strings.add("");
        strings.add(DebugInfo.getTextFrameTimeDelta());
        strings.add(DebugInfo.getTextFrameTimeDeltaAlgo());
        cir.setReturnValue(strings);
    }
}


