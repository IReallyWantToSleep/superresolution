package io.homo.superresolution.forge.platform;

import io.homo.superresolution.api.platform.EnvironmentType;
import io.homo.superresolution.api.platform.Platform;
import net.minecraft.SharedConstants;
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
    public String getMinecraftVersion() {
        #if MC_VER > MC_1_21_6
        return SharedConstants.getCurrentVersion().id();
        #else
        return SharedConstants.getCurrentVersion().getName();
        #endif
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

    public EnvironmentType getEnv() {
        return switch (FMLLoader.getDist()) {
            case CLIENT -> EnvironmentType.CLIENT;
            case DEDICATED_SERVER -> EnvironmentType.SERVER;
        };
    }

    public Path getGameFolder() {
        return FMLPaths.GAMEDIR.get();
    }
}
