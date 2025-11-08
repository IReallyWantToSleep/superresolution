/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaConstants;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDimension;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDisplay;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaErrata;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaJustify;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPhysicalEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPositionType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaWrap;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.LayoutData;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.LayoutPassReason;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison;

public class AbsoluteLayout {

    private static void setFlexStartLayoutPosition(
            final YogaNode parent,
            YogaNode child,
            final YogaDirection direction,
            final YogaFlexDirection axis,
            final float containingBlockWidth) {
        float position = child.getStyle().computeFlexStartMargin(
                axis, direction, containingBlockWidth) +
                parent.getLayout().border(FlexDirectionUtil.flexStartEdge(axis));

        if (!child.hasErrata(YogaErrata.ABSOLUTE_POSITION_WITHOUT_INSETS_EXCLUDES_PADDING)) {
            position += parent.getLayout().padding(FlexDirectionUtil.flexStartEdge(axis));
        }

        child.setLayoutPosition(position, FlexDirectionUtil.flexStartEdge(axis));
    }

    private static void setFlexEndLayoutPosition(
            final YogaNode parent,
            YogaNode child,
            final YogaDirection direction,
            final YogaFlexDirection axis,
            final float containingBlockWidth) {
        float flexEndPosition = parent.getLayout().border(FlexDirectionUtil.flexEndEdge(axis)) +
                child.getStyle().computeFlexEndMargin(
                        axis, direction, containingBlockWidth);

        if (!child.hasErrata(YogaErrata.ABSOLUTE_POSITION_WITHOUT_INSETS_EXCLUDES_PADDING)) {
            flexEndPosition += parent.getLayout().padding(FlexDirectionUtil.flexEndEdge(axis));
        }

        child.setLayoutPosition(
                TrailingPosition.getPositionOfOppositeEdge(flexEndPosition, axis, parent, child),
                FlexDirectionUtil.flexStartEdge(axis));
    }

    private static void setCenterLayoutPosition(
            final YogaNode parent,
            YogaNode child,
            final YogaDirection direction,
            final YogaFlexDirection axis,
            final float containingBlockWidth) {
        float parentContentBoxSize =
                parent.getLayout().measuredDimension(FlexDirectionUtil.dimension(axis)) -
                        parent.getLayout().border(FlexDirectionUtil.flexStartEdge(axis)) -
                        parent.getLayout().border(FlexDirectionUtil.flexEndEdge(axis));

        if (!child.hasErrata(YogaErrata.ABSOLUTE_POSITION_WITHOUT_INSETS_EXCLUDES_PADDING)) {
            parentContentBoxSize -= parent.getLayout().padding(FlexDirectionUtil.flexStartEdge(axis));
            parentContentBoxSize -= parent.getLayout().padding(FlexDirectionUtil.flexEndEdge(axis));
        }

        final float childOuterSize =
                child.getLayout().measuredDimension(FlexDirectionUtil.dimension(axis)) +
                        child.getStyle().computeMarginForAxis(axis, containingBlockWidth);

        float position = (parentContentBoxSize - childOuterSize) / 2.0f +
                parent.getLayout().border(FlexDirectionUtil.flexStartEdge(axis)) +
                child.getStyle().computeFlexStartMargin(
                        axis, direction, containingBlockWidth);

        if (!child.hasErrata(YogaErrata.ABSOLUTE_POSITION_WITHOUT_INSETS_EXCLUDES_PADDING)) {
            position += parent.getLayout().padding(FlexDirectionUtil.flexStartEdge(axis));
        }

        child.setLayoutPosition(position, FlexDirectionUtil.flexStartEdge(axis));
    }

    private static void justifyAbsoluteChild(
            final YogaNode parent,
            YogaNode child,
            final YogaDirection direction,
            final YogaFlexDirection mainAxis,
            final float containingBlockWidth) {
        final YogaJustify parentJustifyContent = parent.getStyle().getJustifyContent();
        switch (parentJustifyContent) {
            case FLEX_START:
            case SPACE_BETWEEN:
                setFlexStartLayoutPosition(
                        parent, child, direction, mainAxis, containingBlockWidth);
                break;
            case FLEX_END:
                setFlexEndLayoutPosition(
                        parent, child, direction, mainAxis, containingBlockWidth);
                break;
            case CENTER:
            case SPACE_AROUND:
            case SPACE_EVENLY:
                setCenterLayoutPosition(
                        parent, child, direction, mainAxis, containingBlockWidth);
                break;
        }
    }

    private static void alignAbsoluteChild(
            final YogaNode parent,
            YogaNode child,
            final YogaDirection direction,
            final YogaFlexDirection crossAxis,
            final float containingBlockWidth) {
        YogaAlign itemAlign = AlignUtil.resolveChildAlignment(parent, child);
        final YogaWrap parentWrap = parent.getStyle().getFlexWrap();
        if (parentWrap == YogaWrap.WRAP_REVERSE) {
            if (itemAlign == YogaAlign.FLEX_END) {
                itemAlign = YogaAlign.FLEX_START;
            } else if (itemAlign != YogaAlign.CENTER) {
                itemAlign = YogaAlign.FLEX_END;
            }
        }

        switch (itemAlign) {
            case AUTO:
            case FLEX_START:
            case BASELINE:
            case SPACE_AROUND:
            case SPACE_BETWEEN:
            case STRETCH:
            case SPACE_EVENLY:
                setFlexStartLayoutPosition(
                        parent, child, direction, crossAxis, containingBlockWidth);
                break;
            case FLEX_END:
                setFlexEndLayoutPosition(
                        parent, child, direction, crossAxis, containingBlockWidth);
                break;
            case CENTER:
                setCenterLayoutPosition(
                        parent, child, direction, crossAxis, containingBlockWidth);
                break;
        }
    }

    /*
     * Absolutely positioned nodes do not participate in flex layout and thus their
     * positions can be determined independently from the rest of their siblings.
     * For each axis there are essentially two cases:
     *
     * 1) The node has insets defined. In this case we can just use these to
     *    determine the position of the node.
     * 2) The node does not have insets defined. In this case we look at the style
     *    of the parent to position the node. Things like justify content and
     *    align content will move absolute children around. If none of these
     *    special properties are defined, the child is positioned at the start
     *    (defined by flex direction) of the leading flex line.
     *
     * This function does that positioning for the given axis. The spec has more
     * information on this topic: https://www.w3.org/TR/css-flexbox-1/#abspos-items
     */
    private static void positionAbsoluteChild(
            final YogaNode containingNode,
            final YogaNode parent,
            YogaNode child,
            final YogaDirection direction,
            final YogaFlexDirection axis,
            final boolean isMainAxis,
            final float containingBlockWidth,
            final float containingBlockHeight) {
        final boolean isAxisRow = FlexDirectionUtil.isRow(axis);
        final float containingBlockSize =
                isAxisRow ? containingBlockWidth : containingBlockHeight;

        // The inline-start position takes priority over the end position in the case
        // that they are both set and the node has a fixed width. Thus we only have 2
        // cases here: if inline-start is defined and if inline-end is defined.
        //
        // Despite checking inline-start to honor prioritization of insets, we write
        // to the flex-start edge because this algorithm works by positioning on the
        // flex-start edge and then filling in the flex-end direction at the end if
        // necessary.
        if (child.getStyle().isInlineStartPositionDefined(axis, direction) &&
                !child.getStyle().isInlineStartPositionAuto(axis, direction)) {
            final float positionRelativeToInlineStart =
                    child.getStyle().computeInlineStartPosition(
                            axis, direction, containingBlockSize) +
                            containingNode.getStyle().computeInlineStartBorder(axis, direction) +
                            child.getStyle().computeInlineStartMargin(
                                    axis, direction, containingBlockSize);
            final float positionRelativeToFlexStart =
                    FlexDirectionUtil.inlineStartEdge(axis, direction) != FlexDirectionUtil.flexStartEdge(axis)
                            ? TrailingPosition.getPositionOfOppositeEdge(
                            positionRelativeToInlineStart, axis, containingNode, child)
                            : positionRelativeToInlineStart;

            child.setLayoutPosition(positionRelativeToFlexStart, FlexDirectionUtil.flexStartEdge(axis));
        } else if (
                child.getStyle().isInlineEndPositionDefined(axis, direction) &&
                        !child.getStyle().isInlineEndPositionAuto(axis, direction)) {
            final float positionRelativeToInlineStart =
                    containingNode.getLayout().measuredDimension(FlexDirectionUtil.dimension(axis)) -
                            child.getLayout().measuredDimension(FlexDirectionUtil.dimension(axis)) -
                            containingNode.getStyle().computeInlineEndBorder(axis, direction) -
                            child.getStyle().computeInlineEndMargin(
                                    axis, direction, containingBlockSize) -
                            child.getStyle().computeInlineEndPosition(
                                    axis, direction, containingBlockSize);
            final float positionRelativeToFlexStart =
                    FlexDirectionUtil.inlineStartEdge(axis, direction) != FlexDirectionUtil.flexStartEdge(axis)
                            ? TrailingPosition.getPositionOfOppositeEdge(
                            positionRelativeToInlineStart, axis, containingNode, child)
                            : positionRelativeToInlineStart;

            child.setLayoutPosition(positionRelativeToFlexStart, FlexDirectionUtil.flexStartEdge(axis));
        } else {
            if (isMainAxis) {
                justifyAbsoluteChild(parent, child, direction, axis, containingBlockWidth);
            } else {
                alignAbsoluteChild(parent, child, direction, axis, containingBlockWidth);
            }
        }
    }

    public static void layoutAbsoluteChild(
            final YogaNode containingNode,
            final YogaNode node,
            YogaNode child,
            final float containingBlockWidth,
            final float containingBlockHeight,
            final io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthMode,
            final YogaDirection direction,
            LayoutData layoutMarkerData,
            final int depth,
            final int generationCount) {
        final YogaFlexDirection mainAxis =
                FlexDirectionUtil.resolveDirection(node.getStyle().getFlexDirection(), direction);
        final YogaFlexDirection crossAxis = FlexDirectionUtil.resolveCrossDirection(mainAxis, direction);
        final boolean isMainAxisRow = FlexDirectionUtil.isRow(mainAxis);

        float childWidth = YogaConstants.UNDEFINED;
        float childHeight = YogaConstants.UNDEFINED;
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childWidthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT;
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childHeightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT;

        var marginRow = child.getStyle().computeMarginForAxis(
                YogaFlexDirection.ROW, containingBlockWidth);
        var marginColumn = child.getStyle().computeMarginForAxis(
                YogaFlexDirection.COLUMN, containingBlockWidth);

        if (child.hasDefiniteLength(YogaDimension.WIDTH, containingBlockWidth)) {
            childWidth = child
                    .getResolvedDimension(
                            direction,
                            YogaDimension.WIDTH,
                            containingBlockWidth,
                            containingBlockWidth)
                    .unwrap() +
                    marginRow;
        } else {
            // If the child doesn't have a specified width, compute the width based on
            // the left/right offsets if they're defined.
            if (child.getStyle().isFlexStartPositionDefined(
                    YogaFlexDirection.ROW, direction) &&
                    child.getStyle().isFlexEndPositionDefined(
                            YogaFlexDirection.ROW, direction) &&
                    !child.getStyle().isFlexStartPositionAuto(
                            YogaFlexDirection.ROW, direction) &&
                    !child.getStyle().isFlexEndPositionAuto(YogaFlexDirection.ROW, direction)) {
                childWidth =
                        containingNode.getLayout().measuredDimension(YogaDimension.WIDTH) -
                                (containingNode.getStyle().computeFlexStartBorder(
                                        YogaFlexDirection.ROW, direction) +
                                        containingNode.getStyle().computeFlexEndBorder(
                                                YogaFlexDirection.ROW, direction)) -
                                (child.getStyle().computeFlexStartPosition(
                                        YogaFlexDirection.ROW, direction, containingBlockWidth) +
                                        child.getStyle().computeFlexEndPosition(
                                                YogaFlexDirection.ROW, direction, containingBlockWidth));
                childWidth = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.BoundAxis.boundAxis(
                        child,
                        YogaFlexDirection.ROW,
                        direction,
                        childWidth,
                        containingBlockWidth,
                        containingBlockWidth);
            }
        }

        if (child.hasDefiniteLength(YogaDimension.HEIGHT, containingBlockHeight)) {
            childHeight = child
                    .getResolvedDimension(
                            direction,
                            YogaDimension.HEIGHT,
                            containingBlockHeight,
                            containingBlockWidth)
                    .unwrap() +
                    marginColumn;
        } else {
            // If the child doesn't have a specified height, compute the height based
            // on the top/bottom offsets if they're defined.
            if (child.getStyle().isFlexStartPositionDefined(
                    YogaFlexDirection.COLUMN, direction) &&
                    child.getStyle().isFlexEndPositionDefined(
                            YogaFlexDirection.COLUMN, direction) &&
                    !child.getStyle().isFlexStartPositionAuto(
                            YogaFlexDirection.COLUMN, direction) &&
                    !child.getStyle().isFlexEndPositionAuto(
                            YogaFlexDirection.COLUMN, direction)) {
                childHeight =
                        containingNode.getLayout().measuredDimension(YogaDimension.HEIGHT) -
                                (containingNode.getStyle().computeFlexStartBorder(
                                        YogaFlexDirection.COLUMN, direction) +
                                        containingNode.getStyle().computeFlexEndBorder(
                                                YogaFlexDirection.COLUMN, direction)) -
                                (child.getStyle().computeFlexStartPosition(
                                        YogaFlexDirection.COLUMN, direction, containingBlockHeight) +
                                        child.getStyle().computeFlexEndPosition(
                                                YogaFlexDirection.COLUMN, direction, containingBlockHeight));
                childHeight = BoundAxis.boundAxis(
                        child,
                        YogaFlexDirection.COLUMN,
                        direction,
                        childHeight,
                        containingBlockHeight,
                        containingBlockWidth);
            }
        }

        // Exactly one dimension needs to be defined for us to be able to do aspect
        // ratio calculation. One dimension being the anchor and the other being
        // flexible.
        final var childStyle = child.getStyle();
        if (Comparison.isUndefined(childWidth) ^ Comparison.isUndefined(childHeight)) {
            if (childStyle.getAspectRatio().isDefined()) {
                if (Comparison.isUndefined(childWidth)) {
                    childWidth = marginRow +
                            (childHeight - marginColumn) * childStyle.getAspectRatio().unwrap();
                } else if (Comparison.isUndefined(childHeight)) {
                    childHeight = marginColumn +
                            (childWidth - marginRow) / childStyle.getAspectRatio().unwrap();
                }
            }
        }

        // If we're still missing one or the other dimension, measure the content.
        if (Comparison.isUndefined(childWidth) || Comparison.isUndefined(childHeight)) {
            childWidthSizingMode = Comparison.isUndefined(childWidth)
                    ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT
                    : io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            childHeightSizingMode = Comparison.isUndefined(childHeight)
                    ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT
                    : io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;

            // If the size of the owner is defined then try to constrain the absolute
            // child to that size as well. This allows text within the absolute child
            // to wrap to the size of its owner. This is the same behavior as many
            // browsers implement.
            if (!isMainAxisRow && Comparison.isUndefined(childWidth) &&
                    widthMode != io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT &&
                    Comparison.isDefined(containingBlockWidth) && containingBlockWidth > 0) {
                childWidth = containingBlockWidth;
                childWidthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT;
            }

            CalculateLayout.calculateLayoutInternal(
                    child,
                    childWidth,
                    childHeight,
                    direction,
                    childWidthSizingMode,
                    childHeightSizingMode,
                    containingBlockWidth,
                    containingBlockHeight,
                    false,
                    LayoutPassReason.ABS_MEASURE_CHILD,
                    layoutMarkerData,
                    depth,
                    generationCount);
            childWidth = child.getLayout().measuredDimension(YogaDimension.WIDTH) +
                    child.getStyle().computeMarginForAxis(
                            YogaFlexDirection.ROW, containingBlockWidth);
            childHeight = child.getLayout().measuredDimension(YogaDimension.HEIGHT) +
                    child.getStyle().computeMarginForAxis(
                            YogaFlexDirection.COLUMN, containingBlockWidth);
        }

        CalculateLayout.calculateLayoutInternal(
                child,
                childWidth,
                childHeight,
                direction,
                io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT,
                io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT,
                containingBlockWidth,
                containingBlockHeight,
                true,
                LayoutPassReason.ABS_LAYOUT,
                layoutMarkerData,
                depth,
                generationCount);

        positionAbsoluteChild(
                containingNode,
                node,
                child,
                direction,
                mainAxis,
                true /*isMainAxis*/,
                containingBlockWidth,
                containingBlockHeight);
        positionAbsoluteChild(
                containingNode,
                node,
                child,
                direction,
                crossAxis,
                false /*isMainAxis*/,
                containingBlockWidth,
                containingBlockHeight);
    }

    public static boolean layoutAbsoluteDescendants(
            YogaNode containingNode,
            YogaNode currentNode,
            SizingMode widthSizingMode,
            YogaDirection currentNodeDirection,
            LayoutData layoutMarkerData,
            int currentDepth,
            int generationCount,
            float currentNodeLeftOffsetFromContainingBlock,
            float currentNodeTopOffsetFromContainingBlock,
            float containingNodeAvailableInnerWidth,
            float containingNodeAvailableInnerHeight) {
        boolean hasNewLayout = false;
        for (YogaNode child : currentNode.getLayoutChildren()) {
            if (child.getStyle().getDisplay() == YogaDisplay.NONE) {
                continue;
            } else if (child.getStyle().getPositionType() == YogaPositionType.ABSOLUTE) {
                final boolean absoluteErrata =
                        currentNode.hasErrata(YogaErrata.ABSOLUTE_PERCENT_AGAINST_INNER_SIZE);
                final float containingBlockWidth = absoluteErrata
                        ? containingNodeAvailableInnerWidth
                        : containingNode.getLayout().measuredDimension(YogaDimension.WIDTH) -
                        containingNode.getStyle().computeBorderForAxis(YogaFlexDirection.ROW);
                final float containingBlockHeight = absoluteErrata
                        ? containingNodeAvailableInnerHeight
                        : containingNode.getLayout().measuredDimension(YogaDimension.HEIGHT) -
                        containingNode.getStyle().computeBorderForAxis(
                                YogaFlexDirection.COLUMN);

                layoutAbsoluteChild(
                        containingNode,
                        currentNode,
                        child,
                        containingBlockWidth,
                        containingBlockHeight,
                        widthSizingMode,
                        currentNodeDirection,
                        layoutMarkerData,
                        currentDepth,
                        generationCount);

                hasNewLayout = hasNewLayout || child.hasNewLayout();

                /*
                 * At this point the child has its position set but only on its the
                 * parent's flexStart edge. Additionally, this position should be
                 * interpreted relative to the containing block of the child if it had
                 * insets defined. So we need to adjust the position by subtracting the
                 * the parents offset from the containing block. However, getting that
                 * offset is complicated since the two nodes can have different main/cross
                 * axes.
                 */
                final YogaFlexDirection parentMainAxis = FlexDirectionUtil.resolveDirection(
                        currentNode.getStyle().getFlexDirection(), currentNodeDirection);
                final YogaFlexDirection parentCrossAxis =
                        FlexDirectionUtil.resolveCrossDirection(parentMainAxis, currentNodeDirection);

                if (TrailingPosition.needsTrailingPosition(parentMainAxis)) {
                    final boolean mainInsetsDefined = FlexDirectionUtil.isRow(parentMainAxis)
                            ? child.getStyle().horizontalInsetsDefined()
                            : child.getStyle().verticalInsetsDefined();
                    TrailingPosition.setChildTrailingPosition(
                            mainInsetsDefined ? containingNode : currentNode,
                            child,
                            parentMainAxis);
                }
                if (TrailingPosition.needsTrailingPosition(parentCrossAxis)) {
                    final boolean crossInsetsDefined = FlexDirectionUtil.isRow(parentCrossAxis)
                            ? child.getStyle().horizontalInsetsDefined()
                            : child.getStyle().verticalInsetsDefined();
                    TrailingPosition.setChildTrailingPosition(
                            crossInsetsDefined ? containingNode : currentNode,
                            child,
                            parentCrossAxis);
                }

                /*
                 * At this point we know the left and top physical edges of the child are
                 * set with positions that are relative to the containing block if insets
                 * are defined
                 */
                final float childLeftPosition =
                        child.getLayout().position(YogaPhysicalEdge.LEFT);
                final float childTopPosition =
                        child.getLayout().position(YogaPhysicalEdge.TOP);

                final float childLeftOffsetFromParent =
                        child.getStyle().horizontalInsetsDefined()
                                ? (childLeftPosition - currentNodeLeftOffsetFromContainingBlock)
                                : childLeftPosition;
                final float childTopOffsetFromParent =
                        child.getStyle().verticalInsetsDefined()
                                ? (childTopPosition - currentNodeTopOffsetFromContainingBlock)
                                : childTopPosition;

                child.setLayoutPosition(childLeftOffsetFromParent, YogaPhysicalEdge.LEFT);
                child.setLayoutPosition(childTopOffsetFromParent, YogaPhysicalEdge.TOP);
            } else if (
                    child.getStyle().getPositionType() == YogaPositionType.STATIC &&
                            !child.alwaysFormsContainingBlock()) {
                // We may write new layout results for absolute descendants of "child"
                // which are positioned relative to the current containing block instead
                // of their parent. "child" may not be dirty, or have new constraints, so
                // absolute positioning may be the first time during this layout pass that
                // we need to mutate these descendents. Make sure the path of
                // nodes to them is mutable before positioning.
                child.cloneChildrenIfNeeded();
                final YogaDirection childDirection =
                        child.resolveDirection(currentNodeDirection);
                // By now all descendants of the containing block that are not absolute
                // will have their positions set for left and top.
                final float childLeftOffsetFromContainingBlock =
                        currentNodeLeftOffsetFromContainingBlock +
                                child.getLayout().position(YogaPhysicalEdge.LEFT);
                final float childTopOffsetFromContainingBlock =
                        currentNodeTopOffsetFromContainingBlock +
                                child.getLayout().position(YogaPhysicalEdge.TOP);

                hasNewLayout = layoutAbsoluteDescendants(
                        containingNode,
                        child,
                        widthSizingMode,
                        childDirection,
                        layoutMarkerData,
                        currentDepth + 1,
                        generationCount,
                        childLeftOffsetFromContainingBlock,
                        childTopOffsetFromContainingBlock,
                        containingNodeAvailableInnerWidth,
                        containingNodeAvailableInnerHeight) ||
                        hasNewLayout;

                if (hasNewLayout) {
                    child.setHasNewLayout(hasNewLayout);
                }
            }
        }
        return hasNewLayout;
    }
}
