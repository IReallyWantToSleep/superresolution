/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaConstants;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode;

/**
 * Java conversion of CachedMeasurement
 */
public class CachedMeasurement {

    public float availableWidth = -1;
    public float availableHeight = -1;
    public SizingMode widthSizingMode = SizingMode.MAX_CONTENT;
    public SizingMode heightSizingMode = SizingMode.MAX_CONTENT;

    public float computedWidth = -1;
    public float computedHeight = -1;

    public CachedMeasurement() {
    }

    /**
     * Creates a new CachedMeasurement that is a copy of the specified CachedMeasurement.
     *
     * @param other The CachedMeasurement to copy
     */
    public CachedMeasurement(CachedMeasurement other) {
        this.availableWidth = other.availableWidth;
        this.availableHeight = other.availableHeight;
        this.widthSizingMode = other.widthSizingMode;
        this.heightSizingMode = other.heightSizingMode;
        this.computedWidth = other.computedWidth;
        this.computedHeight = other.computedHeight;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CachedMeasurement other = (CachedMeasurement) obj;
        boolean isEqual = widthSizingMode == other.widthSizingMode &&
                heightSizingMode == other.heightSizingMode;

        if (!YogaConstants.isUndefined(availableWidth) ||
                !YogaConstants.isUndefined(other.availableWidth)) {
            isEqual = isEqual && availableWidth == other.availableWidth;
        }

        if (!YogaConstants.isUndefined(availableHeight) ||
                !YogaConstants.isUndefined(other.availableHeight)) {
            isEqual = isEqual && availableHeight == other.availableHeight;
        }

        if (!YogaConstants.isUndefined(computedWidth) ||
                !YogaConstants.isUndefined(other.computedWidth)) {
            isEqual = isEqual && computedWidth == other.computedWidth;
        }

        if (!YogaConstants.isUndefined(computedHeight) ||
                !YogaConstants.isUndefined(other.computedHeight)) {
            isEqual = isEqual && computedHeight == other.computedHeight;
        }

        return isEqual;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(availableWidth);
        result = 31 * result + Float.floatToIntBits(availableHeight);
        result = 31 * result + widthSizingMode.hashCode();
        result = 31 * result + heightSizingMode.hashCode();
        result = 31 * result + Float.floatToIntBits(computedWidth);
        result = 31 * result + Float.floatToIntBits(computedHeight);
        return result;
    }
}
