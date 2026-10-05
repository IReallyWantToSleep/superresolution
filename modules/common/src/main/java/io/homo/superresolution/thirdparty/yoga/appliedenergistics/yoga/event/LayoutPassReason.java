/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event;

public enum LayoutPassReason {
    INITIAL,
    ABS_LAYOUT,
    STRETCH,
    MULTILINE_STRETCH,
    FLEX_LAYOUT,
    MEASURE_CHILD,
    ABS_MEASURE_CHILD,
    FLEX_MEASURE;

    public static final int COUNT = values().length;

    public static String toString(LayoutPassReason value) {
        return switch (value) {
            case INITIAL -> "initial";
            case ABS_LAYOUT -> "abs_layout";
            case STRETCH -> "stretch";
            case MULTILINE_STRETCH -> "multiline_stretch";
            case FLEX_LAYOUT -> "flex_layout";
            case MEASURE_CHILD -> "measure";
            case ABS_MEASURE_CHILD -> "abs_measure";
            case FLEX_MEASURE -> "flex_measure";
        };
    }
}
