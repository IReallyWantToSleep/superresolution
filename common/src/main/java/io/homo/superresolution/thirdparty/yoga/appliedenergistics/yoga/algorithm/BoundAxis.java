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
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.FloatOptional;

public class BoundAxis {

    /**
     * Calculate the padding and border for an axis
     *
     * @param node      The node
     * @param axis      The flex direction axis
     * @param direction The layout direction
     * @param widthSize The width size
     * @return The padding and border value
     */
    public static float paddingAndBorderForAxis(
            YogaNode node,
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return node.getStyle().computeInlineStartPaddingAndBorder(axis, direction, widthSize) +
                node.getStyle().computeInlineEndPaddingAndBorder(axis, direction, widthSize);
    }

    /**
     * Bounds an axis value within minimum and maximum constraints
     *
     * @param node      The node
     * @param direction The layout direction
     * @param axis      The flex direction axis
     * @param value     The value to bound
     * @param axisSize  The axis size
     * @param widthSize The width size
     * @return The bounded value
     */
    public static FloatOptional boundAxisWithinMinAndMax(
            YogaNode node,
            YogaDirection direction,
            YogaFlexDirection axis,
            FloatOptional value,
            float axisSize,
            float widthSize) {
        FloatOptional min = FloatOptional.of();
        FloatOptional max = FloatOptional.of();

        if (FlexDirectionUtil.isColumn(axis)) {
            min = node.getStyle().resolveMinDimension(direction, YogaDimension.HEIGHT, axisSize, widthSize);
            max = node.getStyle().resolveMaxDimension(direction, YogaDimension.HEIGHT, axisSize, widthSize);
        } else if (FlexDirectionUtil.isRow(axis)) {
            min = node.getStyle().resolveMinDimension(direction, YogaDimension.WIDTH, axisSize, widthSize);
            max = node.getStyle().resolveMaxDimension(direction, YogaDimension.WIDTH, axisSize, widthSize);
        }

        if (max.isDefined() && max.unwrap() >= 0 && value.unwrap() > max.unwrap()) {
            return max;
        }

        if (min.isDefined() && min.unwrap() >= 0 && value.unwrap() < min.unwrap()) {
            return min;
        }

        return value;
    }

    /**
     * Like boundAxisWithinMinAndMax but also ensures that the value doesn't
     * go below the padding and border amount.
     *
     * @param node      The node
     * @param axis      The flex direction axis
     * @param direction The layout direction
     * @param value     The value to bound
     * @param axisSize  The axis size
     * @param widthSize The width size
     * @return The bounded value
     */
    public static float boundAxis(
            YogaNode node,
            YogaFlexDirection axis,
            YogaDirection direction,
            float value,
            float axisSize,
            float widthSize) {
        return Comparison.maxOrDefined(
                boundAxisWithinMinAndMax(
                        node, direction, axis, FloatOptional.of(value), axisSize, widthSize)
                        .unwrap(),
                paddingAndBorderForAxis(node, axis, direction, widthSize));
    }
}
