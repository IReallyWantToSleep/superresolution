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

package io.homo.superresolution.core.graphics.glslang.enums;

public enum EShTargetClientVersion {
    EShTargetVulkan_1_0(1 << 22),
    EShTargetVulkan_1_1((1 << 22) | (1 << 12)),
    EShTargetVulkan_1_2((1 << 22) | (2 << 12)),
    EShTargetVulkan_1_3((1 << 22) | (3 << 12)),
    EShTargetVulkan_1_4((1 << 22) | (4 << 12)),
    EShTargetOpenGL_450(450);
    private final int value;

    EShTargetClientVersion(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
