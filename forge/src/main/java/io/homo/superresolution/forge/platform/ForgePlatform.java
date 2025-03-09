package io.homo.superresolution.forge.platform;

import io.homo.superresolution.common.platform.EnvType;
import io.homo.superresolution.common.platform.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;

import java.nio.file.Path;

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

    @Override
    public String getModVersionString(String modId) {
        if (isModLoaded(modId)) return ModList.get().getModFileById(modId).versionString();
        return null;
    }

    public EnvType getEnv() {
        return switch (FMLLoader.getDist()) {
            case CLIENT -> EnvType.CLIENT;
            case DEDICATED_SERVER -> EnvType.SERVER;
        };
    }

    public Path getGameFolder() {
        return FMLPaths.GAMEDIR.get();
    }
}
