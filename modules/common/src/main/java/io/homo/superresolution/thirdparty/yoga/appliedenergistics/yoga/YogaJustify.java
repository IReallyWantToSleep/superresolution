/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Justify enum corresponding to YGJustify from Yoga.
 */
public enum YogaJustify {
    FLEX_START,
    CENTER,
    FLEX_END,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case FLEX_START -> "flex-start";
            case CENTER -> "center";
            case FLEX_END -> "flex-end";
            case SPACE_BETWEEN -> "space-between";
            case SPACE_AROUND -> "space-around";
            case SPACE_EVENLY -> "space-evenly";
        };
    }
}
