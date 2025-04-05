package io.homo.superresolution.common.platform;

import com.sun.jna.Platform;
import io.homo.superresolution.common.SuperResolution;
import net.minecraft.network.chat.Component;

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
        if (ANDROID.isCurrentOS || System.getenv("POJAV_RENDERER") != null) {
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

    public String getString() {
        return switch (this) {
            case ANDROID -> "Android";
            case LINUX -> "Linux";
            case WINDOWS -> "Windows";
            case MACOS -> "MacOS";
            case ANY -> Component.translatable("superresolution.requirement.os.any").getString();
        };
    }
}