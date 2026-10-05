/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Unit enum corresponding to YGUnit from Yoga.
 */
public enum YogaUnit {
    UNDEFINED,
    POINT,
    PERCENT,
    AUTO,
    MAX_CONTENT,
    FIT_CONTENT,
    STRETCH;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case UNDEFINED -> "undefined";
            case POINT -> "point";
            case PERCENT -> "percent";
            case AUTO -> "auto";
            case MAX_CONTENT -> "max-content";
            case FIT_CONTENT -> "fit-content";
            case STRETCH -> "stretch";
        };
    }
}
