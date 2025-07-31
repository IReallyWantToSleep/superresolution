package io.homo.superresolution.fabric.mixin.compat.sodium;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
#if MC_VER > MC_1_20_6
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
#else
import me.jellysquid.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
#endif
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GraphicsAdapterProbe.class, remap = false)
public class GraphicsAdapterProbeMixin {
    @Inject(method = "findAdapters", at = @At("HEAD"), cancellable = true)
    private static void fix(CallbackInfo ci) {
        //if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
        //    ci.cancel();
        //}
    }
}
