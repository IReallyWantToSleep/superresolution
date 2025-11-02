/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison;

/**
 * Represents an optional float value that may be undefined (NaN).
 * Corresponds to facebook::yoga::FloatOptional in C++.
 */
public class FloatOptional {
    private static final FloatOptional UNDEFINED = new FloatOptional();

    private final float value;

    /**
     * Creates a FloatOptional with an undefined value.
     */
    private FloatOptional() {
        this.value = Float.NaN;
    }

    private FloatOptional(float value) {
        this.value = value;
    }

    /**
     * @return An immutable undefined float optional.
     */
    public static FloatOptional of() {
        return UNDEFINED;
    }

    /**
     * Creates a FloatOptional with the specified value.
     *
     * @param value The float value to wrap
     */
    public static FloatOptional of(float value) {
        if (Float.isNaN(value)) {
            return UNDEFINED;
        }
        return new FloatOptional(value);
    }

    /**
     * Returns the wrapped value, or NaN if it is undefined.
     *
     * @return The wrapped float value
     */
    public float unwrap() {
        return value;
    }

    /**
     * Returns the wrapped value, or the specified default value if undefined.
     *
     * @param defaultValue The default value to return if this is undefined
     * @return The wrapped value or the default
     */
    public float unwrapOrDefault(float defaultValue) {
        return isUndefined() ? defaultValue : value;
    }

    /**
     * Checks if this FloatOptional is undefined.
     *
     * @return True if the value is undefined (NaN), false otherwise
     */
    public boolean isUndefined() {
        return io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison.isUndefined(value);
    }

    /**
     * Checks if this FloatOptional has a defined value.
     *
     * @return True if the value is defined (not NaN), false otherwise
     */
    public boolean isDefined() {
        return io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison.isDefined(value);
    }

    /**
     * Adds this FloatOptional to another and returns the result.
     *
     * @param other The FloatOptional to add
     * @return A new FloatOptional with the sum of the values
     */
    public FloatOptional add(FloatOptional other) {
        return of(this.value + other.value);
    }

    /**
     * Compares this FloatOptional for approximate equality with another.
     *
     * @param other The FloatOptional to compare with
     * @return True if values are approximately equal or both undefined
     */
    public boolean inexactEquals(FloatOptional other) {
        return io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison.inexactEquals(this.value, other.value);
    }

    /**
     * Returns the maximum of this FloatOptional and another,
     * handling undefined values.
     *
     * @param other The FloatOptional to compare with
     * @return A new FloatOptional with the maximum value
     */
    public FloatOptional maxOrDefined(FloatOptional other) {
        return of(Comparison.maxOrDefined(this.value, other.value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FloatOptional that = (FloatOptional) o;

        // Handle NaN case - two NaN values should be equal
        if (this.isUndefined() && that.isUndefined()) {
            return true;
        }

        return Float.compare(this.value, that.value) == 0;
    }

    @Override
    public int hashCode() {
        return isUndefined() ? 0 : Float.floatToIntBits(value);
    }

    /**
     * Compares this FloatOptional with another for ordering.
     *
     * @param other the FloatOptional to be compared
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object
     */
    public int compareTo(FloatOptional other) {
        return Float.compare(this.value, other.value);
    }

    /**
     * Checks if this FloatOptional is greater than another.
     *
     * @param other The FloatOptional to compare with
     * @return True if this value is greater than the other
     */
    public boolean isGreaterThan(FloatOptional other) {
        return unwrap() > other.unwrap();
    }

    /**
     * Checks if this FloatOptional is less than another.
     *
     * @param other The FloatOptional to compare with
     * @return True if this value is less than the other
     */
    public boolean isLessThan(FloatOptional other) {
        return unwrap() < other.unwrap();
    }

    /**
     * Checks if this FloatOptional is greater than or equal to another.
     *
     * @param other The FloatOptional to compare with
     * @return True if this value is greater than or equal to the other
     */
    public boolean isGreaterThanOrEqual(FloatOptional other) {
        return this.isGreaterThan(other) || this.equals(other);
    }

    /**
     * Checks if this FloatOptional is less than or equal to another.
     *
     * @param other The FloatOptional to compare with
     * @return True if this value is less than or equal to the other
     */
    public boolean isLessThanOrEqual(FloatOptional other) {
        return this.isLessThan(other) || this.equals(other);
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (isUndefined()) {
            return "undefined";
        } else {
            return String.valueOf(value);
        }
    }
}
