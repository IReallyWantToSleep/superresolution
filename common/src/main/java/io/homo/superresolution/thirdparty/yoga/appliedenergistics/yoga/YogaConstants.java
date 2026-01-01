/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
