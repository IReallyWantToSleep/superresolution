package io.homo.superresolution.common.mixin.core;

import com.mojang.blaze3d.platform.GLX;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.LongSupplier;

@Mixin(value = GLX.class)
public class GLXMixin {
    @Inject(method = "_initGlfw", at = @At(value = "RETURN"), remap = false)
    private static void detectSupportedVersions(CallbackInfoReturnable<LongSupplier> cir) {
        GraphicsCapabilities.detectSupportedVersions();
    }
}
