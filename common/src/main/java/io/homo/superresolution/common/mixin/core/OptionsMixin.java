package io.homo.superresolution.common.mixin.core;

import io.homo.superresolution.common.mixin.core.accessor.OptionInstanceAccessor;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class OptionsMixin {
    @Final
    @Shadow
    private OptionInstance<GraphicsStatus> graphicsMode;

    @Inject(method = "graphicsMode", at = @At("TAIL"))
    private void overwriteGraphicsMode(CallbackInfoReturnable<OptionInstance<GraphicsStatus>> cir) {
        if (((GraphicsStatus) (((OptionInstanceAccessor) (Object) cir.getReturnValue()).getValue())).getId() == 2) {
            graphicsMode.set(GraphicsStatus.FANCY);
        }
    }
}
