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

import net.minecraft.network.chat.Component;

public class OperatingSystem {
    public static OperatingSystem any = new OperatingSystem(SystemArchitecture.ANY, OperatingSystemType.ANY);
    public SystemArchitecture arch;
    public OperatingSystemType type;

    public OperatingSystem() {
        type = OperatingSystemType.get();
        arch = SystemArchitecture.get();
    }

    public OperatingSystem(SystemArchitecture arch, OperatingSystemType type) {
        this.type = type;
        this.arch = arch;
    }

    public String getString() {
        if (any.equals(this)) return Component.translatable("superresolution.requirement.os.any").getString();
        return "%s %s:%s".formatted(
                type.getString(),
                Component.translatable("superresolution.requirement.os.arch").getString(),
                arch.getString()
        );
    }
}
