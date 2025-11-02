/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Direction enum corresponding to YGDirection from Yoga.
 */
public enum YogaDirection {
    INHERIT,
    LTR,
    RTL;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case INHERIT -> "inherit";
            case LTR -> "ltr";
            case RTL -> "rtl";
        };
    }
}
