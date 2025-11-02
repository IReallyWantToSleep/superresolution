/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaMeasureMode;

/**
 * Corresponds to a CSS auto box sizes. Missing "min-content", as Yoga does not
 * current support automatic minimum sizes.
 * https://www.w3.org/TR/css-sizing-3/#auto-box-sizes
 * https://www.w3.org/TR/css-flexbox-1/#min-size-auto
 */
public enum SizingMode {
    /**
     * The size a box would take if its outer size filled the available space in
     * the given axis; in other words, the stretch fit into the available space,
     * if that is definite. Undefined if the available space is indefinite.
     */
    STRETCH_FIT,

    /**
     * A box's "ideal" size in a given axis when given infinite available space.
     * Usually this is the smallest size the box could take in that axis while
     * still fitting around its contents, i.e. minimizing unfilled space while
     * avoiding overflow.
     */
    MAX_CONTENT,

    /**
     * If the available space in a given axis is definite, equal to
     * clamp(min-content size, stretch-fit size, max-content size) (i.e.
     * max(min-content size, min(max-content size, stretch-fit size))). When
     * sizing under a min-content constraint, equal to the min-content size.
     * Otherwise, equal to the max-content size in that axis.
     */
    FIT_CONTENT;

    /**
     * Convert a SizingMode to its equivalent MeasureMode
     *
     * @param mode The SizingMode to convert
     * @return The corresponding MeasureMode
     */
    public static YogaMeasureMode measureMode(SizingMode mode) {
        return switch (mode) {
            case STRETCH_FIT -> YogaMeasureMode.EXACTLY;
            case MAX_CONTENT -> YogaMeasureMode.UNDEFINED;
            case FIT_CONTENT -> YogaMeasureMode.AT_MOST;
        };
    }

    /**
     * Convert a MeasureMode to its equivalent SizingMode
     *
     * @param mode The MeasureMode to convert
     * @return The corresponding SizingMode
     * @throws IllegalArgumentException If an invalid MeasureMode is provided
     */
    public static SizingMode sizingMode(YogaMeasureMode mode) {
        return switch (mode) {
            case EXACTLY -> SizingMode.STRETCH_FIT;
            case UNDEFINED -> SizingMode.MAX_CONTENT;
            case AT_MOST -> SizingMode.FIT_CONTENT;
            default -> throw new IllegalArgumentException("Invalid MeasureMode");
        };
    }
}
