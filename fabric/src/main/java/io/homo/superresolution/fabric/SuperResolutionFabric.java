package io.homo.superresolution.fabric;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.resolutioncontrol.mixin.MinecraftAccessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;

public final class SuperResolutionFabric implements ModInitializer {
    public static SuperResolution mod;
    @Override
    public void onInitialize() {
        SuperResolution.initFSR2Lib();
        mod = new SuperResolution();
    }
}
