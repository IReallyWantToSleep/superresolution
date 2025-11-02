/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.test;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaMeasureMode;

public record SerializedMeasureFunc(
        float inputWidth,
        YogaMeasureMode widthMode,
        float inputHeight,
        YogaMeasureMode heightMode,
        float outputWidth,
        float outputHeight,
        long durationNs
) {
    public boolean inputsMatch(
            float actualWidth,
            float actualHeight,
            YogaMeasureMode actualWidthMode,
            YogaMeasureMode actualHeightMode) {
        var widthsAreUndefined =
                Float.isNaN(actualWidth) && Float.isNaN(inputWidth);
        var widthsAreEqual = widthsAreUndefined || actualWidth == inputWidth;
        var heightsAreUndefined =
                Float.isNaN(actualHeight) && Float.isNaN(inputHeight);
        var heightsAreEqual = heightsAreUndefined || actualHeight == inputHeight;

        return widthsAreEqual && heightsAreEqual &&
                actualWidthMode == widthMode &&
                actualHeightMode == heightMode;
    }
}
