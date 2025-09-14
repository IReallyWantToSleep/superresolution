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

package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShTargetLanguageVersion {
    EShTargetSpv_1_0((1 << 16)),
    EShTargetSpv_1_1((1 << 16) | (1 << 8)),
    EShTargetSpv_1_2((1 << 16) | (2 << 8)),
    EShTargetSpv_1_3((1 << 16) | (3 << 8)),
    EShTargetSpv_1_4((1 << 16) | (4 << 8)),
    EShTargetSpv_1_5((1 << 16) | (5 << 8)),
    EShTargetSpv_1_6((1 << 16) | (6 << 8));
    private final int value;

    EShTargetLanguageVersion(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
