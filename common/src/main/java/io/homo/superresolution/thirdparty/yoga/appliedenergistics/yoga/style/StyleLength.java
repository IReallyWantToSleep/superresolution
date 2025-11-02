/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaUnit;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaValue;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.FloatOptional;

/**
 * Style::Length represents a CSS Value which may be one of:
 * 1. Undefined
 * 2. A keyword (e.g. auto)
 * 3. A CSS &lt;length-percentage> value:
 * a. &lt;length> value (e.g. 10px)
 * b. &lt;percentage> value of a reference &lt;length>
 * <p>
 * References:
 * 1. https://www.w3.org/TR/css-values-4/#lengths
 * 2. https://www.w3.org/TR/css-values-4/#percentage-value
 * 3. https://www.w3.org/TR/css-values-4/#mixed-percentages
 */
public class StyleLength {

    private static final StyleLength AUTO = new StyleLength(FloatOptional.of(), YogaUnit.AUTO);
    private static final StyleLength UNDEFINED = new StyleLength(FloatOptional.of(), YogaUnit.UNDEFINED);

    private final FloatOptional value;
    private final YogaUnit unit;

    private StyleLength(FloatOptional value, YogaUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public static StyleLength points(float value) {
        return Float.isNaN(value) || Float.isInfinite(value)
                ? undefined()
                : new StyleLength(FloatOptional.of(value), YogaUnit.POINT);
    }

    public static StyleLength percent(float value) {
        return Float.isNaN(value) || Float.isInfinite(value)
                ? undefined()
                : new StyleLength(FloatOptional.of(value), YogaUnit.PERCENT);
    }

    public static StyleLength ofAuto() {
        return AUTO;
    }

    public static StyleLength undefined() {
        return UNDEFINED;
    }

    public boolean isAuto() {
        return unit == YogaUnit.AUTO;
    }

    public boolean isUndefined() {
        return unit == YogaUnit.UNDEFINED;
    }

    public boolean isPoints() {
        return unit == YogaUnit.POINT;
    }

    public boolean isPercent() {
        return unit == YogaUnit.PERCENT;
    }

    public boolean isDefined() {
        return !isUndefined();
    }

    public FloatOptional value() {
        return value;
    }

    public FloatOptional resolve(float referenceLength) {
        return switch (unit) {
            case POINT -> value;
            case PERCENT -> FloatOptional.of(value.unwrap() * referenceLength * 0.01f);
            default -> FloatOptional.of();
        };
    }

    public YogaValue asYogaValue() {
        return new YogaValue(value.unwrap(), unit);
    }

    public static StyleLength fromYogaValue(YogaValue value) {
        return new StyleLength(FloatOptional.of(value.value), value.unit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        StyleLength other = (StyleLength) obj;
        return value.equals(other.value) && unit == other.unit;
    }

    public boolean inexactEquals(StyleLength other) {
        return unit == other.unit && value.inexactEquals(other.value);
    }
}
