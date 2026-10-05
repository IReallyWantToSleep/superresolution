/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.test;

public record FacebookSerializedMeasureFunc(
        float inputWidth,

        int widthMode,

        float inputHeight,

        int heightMode,

        float outputWidth,

        float outputHeight,

        long durationNs
) {
    public boolean inputsMatch(
            float actualWidth,
            float actualHeight,
            int actualWidthMode,
            int actualHeightMode) {
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
