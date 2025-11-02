/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.PixelGrid;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaConfig;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison;

public class Cache {

    private static boolean sizeIsExactAndMatchesOldMeasuredSize(
            SizingMode sizeMode,
            float size,
            float lastComputedSize) {
        return sizeMode == SizingMode.STRETCH_FIT &&
                Comparison.inexactEquals(size, lastComputedSize);
    }

    private static boolean oldSizeIsMaxContentAndStillFits(
            SizingMode sizeMode,
            float size,
            SizingMode lastSizeMode,
            float lastComputedSize) {
        return sizeMode == SizingMode.FIT_CONTENT &&
                lastSizeMode == SizingMode.MAX_CONTENT &&
                (size >= lastComputedSize || Comparison.inexactEquals(size, lastComputedSize));
    }

    private static boolean newSizeIsStricterAndStillValid(
            SizingMode sizeMode,
            float size,
            SizingMode lastSizeMode,
            float lastSize,
            float lastComputedSize) {
        return lastSizeMode == SizingMode.FIT_CONTENT &&
                sizeMode == SizingMode.FIT_CONTENT &&
                Comparison.isDefined(lastSize) &&
                Comparison.isDefined(size) &&
                Comparison.isDefined(lastComputedSize) &&
                lastSize > size &&
                (lastComputedSize <= size || Comparison.inexactEquals(size, lastComputedSize));
    }

    /**
     * Determines if a cached measurement can be used
     *
     * @param widthMode           Width sizing mode
     * @param availableWidth      Available width
     * @param heightMode          Height sizing mode
     * @param availableHeight     Available height
     * @param lastWidthMode       Last width sizing mode
     * @param lastAvailableWidth  Last available width
     * @param lastHeightMode      Last height sizing mode
     * @param lastAvailableHeight Last available height
     * @param lastComputedWidth   Last computed width
     * @param lastComputedHeight  Last computed height
     * @param marginRow           Margin for row
     * @param marginColumn        Margin for column
     * @param config              The yoga config
     * @return True if cached measurement can be used
     */
    public static boolean canUseCachedMeasurement(
            SizingMode widthMode,
            float availableWidth,
            SizingMode heightMode,
            float availableHeight,
            SizingMode lastWidthMode,
            float lastAvailableWidth,
            SizingMode lastHeightMode,
            float lastAvailableHeight,
            float lastComputedWidth,
            float lastComputedHeight,
            float marginRow,
            float marginColumn,
            YogaConfig config) {

        if ((Comparison.isDefined(lastComputedHeight) && lastComputedHeight < 0) ||
                ((Comparison.isDefined(lastComputedWidth)) && lastComputedWidth < 0)) {
            return false;
        }

        final float pointScaleFactor = config.getPointScaleFactor();

        boolean useRoundedComparison = config != null && pointScaleFactor != 0;
        final float effectiveWidth = useRoundedComparison
                ? PixelGrid.roundValueToPixelGrid(availableWidth, pointScaleFactor, false, false)
                : availableWidth;
        final float effectiveHeight = useRoundedComparison
                ? PixelGrid.roundValueToPixelGrid(availableHeight, pointScaleFactor, false, false)
                : availableHeight;
        final float effectiveLastWidth = useRoundedComparison
                ? PixelGrid.roundValueToPixelGrid(lastAvailableWidth, pointScaleFactor, false, false)
                : lastAvailableWidth;
        final float effectiveLastHeight = useRoundedComparison
                ? PixelGrid.roundValueToPixelGrid(lastAvailableHeight, pointScaleFactor, false, false)
                : lastAvailableHeight;

        final boolean hasSameWidthSpec = lastWidthMode == widthMode &&
                Comparison.inexactEquals(effectiveLastWidth, effectiveWidth);
        final boolean hasSameHeightSpec = lastHeightMode == heightMode &&
                Comparison.inexactEquals(effectiveLastHeight, effectiveHeight);

        final boolean widthIsCompatible =
                hasSameWidthSpec ||
                        sizeIsExactAndMatchesOldMeasuredSize(
                                widthMode, availableWidth - marginRow, lastComputedWidth) ||
                        oldSizeIsMaxContentAndStillFits(
                                widthMode,
                                availableWidth - marginRow,
                                lastWidthMode,
                                lastComputedWidth) ||
                        newSizeIsStricterAndStillValid(
                                widthMode,
                                availableWidth - marginRow,
                                lastWidthMode,
                                lastAvailableWidth,
                                lastComputedWidth);

        final boolean heightIsCompatible = hasSameHeightSpec ||
                sizeIsExactAndMatchesOldMeasuredSize(
                        heightMode,
                        availableHeight - marginColumn,
                        lastComputedHeight) ||
                oldSizeIsMaxContentAndStillFits(
                        heightMode,
                        availableHeight - marginColumn,
                        lastHeightMode,
                        lastComputedHeight) ||
                newSizeIsStricterAndStillValid(
                        heightMode,
                        availableHeight - marginColumn,
                        lastHeightMode,
                        lastAvailableHeight,
                        lastComputedHeight);

        return widthIsCompatible && heightIsCompatible;
    }
}
