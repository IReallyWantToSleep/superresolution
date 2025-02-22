package io.homo.superresolution.fabric;

import io.homo.superresolution.common.SuperResolution;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class SuperResolutionFabricClient implements ClientModInitializer {
    public static SuperResolution mod;

    @Override
    public void onInitializeClient() {
        SuperResolution.preInit();
        mod = new SuperResolution();
    }
}
