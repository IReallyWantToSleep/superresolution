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

package io.homo.superresolution.srapi;

import java.util.EnumSet;

public enum SRResourceUsage {
    READ_ONLY(0),
    RENDERTARGET(1 << 0),
    UAV(1 << 1),
    DEPTHTARGET(1 << 2),
    INDIRECT(1 << 3),
    ARRAYVIEW(1 << 4),
    STENCILTARGET(1 << 5),
    DCC_RENDERTARGET(1 << 15);

    public final int value;

    SRResourceUsage(int value) {
        this.value = value;
    }

    public static int toBitmask(EnumSet<SRResourceUsage> usages) {
        int mask = 0;
        for (SRResourceUsage usage : usages) {
            mask |= usage.value;
        }
        return mask;
    }

    public static EnumSet<SRResourceUsage> fromBitmask(int mask) {
        EnumSet<SRResourceUsage> set = EnumSet.noneOf(SRResourceUsage.class);
        for (SRResourceUsage usage : values()) {
            if ((mask & usage.value) != 0) {
                set.add(usage);
            }
        }
        return set;
    }
}
