/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDisplay;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPositionType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaWrap;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.BoundAxis;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexLineRunningLayout;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node.LayoutableChildren;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.FloatOptional;

import java.util.ArrayList;
import java.util.List;

public class FlexLine {
    // List of children which are part of the line flow. This means they are not
    // positioned absolutely, or with `display: "none"`, and do not overflow the
    // available dimensions.
    private final List<YogaNode> itemsInFlow;

    // Accumulation of the dimensions and margin of all the children on the
    // current line. This will be used in order to either set the dimensions of
    // the node if none already exist or to compute the remaining space left for
    // the flexible children.
    private final float sizeConsumed;

    // Number of edges along the line flow with an auto margin.
    private final int numberOfAutoMargins;

    // Layout information about the line computed in steps after line-breaking
    private FlexLineRunningLayout layout;

    public FlexLine(
            List<YogaNode> itemsInFlow,
            float sizeConsumed,
            int numberOfAutoMargins,
            FlexLineRunningLayout layout) {
        this.itemsInFlow = itemsInFlow;
        this.sizeConsumed = sizeConsumed;
        this.numberOfAutoMargins = numberOfAutoMargins;
        this.layout = layout;
    }

    public List<YogaNode> getItemsInFlow() {
        return itemsInFlow;
    }

    public float getSizeConsumed() {
        return sizeConsumed;
    }

    public int getNumberOfAutoMargins() {
        return numberOfAutoMargins;
    }

    public FlexLineRunningLayout getLayout() {
        return layout;
    }

    public void setLayout(FlexLineRunningLayout layout) {
        this.layout = layout;
    }

    /**
     * Calculates where a line starting at a given index should break, returning
     * information about the collective children on the line.
     * <p>
     * This function assumes that all the children of node have their
     * computedFlexBasis properly computed (To do this use
     * computeFlexBasisForChildren function).
     *
     * @param node                  The parent node
     * @param ownerDirection        The direction of the parent
     * @param ownerWidth            The width of the parent
     * @param mainAxisOwnerSize     The size of the parent along the main axis
     * @param availableInnerWidth   The available inner width
     * @param availableInnerMainDim The available inner dimension along the main axis
     * @param iterator              Iterator for the layoutable children
     * @param lineCount             The current line count
     * @return A FlexLine object containing information about the line
     */
    public static FlexLine calculateFlexLine(
            YogaNode node,
            YogaDirection ownerDirection,
            float ownerWidth,
            float mainAxisOwnerSize,
            float availableInnerWidth,
            float availableInnerMainDim,
            LayoutableChildren.LayoutIterator iterator,
            int lineCount) {

        List<YogaNode> itemsInFlow = new ArrayList<>();

        float sizeConsumed = 0.0f;
        float totalFlexGrowFactors = 0.0f;
        float totalFlexShrinkScaledFactors = 0.0f;
        int numberOfAutoMargins = 0;
        YogaNode firstElementInLine = null;

        float sizeConsumedIncludingMinConstraint = 0;
        YogaDirection direction = node.resolveDirection(ownerDirection);
        YogaFlexDirection mainAxis = FlexDirectionUtil.resolveDirection(
                node.getStyle().getFlexDirection(), direction);
        boolean isNodeFlexWrap = node.getStyle().getFlexWrap() != YogaWrap.NO_WRAP;
        float gap = node.getStyle().computeGapForAxis(mainAxis, availableInnerMainDim);

        // Add items to the current line until it's full or we run out of items.
        for (; iterator.hasNext(); iterator.next()) {
            var child = iterator.current();

            if (child.getStyle().getDisplay() == YogaDisplay.NONE ||
                    child.getStyle().getPositionType() == YogaPositionType.ABSOLUTE) {
                continue;
            }

            if (firstElementInLine == null) {
                firstElementInLine = child;
            }

            if (child.getStyle().flexStartMarginIsAuto(mainAxis, ownerDirection)) {
                numberOfAutoMargins++;
            }
            if (child.getStyle().flexEndMarginIsAuto(mainAxis, ownerDirection)) {
                numberOfAutoMargins++;
            }

            child.setLineIndex(lineCount);
            float childMarginMainAxis = child.getStyle().computeMarginForAxis(mainAxis, availableInnerWidth);
            float childLeadingGapMainAxis = child == firstElementInLine ? 0.0f : gap;

            FloatOptional flexBasisWithMinAndMaxConstraints = BoundAxis.boundAxisWithinMinAndMax(
                    child,
                    direction,
                    mainAxis,
                    child.getLayout().computedFlexBasis,
                    mainAxisOwnerSize,
                    ownerWidth);

            // If this is a multi-line flow and this item pushes us over the available
            // size, we've hit the end of the current line. Break out of the loop and
            // lay out the current line.
            if (sizeConsumedIncludingMinConstraint + flexBasisWithMinAndMaxConstraints.unwrap() +
                    childMarginMainAxis + childLeadingGapMainAxis > availableInnerMainDim &&
                    isNodeFlexWrap && !itemsInFlow.isEmpty()) {
                break;
            }

            sizeConsumedIncludingMinConstraint += flexBasisWithMinAndMaxConstraints.unwrap() +
                    childMarginMainAxis + childLeadingGapMainAxis;
            sizeConsumed += flexBasisWithMinAndMaxConstraints.unwrap() + childMarginMainAxis +
                    childLeadingGapMainAxis;

            if (child.isNodeFlexible()) {
                totalFlexGrowFactors += child.resolveFlexGrow();

                // Unlike the grow factor, the shrink factor is scaled relative to the
                // child dimension.
                totalFlexShrinkScaledFactors += -child.resolveFlexShrink() *
                        child.getLayout().computedFlexBasis.unwrap();
            }

            itemsInFlow.add(child);
        }

        // The total flex factor needs to be floored to 1.
        if (totalFlexGrowFactors > 0 && totalFlexGrowFactors < 1) {
            totalFlexGrowFactors = 1;
        }

        // The total flex shrink factor needs to be floored to 1.
        if (totalFlexShrinkScaledFactors > 0 && totalFlexShrinkScaledFactors < 1) {
            totalFlexShrinkScaledFactors = 1;
        }

        FlexLineRunningLayout layout = new FlexLineRunningLayout(
                totalFlexGrowFactors,
                totalFlexShrinkScaledFactors,
                0.0f,
                0.0f,
                0.0f);

        return new FlexLine(itemsInFlow, sizeConsumed, numberOfAutoMargins, layout);
    }
}
