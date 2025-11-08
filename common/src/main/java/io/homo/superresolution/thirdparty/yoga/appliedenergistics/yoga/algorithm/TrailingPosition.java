/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;

public class TrailingPosition {

    /**
     * Given an offset to an edge, returns the offset to the opposite edge on the
     * same axis. This assumes that the width/height of both nodes is determined at
     * this point.
     *
     * @param position       The position of the edge
     * @param axis           The flex direction axis
     * @param containingNode The containing node
     * @param node           The node to position
     * @return The position of the opposite edge
     */
    public static float getPositionOfOppositeEdge(
            float position,
            YogaFlexDirection axis,
            YogaNode containingNode,
            YogaNode node) {
        return containingNode.getLayout().measuredDimension(FlexDirectionUtil.dimension(axis)) -
                node.getLayout().measuredDimension(FlexDirectionUtil.dimension(axis)) - position;
    }

    /**
     * Sets the trailing position of a child
     *
     * @param node  The parent node
     * @param child The child node
     * @param axis  The flex direction axis
     */
    public static void setChildTrailingPosition(
            YogaNode node,
            YogaNode child,
            YogaFlexDirection axis) {
        child.setLayoutPosition(
                getPositionOfOppositeEdge(
                        child.getLayout().position(FlexDirectionUtil.flexStartEdge(axis)),
                        axis, node, child),
                FlexDirectionUtil.flexEndEdge(axis));
    }

    /**
     * Determines if an axis needs trailing position
     *
     * @param axis The flex direction axis
     * @return True if the axis needs trailing position
     */
    public static boolean needsTrailingPosition(YogaFlexDirection axis) {
        return axis == YogaFlexDirection.ROW_REVERSE ||
                axis == YogaFlexDirection.COLUMN_REVERSE;
    }
}
