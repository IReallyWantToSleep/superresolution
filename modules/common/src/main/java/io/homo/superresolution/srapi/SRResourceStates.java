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

public enum SRResourceStates {
    COMMON(1 << 0),
    UNORDERED_ACCESS(1 << 1),
    COMPUTE_READ(1 << 2),
    PIXEL_READ(1 << 3),
    PIXEL_COMPUTE_READ(COMPUTE_READ.value | PIXEL_READ.value),
    COPY_SRC(1 << 4),
    COPY_DEST(1 << 5),
    GENERIC_READ(COPY_SRC.value | COMPUTE_READ.value),
    INDIRECT_ARGUMENT(1 << 6),
    PRESENT(1 << 7),
    RENDER_TARGET(1 << 8),
    DEPTH_ATTACHMENT(1 << 9);

    public final int value;

    SRResourceStates(int value) {
        this.value = value;
    }

    public static int toBitmask(EnumSet<SRResourceStates> states) {
        int mask = 0;
        for (SRResourceStates state : states) {
            mask |= state.value;
        }
        return mask;
    }

    public static EnumSet<SRResourceStates> fromBitmask(int mask) {
        EnumSet<SRResourceStates> set = EnumSet.noneOf(SRResourceStates.class);
        for (SRResourceStates state : values()) {
            if ((mask & state.value) != 0) {
                set.add(state);
            }
        }
        return set;
    }
}
