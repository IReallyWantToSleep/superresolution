/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

/**
 * PhysicalEdge enum corresponding to the physical edges from Yoga.
 */
public enum YogaPhysicalEdge {
    LEFT(0),    // Matching the C++ enum where Edge::Left is 0
    TOP(1),     // Matching the C++ enum where Edge::Top is 1
    RIGHT(2),   // Matching the C++ enum where Edge::Right is 2
    BOTTOM(3);  // Matching the C++ enum where Edge::Bottom is 3

    private final int value;

    YogaPhysicalEdge(int value) {
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
            case LEFT -> "left";
            case TOP -> "top";
            case RIGHT -> "right";
            case BOTTOM -> "bottom";
        };
    }
}
