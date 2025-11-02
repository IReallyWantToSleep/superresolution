/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDimension;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNodeType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPhysicalEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison;

public class PixelGrid {

    /**
     * Round a point value to the nearest physical pixel based on DPI
     * (pointScaleFactor)
     *
     * @param value            The value to round
     * @param pointScaleFactor The point scale factor
     * @param forceCeil        Whether to force rounding up
     * @param forceFloor       Whether to force rounding down
     * @return The rounded value
     */
    public static float roundValueToPixelGrid(
            double value,
            double pointScaleFactor,
            boolean forceCeil,
            boolean forceFloor) {

        double scaledValue = value * pointScaleFactor;
        // We want to calculate `fractial` such that `floor(scaledValue) = scaledValue
        // - fractial`.
        double fractial = scaledValue % 1.0;
        if (fractial < 0) {
            // This branch is for handling negative numbers for `value`.
            //
            // Regarding `floor` and `ceil`. Note that for a number x, `floor(x) <= x <=
            // ceil(x)` even for negative numbers. Here are a couple of examples:
            //   - x =  2.2: floor( 2.2) =  2, ceil( 2.2) =  3
            //   - x = -2.2: floor(-2.2) = -3, ceil(-2.2) = -2
            //
            // Regarding `fmodf`. For fractional negative numbers, `fmodf` returns a
            // negative number. For example, `fmodf(-2.2) = -0.2`. However, we want
            // `fractial` to be the number such that subtracting it from `value` will
            // give us `floor(value)`. In the case of negative numbers, adding 1 to
            // `fmodf(value)` gives us this. Let's continue the example from above:
            //   - fractial = fmodf(-2.2) = -0.2
            //   - Add 1 to the fraction: fractial2 = fractial + 1 = -0.2 + 1 = 0.8
            //   - Finding the `floor`: -2.2 - fractial2 = -2.2 - 0.8 = -3
            ++fractial;
        }
        if (Comparison.inexactEquals(fractial, 0)) {
            // First we check if the value is already rounded
            scaledValue = scaledValue - fractial;
        } else if (Comparison.inexactEquals(fractial, 1.0)) {
            scaledValue = scaledValue - fractial + 1.0;
        } else if (forceCeil) {
            // Next we check if we need to use forced rounding
            scaledValue = scaledValue - fractial + 1.0;
        } else if (forceFloor) {
            scaledValue = scaledValue - fractial;
        } else {
            // Finally we just round the value
            scaledValue = scaledValue - fractial +
                    (!Double.isNaN(fractial) &&
                            (fractial > 0.5 || Comparison.inexactEquals(fractial, 0.5))
                            ? 1.0
                            : 0.0);
        }
        return (Double.isNaN(scaledValue) || Double.isNaN(pointScaleFactor))
                ? Float.NaN
                : (float) (scaledValue / pointScaleFactor);
    }

    /**
     * Round the layout results of a node and its subtree to the pixel grid.
     *
     * @param node         The node to round layout results for
     * @param absoluteLeft The absolute left position
     * @param absoluteTop  The absolute top position
     */
    public static void roundLayoutResultsToPixelGrid(
            YogaNode node,
            double absoluteLeft,
            double absoluteTop) {

        final double pointScaleFactor = node.getConfig().getPointScaleFactor();

        final double nodeLeft = node.getLayout().position(YogaPhysicalEdge.LEFT);
        final double nodeTop = node.getLayout().position(YogaPhysicalEdge.TOP);

        final double nodeWidth = node.getLayout().dimension(YogaDimension.WIDTH);
        final double nodeHeight = node.getLayout().dimension(YogaDimension.HEIGHT);

        final double absoluteNodeLeft = absoluteLeft + nodeLeft;
        final double absoluteNodeTop = absoluteTop + nodeTop;

        final double absoluteNodeRight = absoluteNodeLeft + nodeWidth;
        final double absoluteNodeBottom = absoluteNodeTop + nodeHeight;

        if (pointScaleFactor != 0.0) {
            // If a node has a custom measure function we never want to round down its
            // size as this could lead to unwanted text truncation.
            final boolean textRounding = node.getNodeType() == YogaNodeType.TEXT;

            node.setLayoutPosition(
                    roundValueToPixelGrid(nodeLeft, pointScaleFactor, false, textRounding),
                    YogaPhysicalEdge.LEFT);

            node.setLayoutPosition(
                    roundValueToPixelGrid(nodeTop, pointScaleFactor, false, textRounding),
                    YogaPhysicalEdge.TOP);

            // We multiply dimension by scale factor and if the result is close to the
            // whole number, we don't have any fraction To verify if the result is close
            // to whole number we want to check both floor and ceil numbers

            final double scaledNodeWith = nodeWidth * pointScaleFactor;
            final boolean hasFractionalWidth =
                    !Comparison.inexactEquals(Math.round(scaledNodeWith), scaledNodeWith);

            final double scaledNodeHeight = nodeHeight * pointScaleFactor;
            final boolean hasFractionalHeight =
                    !Comparison.inexactEquals(Math.round(scaledNodeHeight), scaledNodeHeight);

            node.setLayoutDimension(
                    roundValueToPixelGrid(
                            absoluteNodeRight,
                            pointScaleFactor,
                            (textRounding && hasFractionalWidth),
                            (textRounding && !hasFractionalWidth)) -
                            roundValueToPixelGrid(
                                    absoluteNodeLeft, pointScaleFactor, false, textRounding),
                    YogaDimension.WIDTH);

            node.setLayoutDimension(
                    roundValueToPixelGrid(
                            absoluteNodeBottom,
                            pointScaleFactor,
                            (textRounding && hasFractionalHeight),
                            (textRounding && !hasFractionalHeight)) -
                            roundValueToPixelGrid(
                                    absoluteNodeTop, pointScaleFactor, false, textRounding),
                    YogaDimension.HEIGHT);
        }

        for (YogaNode child : node.getChildren()) {
            roundLayoutResultsToPixelGrid(child, absoluteNodeLeft, absoluteNodeTop);
        }
    }
}
