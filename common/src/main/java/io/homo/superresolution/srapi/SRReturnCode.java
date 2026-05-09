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

public enum SRReturnCode {
    OK(0),
    NULL_POINTER(1),
    ERROR(2),
    CANNOT_FIND_PROVIDER(3),
    UNEXPECTED_ERROR(4),
    CANNOT_FIND_LIBRARY(5),
    INVALID_PROVIDER_LIBRARY(6),
    INVALID_ARGUMENT(7),
    UNSUPPORTED(8);
    private static final SRReturnCode[] values = values();
    public final int value;

    SRReturnCode(int value) {
        this.value = value;
    }

    public static SRReturnCode fromValue(int value) {
        for (SRReturnCode v : values) {
            if (v.value == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown SRReturnCode value: " + value);
    }
}
