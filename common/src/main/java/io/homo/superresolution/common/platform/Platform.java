package io.homo.superresolution.common.platform;

import net.minecraft.SharedConstants;

import java.nio.file.Path;

public abstract class Platform {
    public static Platform currentPlatform = null;
    private static Boolean isInstallIris = null;
    protected IrisPlatform irisPlatform = null;

    public abstract boolean isModLoaded(String modId);

    public abstract boolean isDevelopmentEnvironment();

    public abstract String getModVersionString(String modId);

    public OS getOS() {
        return new OS();
    }

    public abstract EnvType getEnv();

    public abstract Path getGameFolder();

    public IrisPlatform iris() {
        return irisPlatform;
    }

    public abstract void init();

    public boolean isInstallIris() {
        if (isInstallIris == null)

            isInstallIris = currentPlatform.isModLoaded("iris") || currentPlatform.isModLoaded("oculus");
        return isInstallIris;
    }

    public String getMinecraftVersion() {
        return SharedConstants.VERSION_STRING;
    }
}
