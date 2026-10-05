/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * PositionType enum corresponding to YGPositionType from Yoga.
 */
public enum YogaPositionType {
    STATIC,
    RELATIVE,
    ABSOLUTE;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case STATIC -> "static";
            case RELATIVE -> "relative";
            case ABSOLUTE -> "absolute";
        };
    }
}
