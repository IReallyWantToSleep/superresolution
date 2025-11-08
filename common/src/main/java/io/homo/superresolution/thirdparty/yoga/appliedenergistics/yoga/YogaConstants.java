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

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Java conversion of the constants used in Yoga
 */
public class YogaConstants {

    /**
     * Float value to represent "undefined" in style values.
     * Java equivalent of NaN (Not a Number) in C/C++
     */
    public static final float UNDEFINED = Float.NaN;

    /**
     * Determines if a float value is undefined.
     *
     * @param value the value to check
     * @return true if the value is undefined (NaN)
     */
    public static boolean isUndefined(float value) {
        return Float.isNaN(value);
    }

    public static boolean isUndefined(YogaValue value) {
        return value.unit == YogaUnit.UNDEFINED;
    }
}
