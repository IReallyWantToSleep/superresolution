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

import java.util.Objects;

/**
 * Java conversion of YGValue
 */
public class YogaValue {
    public final float value;
    public final io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaUnit unit;

    // Constants
    public static final YogaValue ZERO = new YogaValue(0, io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaUnit.POINT);
    public static final YogaValue UNDEFINED = new YogaValue(YogaConstants.UNDEFINED, io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaUnit.UNDEFINED);
    public static final YogaValue AUTO = new YogaValue(YogaConstants.UNDEFINED, io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaUnit.AUTO);

    public YogaValue(float value, io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public static YogaValue point(float value) {
        return new YogaValue(value, io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaUnit.POINT);
    }

    public static YogaValue percent(float value) {
        return new YogaValue(value, YogaUnit.PERCENT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        YogaValue yogaValue = (YogaValue) o;

        if (unit != yogaValue.unit) {
            return false;
        }

        return switch (unit) {
            case UNDEFINED, AUTO, FIT_CONTENT, MAX_CONTENT, STRETCH -> true;
            case POINT, PERCENT -> Float.compare(value, yogaValue.value) == 0;
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, unit);
    }

    @Override
    public String toString() {
        return switch (unit) {
            case UNDEFINED -> "undefined";
            case POINT -> value + "pt";
            case PERCENT -> value + "%";
            case AUTO -> "auto";
            case MAX_CONTENT -> "max-content";
            case FIT_CONTENT -> "fit-content";
            case STRETCH -> "stretch";
        };
    }

    public YogaValue neg() {
        return new YogaValue(-value, unit);
    }
}
