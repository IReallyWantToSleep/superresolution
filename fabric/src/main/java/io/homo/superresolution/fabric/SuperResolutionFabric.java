package io.homo.superresolution.fabric;

import io.homo.superresolution.SuperResolution;
import net.fabricmc.api.ModInitializer;

public final class SuperResolutionFabric implements ModInitializer {
    public static SuperResolution mod;
    @Override
    public void onInitialize() {
        SuperResolution.initFSR2Lib();
        mod = new SuperResolution();
    }
}
