package io.homo.superresolution.fabric.platform;

import io.homo.superresolution.common.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatform extends Platform {
    @Override
    public void init() {
        this.irisPlatform = new IrisFabricPlatform();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
