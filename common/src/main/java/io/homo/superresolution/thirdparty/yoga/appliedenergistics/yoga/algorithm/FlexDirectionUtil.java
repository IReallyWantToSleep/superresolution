/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDimension;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPhysicalEdge;

/**
 * Utility class for FlexDirection related operations.
 * Corresponds to facebook::yoga::FlexDirection.h in C++.
 */
public final class FlexDirectionUtil {
    private FlexDirectionUtil() {
        // Prevent instantiation
    }

    /**
     * Checks if the flex direction is a row.
     *
     * @param flexDirection The flex direction to check
     *
     * @return True if the flex direction is Row or RowReverse
     */
    public static boolean isRow(YogaFlexDirection flexDirection) {
        return flexDirection == YogaFlexDirection.ROW ||
                flexDirection == YogaFlexDirection.ROW_REVERSE;
    }

    /**
     * Checks if the flex direction is a column.
     *
     * @param flexDirection The flex direction to check
     *
     * @return True if the flex direction is Column or ColumnReverse
     */
    public static boolean isColumn(YogaFlexDirection flexDirection) {
        return flexDirection == YogaFlexDirection.COLUMN ||
                flexDirection == YogaFlexDirection.COLUMN_REVERSE;
    }

    /**
     * Resolves the flex direction considering the text direction.
     *
     * @param flexDirection The flex direction to resolve
     * @param direction     The text direction
     *
     * @return The resolved flex direction
     */
    public static YogaFlexDirection resolveDirection(
            YogaFlexDirection flexDirection,
            YogaDirection direction) {
        if (direction == YogaDirection.RTL) {
            if (flexDirection == YogaFlexDirection.ROW) {
                return YogaFlexDirection.ROW_REVERSE;
            } else if (flexDirection == YogaFlexDirection.ROW_REVERSE) {
                return YogaFlexDirection.ROW;
            }
        }

        return flexDirection;
    }

    /**
     * Resolves the cross direction based on the flex direction and text direction.
     *
     * @param flexDirection The flex direction
     * @param direction     The text direction
     *
     * @return The resolved cross direction
     */
    public static YogaFlexDirection resolveCrossDirection(
            YogaFlexDirection flexDirection,
            YogaDirection direction) {
        return isColumn(flexDirection)
                ? resolveDirection(YogaFlexDirection.ROW, direction)
                : YogaFlexDirection.COLUMN;
    }

    /**
     * Gets the flex start edge based on the flex direction.
     *
     * @param flexDirection The flex direction
     *
     * @return The physical edge corresponding to flex start
     *
     * @throws IllegalArgumentException If the flex direction is invalid
     */
    public static YogaPhysicalEdge flexStartEdge(YogaFlexDirection flexDirection) {
        switch (flexDirection) {
            case COLUMN:
                return YogaPhysicalEdge.TOP;
            case COLUMN_REVERSE:
                return YogaPhysicalEdge.BOTTOM;
            case ROW:
                return YogaPhysicalEdge.LEFT;
            case ROW_REVERSE:
                return YogaPhysicalEdge.RIGHT;
            default:
                throw new IllegalArgumentException("Invalid FlexDirection");
        }
    }

    /**
     * Gets the flex end edge based on the flex direction.
     *
     * @param flexDirection The flex direction
     *
     * @return The physical edge corresponding to flex end
     *
     * @throws IllegalArgumentException If the flex direction is invalid
     */
    public static YogaPhysicalEdge flexEndEdge(YogaFlexDirection flexDirection) {
        switch (flexDirection) {
            case COLUMN:
                return YogaPhysicalEdge.BOTTOM;
            case COLUMN_REVERSE:
                return YogaPhysicalEdge.TOP;
            case ROW:
                return YogaPhysicalEdge.RIGHT;
            case ROW_REVERSE:
                return YogaPhysicalEdge.LEFT;
            default:
                throw new IllegalArgumentException("Invalid FlexDirection");
        }
    }

    /**
     * Gets the inline start edge based on the flex direction and text direction.
     *
     * @param flexDirection The flex direction
     * @param direction     The text direction
     *
     * @return The physical edge corresponding to inline start
     */
    public static YogaPhysicalEdge inlineStartEdge(
            YogaFlexDirection flexDirection,
            YogaDirection direction) {
        if (isRow(flexDirection)) {
            return direction == YogaDirection.RTL ? YogaPhysicalEdge.RIGHT : YogaPhysicalEdge.LEFT;
        }

        return YogaPhysicalEdge.TOP;
    }

    /**
     * Gets the inline end edge based on the flex direction and text direction.
     *
     * @param flexDirection The flex direction
     * @param direction     The text direction
     *
     * @return The physical edge corresponding to inline end
     */
    public static YogaPhysicalEdge inlineEndEdge(
            YogaFlexDirection flexDirection,
            YogaDirection direction) {
        if (isRow(flexDirection)) {
            return direction == YogaDirection.RTL ? YogaPhysicalEdge.LEFT : YogaPhysicalEdge.RIGHT;
        }

        return YogaPhysicalEdge.BOTTOM;
    }

    /**
     * Gets the dimension corresponding to the flex direction.
     *
     * @param flexDirection The flex direction
     *
     * @return The dimension (width or height)
     *
     * @throws IllegalArgumentException If the flex direction is invalid
     */
    public static YogaDimension dimension(YogaFlexDirection flexDirection) {
        switch (flexDirection) {
            case COLUMN:
            case COLUMN_REVERSE:
                return YogaDimension.HEIGHT;
            case ROW:
            case ROW_REVERSE:
                return YogaDimension.WIDTH;
            default:
                throw new IllegalArgumentException("Invalid FlexDirection");
        }
    }
}
