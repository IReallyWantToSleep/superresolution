package io.homo.superresolution.neoforge.platform;

import io.homo.superresolution.common.platform.EnvType;
import io.homo.superresolution.common.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;

import java.nio.file.Path;

public class NeoForgePlatform extends Platform {
    @Override
    public void init() {
        this.irisPlatform = new IrisNeoForgePlatform();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public String getModVersionString(String modId) {
        if (isModLoaded(modId)) return ModList.get().getModFileById(modId).versionString();
        return null;
    }

    @Override
    public EnvType getEnv() {
        return FMLLoader.getDist().isClient() ? EnvType.CLIENT : EnvType.SERVER;
    }

    @Override
    public Path getGameFolder() {
        return FMLLoader.getGamePath();
    }
}
