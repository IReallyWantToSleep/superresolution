package io.homo.superresolution.fabric;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.resolutioncontrol.mixin.MinecraftAccessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;

public final class SuperResolutionFabric implements ModInitializer {
    private SuperResolution common;
    @Override
    public void onInitialize() {
        this.common = new SuperResolution();
        this.common.init();

    }
}
