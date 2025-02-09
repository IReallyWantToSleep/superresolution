package io.homo.superresolution.forge.platform;

import io.homo.superresolution.common.platform.Platform;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;

public class ForgePlatform extends Platform {
    @Override
    public void init() {
        this.irisPlatform = new IrisForgePlatform();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
