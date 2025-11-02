/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric;

/**
 * Utility class for floating-point comparisons and operations
 * corresponding to facebook::yoga::Comparison.h in C++.
 */
public final class Comparison {
    private Comparison() {
        // Prevent instantiation
    }

    /**
     * Checks if a floating-point value is undefined (NaN).
     *
     * @param value The floating-point value to check
     * @return True if the value is undefined (NaN), false otherwise
     */
    public static boolean isUndefined(float value) {
        return Float.isNaN(value);
    }

    /**
     * Checks if a floating-point value is undefined (NaN).
     *
     * @param value The floating-point value to check
     * @return True if the value is undefined (NaN), false otherwise
     */
    public static boolean isUndefined(double value) {
        return Double.isNaN(value);
    }

    /**
     * Checks if a floating-point value is defined (not NaN).
     *
     * @param value The floating-point value to check
     * @return True if the value is defined (not NaN), false otherwise
     */
    public static boolean isDefined(float value) {
        return !isUndefined(value);
    }

    /**
     * Checks if a floating-point value is defined (not NaN).
     *
     * @param value The floating-point value to check
     * @return True if the value is defined (not NaN), false otherwise
     */
    public static boolean isDefined(double value) {
        return !isUndefined(value);
    }

    /**
     * Checks if a floating-point value is infinite.
     *
     * @param value The floating-point value to check
     * @return True if the value is infinite, false otherwise
     */
    public static boolean isInf(float value) {
        return Float.isInfinite(value);
    }

    /**
     * Checks if a floating-point value is infinite.
     *
     * @param value The floating-point value to check
     * @return True if the value is infinite, false otherwise
     */
    public static boolean isInf(double value) {
        return Double.isInfinite(value);
    }

    /**
     * Returns the maximum of two floating-point values, handling undefined values.
     *
     * @param a The first value
     * @param b The second value
     * @return The maximum value, or the defined value if one is undefined
     */
    public static float maxOrDefined(float a, float b) {
        if (isDefined(a) && isDefined(b)) {
            return Math.max(a, b);
        }
        return isUndefined(a) ? b : a;
    }

    /**
     * Returns the maximum of two floating-point values, handling undefined values.
     *
     * @param a The first value
     * @param b The second value
     * @return The maximum value, or the defined value if one is undefined
     */
    public static double maxOrDefined(double a, double b) {
        if (isDefined(a) && isDefined(b)) {
            return Math.max(a, b);
        }
        return isUndefined(a) ? b : a;
    }

    /**
     * Returns the minimum of two floating-point values, handling undefined values.
     *
     * @param a The first value
     * @param b The second value
     * @return The minimum value, or the defined value if one is undefined
     */
    public static float minOrDefined(float a, float b) {
        if (isDefined(a) && isDefined(b)) {
            return Math.min(a, b);
        }
        return isUndefined(a) ? b : a;
    }

    /**
     * Returns the minimum of two floating-point values, handling undefined values.
     *
     * @param a The first value
     * @param b The second value
     * @return The minimum value, or the defined value if one is undefined
     */
    public static double minOrDefined(double a, double b) {
        if (isDefined(a) && isDefined(b)) {
            return Math.min(a, b);
        }
        return isUndefined(a) ? b : a;
    }

    /**
     * Compares two floating-point values for approximate equality,
     * or returns true if both are undefined.
     *
     * @param a The first value
     * @param b The second value
     * @return True if values are approximately equal or both undefined
     */
    public static boolean inexactEquals(float a, float b) {
        if (isDefined(a) && isDefined(b)) {
            return Math.abs(a - b) < 0.0001f;
        }
        return isUndefined(a) && isUndefined(b);
    }

    /**
     * Compares two floating-point values for approximate equality,
     * or returns true if both are undefined.
     *
     * @param a The first value
     * @param b The second value
     * @return True if values are approximately equal or both undefined
     */
    public static boolean inexactEquals(double a, double b) {
        if (isDefined(a) && isDefined(b)) {
            return Math.abs(a - b) < 0.0001;
        }
        return isUndefined(a) && isUndefined(b);
    }

    /**
     * Compares two arrays of floating-point values for approximate equality.
     *
     * @param val1 The first array
     * @param val2 The second array
     * @return True if all elements are approximately equal
     */
    public static boolean inexactEquals(float[] val1, float[] val2) {
        if (val1.length != val2.length) {
            return false;
        }

        for (int i = 0; i < val1.length; ++i) {
            if (!inexactEquals(val1[i], val2[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two arrays of floating-point values for approximate equality.
     *
     * @param val1 The first array
     * @param val2 The second array
     * @return True if all elements are approximately equal
     */
    public static boolean inexactEquals(double[] val1, double[] val2) {
        if (val1.length != val2.length) {
            return false;
        }

        for (int i = 0; i < val1.length; ++i) {
            if (!inexactEquals(val1[i], val2[i])) {
                return false;
            }
        }
        return true;
    }
}
