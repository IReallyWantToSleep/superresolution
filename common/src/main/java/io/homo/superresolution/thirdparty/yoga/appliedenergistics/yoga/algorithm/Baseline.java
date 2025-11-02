/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDimension;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPhysicalEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPositionType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.AlignUtil;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.YogaEvent;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.YogaEventType;

public class Baseline {

    /**
     * Calculate baseline represented as an offset from the top edge of the node.
     *
     * @param node The node to calculate baseline for
     * @return The baseline value
     */
    public static float calculateBaseline(YogaNode node) {
        if (node.isBaselineDefined()) {
            YogaEvent.publish(node, YogaEventType.NODE_BASELINE_START);

            final float baseline = node.baseline(
                    node.getLayout().measuredDimension(YogaDimension.WIDTH),
                    node.getLayout().measuredDimension(YogaDimension.HEIGHT));

            YogaEvent.publish(node, YogaEventType.NODE_BASELINE_END);

            assert !Float.isNaN(baseline) : "Expect custom baseline function to not return NaN";
            return baseline;
        }

        YogaNode baselineChild = null;
        for (YogaNode child : node.getLayoutChildren()) {
            if (child.getLineIndex() > 0) {
                break;
            }
            if (child.getStyle().getPositionType() == YogaPositionType.ABSOLUTE) {
                continue;
            }
            if (AlignUtil.resolveChildAlignment(node, child) == YogaAlign.BASELINE ||
                    child.isReferenceBaseline()) {
                baselineChild = child;
                break;
            }

            if (baselineChild == null) {
                baselineChild = child;
            }
        }

        if (baselineChild == null) {
            return node.getLayout().measuredDimension(YogaDimension.HEIGHT);
        }

        final float baseline = calculateBaseline(baselineChild);
        return baseline + baselineChild.getLayout().position(YogaPhysicalEdge.TOP);
    }

    /**
     * Whether any of the children of this node participate in baseline alignment
     *
     * @param node The node to check
     * @return True if any children participate in baseline alignment
     */
    public static boolean isBaselineLayout(YogaNode node) {
        if (FlexDirectionUtil.isColumn(node.getStyle().getFlexDirection())) {
            return false;
        }
        if (node.getStyle().getAlignItems() == YogaAlign.BASELINE) {
            return true;
        }
        for (YogaNode child : node.getLayoutChildren()) {
            if (child.getStyle().getPositionType() != YogaPositionType.ABSOLUTE &&
                    child.getStyle().getAlignSelf() == YogaAlign.BASELINE) {
                return true;
            }
        }

        return false;
    }
}
