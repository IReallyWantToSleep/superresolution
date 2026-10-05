/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Wrap enum corresponding to YGWrap from Yoga.
 */
public enum YogaWrap {
    NO_WRAP,
    WRAP,
    WRAP_REVERSE;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case NO_WRAP -> "nowrap";
            case WRAP -> "wrap";
            case WRAP_REVERSE -> "wrap-reverse";
        };
    }
}
