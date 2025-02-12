package io.homo.superresolution.common.platform;

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

    public IrisPlatform iris() {
        return irisPlatform;
    }

    public abstract void init();

    public boolean isInstallIris() {
        if (isInstallIris == null)
            isInstallIris = currentPlatform.isModLoaded("iris") || currentPlatform.isModLoaded("oculus");
        return isInstallIris;
    }
}
