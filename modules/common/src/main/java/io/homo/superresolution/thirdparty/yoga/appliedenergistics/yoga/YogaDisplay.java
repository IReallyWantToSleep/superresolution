/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Display enum corresponding to YGDisplay from Yoga.
 */
public enum YogaDisplay {
    FLEX,
    NONE,
    CONTENTS;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case FLEX -> "flex";
            case NONE -> "none";
            case CONTENTS -> "contents";
        };
    }
}
