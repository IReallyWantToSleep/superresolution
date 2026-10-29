/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * Edge enum corresponding to YGEdge from Yoga.
 */
public enum YogaEdge {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    START,
    END,
    HORIZONTAL,
    VERTICAL,
    ALL;

    /**
     * Returns the string representation of the enum value.
     *
     * @return String representation of the enum value.
     */
    @Override
    public String toString() {
        return switch (this) {
            case LEFT -> "left";
            case TOP -> "top";
            case RIGHT -> "right";
            case BOTTOM -> "bottom";
            case START -> "start";
            case END -> "end";
            case HORIZONTAL -> "horizontal";
            case VERTICAL -> "vertical";
            case ALL -> "all";
        };
    }
}
