package io.homo.superresolution.forge.platform;

import io.homo.superresolution.common.platform.Platform;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;

public class ForgePlatform extends Platform {
    @Override
    public void init() {
        if (isInstallIris()) this.irisPlatform = new IrisForgePlatform();
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
