/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.api.platform;

import com.sun.jna.Platform;
import net.minecraft.network.chat.Component;

public enum OperatingSystemType {
    ANDROID(Platform.ANDROID == Platform.getOSType()),
    LINUX(Platform.LINUX == Platform.getOSType()),
    WINDOWS(Platform.WINDOWS == Platform.getOSType()),
    MACOS(Platform.MAC == Platform.getOSType()),
    ANY(true);

    private final boolean isCurrentOS;

    OperatingSystemType(boolean isCurrentOS) {
        this.isCurrentOS = isCurrentOS;
    }

    public static OperatingSystemType get() {
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

    public static boolean isCurrentOS(OperatingSystemType osType) {
        return osType.isCurrentOS;
    }

    public boolean equals(OperatingSystemType type) {
        return type == ANY || OperatingSystemType.get() == type;
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