package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Iris.class,remap = false)
public class IrisMixin {
    @Inject(method = "loadShaderpack",at=@At("TAIL"))
    private static void loadShaderpackMixin(CallbackInfo ci) {
        SuperResolution.recreateAlgorithm();
    }
}
