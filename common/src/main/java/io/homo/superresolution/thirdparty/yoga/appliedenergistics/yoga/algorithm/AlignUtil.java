/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaJustify;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil;

public class AlignUtil {

    /**
     * Resolves the child alignment based on the parent and child styles
     *
     * @param node  The parent node
     * @param child The child node
     * @return The resolved alignment
     */
    public static YogaAlign resolveChildAlignment(YogaNode node, YogaNode child) {
        final YogaAlign align = child.getStyle().getAlignSelf() == YogaAlign.AUTO
                ? node.getStyle().getAlignItems()
                : child.getStyle().getAlignSelf();
        if (align == YogaAlign.BASELINE && FlexDirectionUtil.isColumn(node.getStyle().getFlexDirection())) {
            return YogaAlign.FLEX_START;
        }
        return align;
    }

    /**
     * Fallback alignment to use on overflow
     * https://www.w3.org/TR/css-align-3/#distribution-values
     *
     * @param align The alignment
     * @return The fallback alignment
     */
    public static YogaAlign fallbackAlignment(YogaAlign align) {
        return switch (align) {
            // Fallback to flex-start
            case SPACE_BETWEEN, STRETCH -> YogaAlign.FLEX_START;

            // Fallback to safe center. TODO (T208209388): This should be aligned to
            // Start instead of FlexStart (for row-reverse containers)
            case SPACE_AROUND, SPACE_EVENLY -> YogaAlign.FLEX_START;

            default -> align;
        };
    }

    /**
     * Fallback alignment to use on overflow
     * https://www.w3.org/TR/css-align-3/#distribution-values
     *
     * @param align The justify alignment
     * @return The fallback alignment
     */
    public static YogaJustify fallbackAlignment(YogaJustify align) {
        return switch (align) {
            // Fallback to flex-start
            case SPACE_BETWEEN -> YogaJustify.FLEX_START;
            // TODO: Support `justify-content: stretch`
            // case Justify.Stretch -> Justify.FlexStart;

            // Fallback to safe center. TODO (T208209388): This should be aligned to
            // Start instead of FlexStart (for row-reverse containers)
            case SPACE_AROUND, SPACE_EVENLY -> YogaJustify.FLEX_START;

            default -> align;
        };
    }
}