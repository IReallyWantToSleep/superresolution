package io.homo.superresolution.common.platform;

import com.sun.jna.Platform;

public enum OSType {
    ANDROID(Platform.ANDROID == Platform.getOSType()),
    LINUX(Platform.LINUX == Platform.getOSType()),
    WINDOWS(Platform.WINDOWS == Platform.getOSType()),
    MACOS(Platform.MAC == Platform.getOSType()),
    ANY(true);

    private final boolean isCurrentOS;

    OSType(boolean isCurrentOS) {
        this.isCurrentOS = isCurrentOS;
    }

    public static OSType get() {
        if (ANDROID.isCurrentOS) {
            return ANDROID;
        } else if (LINUX.isCurrentOS) {
            return LINUX;
        } else if (WINDOWS.isCurrentOS) {
            return WINDOWS;
        } else if (MACOS.isCurrentOS) {
            return MACOS;
        } else {
            return ANY;
        }
    }

    public static boolean isCurrentOS(OSType osType) {
        return osType.isCurrentOS;
    }

    public boolean equals(OSType type) {
        return type == ANY || OSType.get() == type;
    }
}