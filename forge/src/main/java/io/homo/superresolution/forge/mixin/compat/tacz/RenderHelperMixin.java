package io.homo.superresolution.forge.mixin.compat.tacz;

import com.tacz.guns.util.RenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RenderHelper.class, remap = false)
public class RenderHelperMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lcom/tacz/guns/compat/optifine/OptifineCompat;isOptifineInstalled()Z"), method = "enableItemEntityStencilTest")
    private static boolean enableItemEntityStencilTest() {
        return false;
    }
}
