/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * FlexDirection enum corresponding to YGFlexDirection from Yoga.
 */
public enum YogaFlexDirection {
    COLUMN,
    COLUMN_REVERSE,
    ROW,
    ROW_REVERSE;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case COLUMN -> "column";
            case COLUMN_REVERSE -> "column-reverse";
            case ROW -> "row";
            case ROW_REVERSE -> "row-reverse";
        };
    }
}
