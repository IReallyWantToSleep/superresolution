/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Errata enum corresponding to YGErrata from Yoga.
 * Implemented as a bitfield, representing flags.
 */
public enum YogaErrata {
    STRETCH_FLEX_BASIS(1),
    ABSOLUTE_POSITION_WITHOUT_INSETS_EXCLUDES_PADDING(2),
    ABSOLUTE_PERCENT_AGAINST_INNER_SIZE(4);

    public static final Set<YogaErrata> NONE = Collections.unmodifiableSet(EnumSet.noneOf(YogaErrata.class));

    public static final Set<YogaErrata> ALL = Collections.unmodifiableSet(EnumSet.allOf(YogaErrata.class));

    // Same as ALL in the original code
    public static final Set<YogaErrata> CLASSIC = ALL;

    private final int value;

    YogaErrata(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case STRETCH_FLEX_BASIS -> "stretch-flex-basis";
            case ABSOLUTE_POSITION_WITHOUT_INSETS_EXCLUDES_PADDING ->
                    "absolute-position-without-insets-excludes-padding";
            case ABSOLUTE_PERCENT_AGAINST_INNER_SIZE -> "absolute-percent-against-inner-size";
        };
    }
}
