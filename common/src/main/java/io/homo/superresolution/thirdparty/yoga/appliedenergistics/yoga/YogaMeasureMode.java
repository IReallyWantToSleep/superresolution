/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * MeasureMode enum corresponding to YGMeasureMode from Yoga.
 */
public enum YogaMeasureMode {
    UNDEFINED,
    EXACTLY,
    AT_MOST;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case UNDEFINED -> "undefined";
            case EXACTLY -> "exactly";
            case AT_MOST -> "at-most";
        };
    }
}
