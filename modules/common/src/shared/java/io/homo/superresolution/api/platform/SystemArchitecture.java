/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

public enum SystemArchitecture {
    AARCH64("aarch64"),
    ARM32("arm"),
    X86_64("x86-64"),
    X86("x86"),
    ANY("&^*");

    private final String platformArch;

    SystemArchitecture(String platformArch) {
        this.platformArch = platformArch;
    }

    public static SystemArchitecture get() {
        String arch = Platform.ARCH;
        for (SystemArchitecture a : values()) {
            if (a != ANY && a.platformArch.equals(arch)) {
                return a;
            }
        }
        return ANY;
    }

    public boolean equals(SystemArchitecture arch) {
        return arch == ANY || SystemArchitecture.get() == arch;
    }

    public String getString() {
        return switch (this) {
            case AARCH64 -> "aarch64";
            case ARM32 -> "arm32";
            case X86_64 -> "x64";
            case X86 -> "x32";
            case ANY -> Component.translatable("superresolution.requirement.os.any").getString();
        };
    }
}