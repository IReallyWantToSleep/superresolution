package io.homo.superresolution.fabric.mixin.compat.tacz;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.tacz.guns.util.RenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderHelper.class)
public class RenderHelperMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;tacz$enableStencil()V"), method = "enableItemEntityStencilTest", remap = false)
    private static void enableItemEntityStencilTest(RenderTarget instance) {

    }
}
