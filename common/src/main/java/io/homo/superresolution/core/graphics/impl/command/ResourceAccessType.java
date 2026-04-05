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

package io.homo.superresolution.core.graphics.impl.command;

public enum ResourceAccessType {
    UNDEFINED,
    SAMPLED_READ,
    STORAGE_READ,
    STORAGE_WRITE,
    STORAGE_READ_WRITE,
    COLOR_ATTACHMENT_WRITE,
    DEPTH_ATTACHMENT_WRITE,
    TRANSFER_SRC,
    TRANSFER_DST;

    public boolean includesWrite() {
        return this == STORAGE_WRITE || this == STORAGE_READ_WRITE
                || this == COLOR_ATTACHMENT_WRITE || this == DEPTH_ATTACHMENT_WRITE
                || this == TRANSFER_DST;
    }
}
