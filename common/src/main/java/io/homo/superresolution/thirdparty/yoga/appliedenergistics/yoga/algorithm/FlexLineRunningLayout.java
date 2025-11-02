/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

public class FlexLineRunningLayout {
    // Total flex grow factors of flex items which are to be laid in the current
    // line. This is decremented as free space is distributed.
    private float totalFlexGrowFactors;

    // Total flex shrink factors of flex items which are to be laid in the current
    // line. This is decremented as free space is distributed.
    private float totalFlexShrinkScaledFactors;

    // The amount of available space within inner dimensions of the line which may
    // still be distributed.
    private float remainingFreeSpace;

    // The size of the mainDim for the row after considering size, padding, margin
    // and border of flex items. This is used to calculate maxLineDim after going
    // through all the rows to decide on the main axis size of owner.
    private float mainDim;

    // The size of the crossDim for the row after considering size, padding,
    // margin and border of flex items. Used for calculating containers crossSize.
    private float crossDim;

    public FlexLineRunningLayout() {
        this(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public FlexLineRunningLayout(
            float totalFlexGrowFactors,
            float totalFlexShrinkScaledFactors,
            float remainingFreeSpace,
            float mainDim,
            float crossDim) {
        this.totalFlexGrowFactors = totalFlexGrowFactors;
        this.totalFlexShrinkScaledFactors = totalFlexShrinkScaledFactors;
        this.remainingFreeSpace = remainingFreeSpace;
        this.mainDim = mainDim;
        this.crossDim = crossDim;
    }

    public float getTotalFlexGrowFactors() {
        return totalFlexGrowFactors;
    }

    public void setTotalFlexGrowFactors(float totalFlexGrowFactors) {
        this.totalFlexGrowFactors = totalFlexGrowFactors;
    }

    public float getTotalFlexShrinkScaledFactors() {
        return totalFlexShrinkScaledFactors;
    }

    public void setTotalFlexShrinkScaledFactors(float totalFlexShrinkScaledFactors) {
        this.totalFlexShrinkScaledFactors = totalFlexShrinkScaledFactors;
    }

    public float getRemainingFreeSpace() {
        return remainingFreeSpace;
    }

    public void setRemainingFreeSpace(float remainingFreeSpace) {
        this.remainingFreeSpace = remainingFreeSpace;
    }

    public float getMainDim() {
        return mainDim;
    }

    public void setMainDim(float mainDim) {
        this.mainDim = mainDim;
    }

    public float getCrossDim() {
        return crossDim;
    }

    public void setCrossDim(float crossDim) {
        this.crossDim = crossDim;
    }
}
