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
 * This class represents a CSS Value for sizes (e.g. width, height, min-width,
 * etc.). It may be one of:
 * 1. Undefined
 * 2. A keyword (e.g. auto, max-content, stretch, etc.)
 * 3. A CSS &lt;length-percentage> value:
 * a. &lt;length> value (e.g. 10px)
 * b. &lt;percentage> value of a reference &lt;length>
 * <p>
 * References:
 * 1. https://www.w3.org/TR/css-values-4/#lengths
 * 2. https://www.w3.org/TR/css-values-4/#percentage-value
 * 3. https://www.w3.org/TR/css-values-4/#mixed-percentages
 */
public class StyleSizeLength {

    public static final StyleSizeLength AUTO = new StyleSizeLength(FloatOptional.of(), YogaUnit.AUTO);
    public static final StyleSizeLength MAX_CONTENT = new StyleSizeLength(FloatOptional.of(), YogaUnit.MAX_CONTENT);
    public static final StyleSizeLength FIT_CONTENT = new StyleSizeLength(FloatOptional.of(), YogaUnit.FIT_CONTENT);
    public static final StyleSizeLength STRETCH = new StyleSizeLength(FloatOptional.of(), YogaUnit.STRETCH);
    public static final StyleSizeLength UNDEFINED = new StyleSizeLength(FloatOptional.of(), YogaUnit.UNDEFINED);
    private FloatOptional value;
    private YogaUnit unit;

    private StyleSizeLength(FloatOptional value, YogaUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public static StyleSizeLength points(float value) {
        return Float.isNaN(value) || Float.isInfinite(value)
                ? undefined()
                : new StyleSizeLength(FloatOptional.of(value), YogaUnit.POINT);
    }

    public static StyleSizeLength percent(float value) {
        return Float.isNaN(value) || Float.isInfinite(value)
                ? undefined()
                : new StyleSizeLength(FloatOptional.of(value), YogaUnit.PERCENT);
    }

    public static StyleSizeLength ofAuto() {
        return AUTO;
    }

    public static StyleSizeLength ofMaxContent() {
        return MAX_CONTENT;
    }

    public static StyleSizeLength ofFitContent() {
        return FIT_CONTENT;
    }

    public static StyleSizeLength ofStretch() {
        return STRETCH;
    }

    public static StyleSizeLength undefined() {
        return UNDEFINED;
    }

    public boolean isAuto() {
        return unit == YogaUnit.AUTO;
    }

    public boolean isMaxContent() {
        return unit == YogaUnit.MAX_CONTENT;
    }

    public boolean isFitContent() {
        return unit == YogaUnit.FIT_CONTENT;
    }

    public boolean isStretch() {
        return unit == YogaUnit.STRETCH;
    }

    public boolean isUndefined() {
        return unit == YogaUnit.UNDEFINED;
    }

    public boolean isDefined() {
        return !isUndefined();
    }

    public boolean isPoints() {
        return unit == YogaUnit.POINT;
    }

    public boolean isPercent() {
        return unit == YogaUnit.PERCENT;
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

    public static StyleSizeLength fromYogaValue(YogaValue value) {
        return new StyleSizeLength(FloatOptional.of(value.value), value.unit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        StyleSizeLength other = (StyleSizeLength) obj;
        return value.equals(other.value) && unit == other.unit;
    }

    public boolean inexactEquals(StyleSizeLength other) {
        return unit == other.unit && value.inexactEquals(other.value);
    }

    @Override
    public String toString() {
        return switch (unit) {
            case UNDEFINED, AUTO, MAX_CONTENT, FIT_CONTENT, STRETCH -> unit.toString();
            case POINT -> value.toString();
            case PERCENT -> value + "%";
        };
    }
}
