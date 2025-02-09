package io.homo.superresolution.common.platform;

public abstract class Platform {
    public static Platform currentPlatform = null;

    public abstract boolean isModLoaded(String modId);

    public abstract boolean isDevelopmentEnvironment();

    public OS getOS() {
        return new OS();
    }
}
