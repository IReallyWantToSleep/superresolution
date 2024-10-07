package io.homo.superresolution.fabric.compat.sodium;


import me.jellysquid.mods.sodium.client.platform.windows.api.d3dkmt.D3DKMT;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(D3DKMT.class)
public class SoidumD3DKMTMixin {
    @Inject(method = "findGraphicsAdapters",at=@At(value = "HEAD"),remap = false, cancellable = true)
    private static void fixDevError(CallbackInfoReturnable<List<D3DKMT.WDDMAdapterInfo>> cir){
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            cir.setReturnValue(List.of());
        }
    }

}
