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

public enum SRParamValueType {
    UNKNOWN(0),
    BOOL(1),
    INT32(2),
    UINT32(3),
    INT64(4),
    UINT64(5),
    FLOAT(6),
    DOUBLE(7),
    STRING(8),
    POINTER(9);

    public final int value;

    SRParamValueType(int value) {
        this.value = value;
    }

    public static SRParamValueType fromValue(int value) {
        for (SRParamValueType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
