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
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaExperimentalFeature;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaJustify;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaOverflow;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPhysicalEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPositionType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaSize;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaWrap;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.BoundAxis;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexLine;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.LayoutData;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.LayoutPassReason;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.LayoutType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.YogaEvent;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.YogaEventType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node.CachedMeasurement;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node.LayoutResults;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node.LayoutableChildren;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.FloatOptional;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.AbsoluteLayout.layoutAbsoluteDescendants;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.AlignUtil.fallbackAlignment;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.AlignUtil.resolveChildAlignment;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.Baseline.calculateBaseline;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.Baseline.isBaselineLayout;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.BoundAxis.boundAxis;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.BoundAxis.boundAxisWithinMinAndMax;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.BoundAxis.paddingAndBorderForAxis;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.Cache.canUseCachedMeasurement;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil.dimension;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil.flexStartEdge;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexLine.calculateFlexLine;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.PixelGrid.roundLayoutResultsToPixelGrid;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.measureMode;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.TrailingPosition.needsTrailingPosition;
import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.TrailingPosition.setChildTrailingPosition;

public class CalculateLayout {
    private static final AtomicInteger gCurrentGenerationCount = new AtomicInteger(0);

    /**
     * Calculates the layout for a given node.
     *
     * @param node           The node to calculate layout for
     * @param ownerWidth     The available width for the node
     * @param ownerHeight    The available height for the node
     * @param ownerDirection The direction of the owner node
     */
    public static void calculateLayout(
            YogaNode node,
            float ownerWidth,
            float ownerHeight,
            YogaDirection ownerDirection) {
        YogaEvent.publish(YogaEventType.LAYOUT_PASS_START, node);
        LayoutData markerData = new LayoutData();

        // Increment the generation count. This will force the recursive routine to
        // visit all dirty nodes at least once. Subsequent visits will be skipped if
        // the input parameters don't change.
        gCurrentGenerationCount.incrementAndGet();
        node.processDimensions();
        YogaDirection direction = node.resolveDirection(ownerDirection);
        float width = YogaConstants.UNDEFINED;
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT;
        var style = node.getStyle();
        if (node.hasDefiniteLength(YogaDimension.WIDTH, ownerWidth)) {
            width = (node.getResolvedDimension(
                            direction,
                            FlexDirectionUtil.dimension(YogaFlexDirection.ROW),
                            ownerWidth,
                            ownerWidth)
                    .unwrap() +
                    node.getStyle().computeMarginForAxis(YogaFlexDirection.ROW, ownerWidth));
            widthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
        } else if (style
                .resolveMaxDimension(
                        direction, YogaDimension.WIDTH, ownerWidth, ownerWidth)
                .isDefined()) {
            width = style
                    .resolveMaxDimension(
                            direction, YogaDimension.WIDTH, ownerWidth, ownerWidth)
                    .unwrap();
            widthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT;
        } else {
            width = ownerWidth;
            widthSizingMode = Comparison.isUndefined(width) ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT
                    : io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
        }

        float height = YogaConstants.UNDEFINED;
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode heightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT;
        if (node.hasDefiniteLength(YogaDimension.HEIGHT, ownerHeight)) {
            height = (node.getResolvedDimension(
                            direction,
                            FlexDirectionUtil.dimension(YogaFlexDirection.COLUMN),
                            ownerHeight,
                            ownerWidth)
                    .unwrap() +
                    node.getStyle().computeMarginForAxis(YogaFlexDirection.COLUMN, ownerWidth));
            heightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
        } else if (style
                .resolveMaxDimension(
                        direction, YogaDimension.HEIGHT, ownerHeight, ownerWidth)
                .isDefined()) {
            height = style
                    .resolveMaxDimension(
                            direction, YogaDimension.HEIGHT, ownerHeight, ownerWidth)
                    .unwrap();
            heightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT;
        } else {
            height = ownerHeight;
            heightSizingMode = Comparison.isUndefined(height) ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT
                    : io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
        }
        if (calculateLayoutInternal(
                node,
                width,
                height,
                ownerDirection,
                widthSizingMode,
                heightSizingMode,
                ownerWidth,
                ownerHeight,
                true,
                LayoutPassReason.INITIAL,
                markerData,
                0, // tree root
                gCurrentGenerationCount.get())) {
            node.setPosition(node.getLayout().direction(), ownerWidth, ownerHeight);
            roundLayoutResultsToPixelGrid(node, 0.0f, 0.0f);
        }

        YogaEvent.publish(node, YogaEventType.LAYOUT_PASS_END, new YogaEvent.LayoutPassEndData(markerData));
    }

    /**
     * Internal method for calculating layout.
     *
     * @param node             The node to calculate layout for
     * @param availableWidth   The available width for the node
     * @param availableHeight  The available height for the node
     * @param ownerDirection   The direction of the owner node
     * @param widthSizingMode  The sizing mode for width
     * @param heightSizingMode The sizing mode for height
     * @param ownerWidth       The owner width
     * @param ownerHeight      The owner height
     * @param performLayout    Whether to perform layout or just measure
     * @param reason           The reason for the layout pass
     * @param layoutMarkerData Data for layout markers
     * @param depth            The current depth in the tree
     * @param generationCount  The current generation count
     * @return Whether layout was performed
     */
    public static boolean calculateLayoutInternal(
            YogaNode node,
            float availableWidth,
            float availableHeight,
            YogaDirection ownerDirection,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthSizingMode,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode heightSizingMode,
            float ownerWidth,
            float ownerHeight,
            boolean performLayout,
            LayoutPassReason reason,
            LayoutData layoutMarkerData,
            int depth,
            int generationCount) {

        LayoutResults layout = node.getLayout();

        depth++;

        boolean needToVisitNode =
                (node.isDirty() && layout.generationCount != generationCount) ||
                        layout.configVersion != node.getConfig().getVersion() ||
                        layout.lastOwnerDirection != ownerDirection;

        if (needToVisitNode) {
            // Invalidate the cached results
            layout.nextCachedMeasurementsIndex = (0);
            layout.cachedLayout.availableWidth = (-1);
            layout.cachedLayout.availableHeight = (-1);
            layout.cachedLayout.widthSizingMode = (io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT);
            layout.cachedLayout.heightSizingMode = (io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT);
            layout.cachedLayout.computedWidth = (-1);
            layout.cachedLayout.computedHeight = (-1);
        }

        CachedMeasurement cachedResults = null;

        // Determine whether the results are already cached. We maintain a separate
        // cache for layouts and measurements. A layout operation modifies the
        // positions and dimensions for nodes in the subtree. The algorithm assumes
        // that each node gets laid out a maximum of one time per tree layout, but
        // multiple measurements may be required to resolve all of the flex
        // dimensions. We handle nodes with measure functions specially here because
        // they are the most expensive to measure, so it's worth avoiding redundant
        // measurements if at all possible.
        if (node.hasMeasureFunc()) {
            float marginAxisRow =
                    node.getStyle().computeMarginForAxis(YogaFlexDirection.ROW, ownerWidth);
            float marginAxisColumn =
                    node.getStyle().computeMarginForAxis(YogaFlexDirection.COLUMN, ownerWidth);

            // First, try to use the layout cache
            if (canUseCachedMeasurement(
                    widthSizingMode,
                    availableWidth,
                    heightSizingMode,
                    availableHeight,
                    layout.cachedLayout.widthSizingMode,
                    layout.cachedLayout.availableWidth,
                    layout.cachedLayout.heightSizingMode,
                    layout.cachedLayout.availableHeight,
                    layout.cachedLayout.computedWidth,
                    layout.cachedLayout.computedHeight,
                    marginAxisRow,
                    marginAxisColumn,
                    node.getConfig())) {
                cachedResults = layout.cachedLayout;
            } else {
                // Try to use the measurement cache
                for (int i = 0; i < layout.nextCachedMeasurementsIndex; i++) {
                    if (canUseCachedMeasurement(
                            widthSizingMode,
                            availableWidth,
                            heightSizingMode,
                            availableHeight,
                            layout.cachedMeasurements[i].widthSizingMode,
                            layout.cachedMeasurements[i].availableWidth,
                            layout.cachedMeasurements[i].heightSizingMode,
                            layout.cachedMeasurements[i].availableHeight,
                            layout.cachedMeasurements[i].computedWidth,
                            layout.cachedMeasurements[i].computedHeight,
                            marginAxisRow,
                            marginAxisColumn,
                            node.getConfig())) {
                        cachedResults = layout.cachedMeasurements[i];
                        break;
                    }
                }
            }
        } else if (performLayout) {
            if (Comparison.inexactEquals(
                    layout.cachedLayout.availableWidth, availableWidth) &&
                    Comparison.inexactEquals(
                            layout.cachedLayout.availableHeight, availableHeight) &&
                    layout.cachedLayout.widthSizingMode == widthSizingMode &&
                    layout.cachedLayout.heightSizingMode == heightSizingMode) {
                cachedResults = layout.cachedLayout;
            }
        } else {
            for (int i = 0; i < layout.nextCachedMeasurementsIndex; i++) {
                if (Comparison.inexactEquals(
                        layout.cachedMeasurements[i].availableWidth, availableWidth) &&
                        Comparison.inexactEquals(
                                layout.cachedMeasurements[i].availableHeight, availableHeight) &&
                        layout.cachedMeasurements[i].widthSizingMode == widthSizingMode &&
                        layout.cachedMeasurements[i].heightSizingMode == heightSizingMode) {
                    cachedResults = layout.cachedMeasurements[i];
                    break;
                }
            }
        }

        if (!needToVisitNode && cachedResults != null) {
            layout.setMeasuredDimension(
                    YogaDimension.WIDTH, cachedResults.computedWidth);
            layout.setMeasuredDimension(
                    YogaDimension.HEIGHT, cachedResults.computedHeight);

            if (performLayout) {
                layoutMarkerData.cachedLayouts = (layoutMarkerData.cachedLayouts() + 1);
            } else {
                layoutMarkerData.cachedMeasures = (layoutMarkerData.cachedMeasures() + 1);
            }
        } else {
            calculateLayoutImpl(
                    node,
                    availableWidth,
                    availableHeight,
                    ownerDirection,
                    widthSizingMode,
                    heightSizingMode,
                    ownerWidth,
                    ownerHeight,
                    performLayout,
                    reason,
                    layoutMarkerData,
                    depth,
                    generationCount);

            layout.lastOwnerDirection = (ownerDirection);
            layout.configVersion = (node.getConfig().getVersion());

            if (cachedResults == null) {
                layoutMarkerData.maxMeasureCache = (Math.max(
                        layoutMarkerData.maxMeasureCache(),
                        layout.nextCachedMeasurementsIndex + 1));

                if (layout.nextCachedMeasurementsIndex ==
                        LayoutResults.MAX_CACHED_MEASUREMENTS) {
                    layout.nextCachedMeasurementsIndex = (0);
                }

                CachedMeasurement newCacheEntry;
                if (performLayout) {
                    // Use the single layout cache entry
                    newCacheEntry = layout.cachedLayout;
                } else {
                    // Allocate a new measurement cache entry
                    newCacheEntry =
                            layout.cachedMeasurements[layout.nextCachedMeasurementsIndex];
                    layout.nextCachedMeasurementsIndex = (
                            layout.nextCachedMeasurementsIndex + 1);
                }

                newCacheEntry.availableWidth = (availableWidth);
                newCacheEntry.availableHeight = (availableHeight);
                newCacheEntry.widthSizingMode = (widthSizingMode);
                newCacheEntry.heightSizingMode = (heightSizingMode);
                newCacheEntry.computedWidth = (
                        layout.measuredDimension(YogaDimension.WIDTH));
                newCacheEntry.computedHeight = (
                        layout.measuredDimension(YogaDimension.HEIGHT));
            }
        }

        if (performLayout) {
            node.setLayoutDimension(
                    node.getLayout().measuredDimension(YogaDimension.WIDTH),
                    YogaDimension.WIDTH);
            node.setLayoutDimension(
                    node.getLayout().measuredDimension(YogaDimension.HEIGHT),
                    YogaDimension.HEIGHT);

            node.setHasNewLayout(true);
            node.setDirty(false);
        }

        layout.generationCount = (generationCount);

        LayoutType layoutType;
        if (performLayout) {
            layoutType = !needToVisitNode && cachedResults == layout.cachedLayout
                    ? LayoutType.CACHED_LAYOUT
                    : LayoutType.LAYOUT;
        } else {
            layoutType = cachedResults != null
                    ? LayoutType.CACHED_MEASURE
                    : LayoutType.MEASURE;
        }

        YogaEvent.publish(node, YogaEventType.NODE_LAYOUT, new YogaEvent.NodeLayoutData(layoutType));

        return (needToVisitNode || cachedResults == null);
    }

    private static void calculateLayoutImpl(
            YogaNode node,
            float availableWidth,
            float availableHeight,
            YogaDirection ownerDirection,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthSizingMode,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode heightSizingMode,
            float ownerWidth,
            float ownerHeight,
            boolean performLayout,
            LayoutPassReason reason,
            LayoutData layoutMarkerData,
            int depth,
            int generationCount) {
        assert Comparison.isUndefined(availableWidth) ?
                widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT : true :
                "availableWidth is indefinite so widthSizingMode must be SizingMode.MAX_CONTENT";
        assert Comparison.isUndefined(availableHeight) ?
                heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT : true :
                "availableHeight is indefinite so heightSizingMode must be SizingMode.MAX_CONTENT";

        if (performLayout) {
            layoutMarkerData.layouts += 1;
        } else {
            layoutMarkerData.measures += 1;
        }

        // Set the resolved resolution in the node's layout.
        final YogaDirection direction = node.resolveDirection(ownerDirection);
        node.setLayoutDirection(direction);

        final YogaFlexDirection flexRowDirection =
                FlexDirectionUtil.resolveDirection(YogaFlexDirection.ROW, direction);
        final YogaFlexDirection flexColumnDirection =
                FlexDirectionUtil.resolveDirection(YogaFlexDirection.COLUMN, direction);

        final YogaPhysicalEdge startEdge =
                direction == YogaDirection.LTR ? YogaPhysicalEdge.LEFT : YogaPhysicalEdge.RIGHT;
        final YogaPhysicalEdge endEdge =
                direction == YogaDirection.LTR ? YogaPhysicalEdge.RIGHT : YogaPhysicalEdge.LEFT;

        final float marginRowLeading = node.getStyle().computeInlineStartMargin(
                flexRowDirection, direction, ownerWidth);
        node.setLayoutMargin(marginRowLeading, startEdge);
        final float marginRowTrailing = node.getStyle().computeInlineEndMargin(
                flexRowDirection, direction, ownerWidth);
        node.setLayoutMargin(marginRowTrailing, endEdge);
        final float marginColumnLeading = node.getStyle().computeInlineStartMargin(
                flexColumnDirection, direction, ownerWidth);
        node.setLayoutMargin(marginColumnLeading, YogaPhysicalEdge.TOP);
        final float marginColumnTrailing = node.getStyle().computeInlineEndMargin(
                flexColumnDirection, direction, ownerWidth);
        node.setLayoutMargin(marginColumnTrailing, YogaPhysicalEdge.BOTTOM);

        final float marginAxisRow = marginRowLeading + marginRowTrailing;
        final float marginAxisColumn = marginColumnLeading + marginColumnTrailing;

        node.setLayoutBorder(
                node.getStyle().computeInlineStartBorder(flexRowDirection, direction),
                startEdge);
        node.setLayoutBorder(
                node.getStyle().computeInlineEndBorder(flexRowDirection, direction),
                endEdge);
        node.setLayoutBorder(
                node.getStyle().computeInlineStartBorder(flexColumnDirection, direction),
                YogaPhysicalEdge.TOP);
        node.setLayoutBorder(
                node.getStyle().computeInlineEndBorder(flexColumnDirection, direction),
                YogaPhysicalEdge.BOTTOM);

        node.setLayoutPadding(
                node.getStyle().computeInlineStartPadding(
                        flexRowDirection, direction, ownerWidth),
                startEdge);
        node.setLayoutPadding(
                node.getStyle().computeInlineEndPadding(
                        flexRowDirection, direction, ownerWidth),
                endEdge);
        node.setLayoutPadding(
                node.getStyle().computeInlineStartPadding(
                        flexColumnDirection, direction, ownerWidth),
                YogaPhysicalEdge.TOP);
        node.setLayoutPadding(
                node.getStyle().computeInlineEndPadding(
                        flexColumnDirection, direction, ownerWidth),
                YogaPhysicalEdge.BOTTOM);

        if (node.hasMeasureFunc()) {
            measureNodeWithMeasureFunc(
                    node,
                    direction,
                    availableWidth - marginAxisRow,
                    availableHeight - marginAxisColumn,
                    widthSizingMode,
                    heightSizingMode,
                    ownerWidth,
                    ownerHeight,
                    layoutMarkerData,
                    reason);

            // Clean and update all display: contents nodes with a direct path to the
            // current node as they will not be traversed
            cleanupContentsNodesRecursively(node);
            return;
        }

        final int childCount = node.getLayoutChildCount();
        if (childCount == 0) {
            measureNodeWithoutChildren(
                    node,
                    direction,
                    availableWidth - marginAxisRow,
                    availableHeight - marginAxisColumn,
                    widthSizingMode,
                    heightSizingMode,
                    ownerWidth,
                    ownerHeight);

            // Clean and update all display: contents nodes with a direct path to the
            // current node as they will not be traversed
            cleanupContentsNodesRecursively(node);
            return;
        }

        // If we're not being asked to perform a full layout we can skip the algorithm
        // if we already know the size
        if (!performLayout &&
                measureNodeWithFixedSize(
                        node,
                        direction,
                        availableWidth - marginAxisRow,
                        availableHeight - marginAxisColumn,
                        widthSizingMode,
                        heightSizingMode,
                        ownerWidth,
                        ownerHeight)) {
            // Clean and update all display: contents nodes with a direct path to the
            // current node as they will not be traversed
            cleanupContentsNodesRecursively(node);
            return;
        }

        // At this point we know we're going to perform work. Ensure that each child
        // has a mutable copy.
        node.cloneChildrenIfNeeded();
        // Reset layout flags, as they could have changed.
        node.setLayoutHadOverflow(false);

        // Clean and update all display: contents nodes with a direct path to the
        // current node as they will not be traversed
        cleanupContentsNodesRecursively(node);

        // STEP 1: CALCULATE VALUES FOR REMAINDER OF ALGORITHM
        final YogaFlexDirection mainAxis =
                FlexDirectionUtil.resolveDirection(node.getStyle().getFlexDirection(), direction);
        final YogaFlexDirection crossAxis = FlexDirectionUtil.resolveCrossDirection(mainAxis, direction);
        final boolean isMainAxisRow = FlexDirectionUtil.isRow(mainAxis);
        final boolean isNodeFlexWrap = node.getStyle().getFlexWrap() != YogaWrap.NO_WRAP;

        final float mainAxisOwnerSize = isMainAxisRow ? ownerWidth : ownerHeight;
        final float crossAxisOwnerSize = isMainAxisRow ? ownerHeight : ownerWidth;

        final float paddingAndBorderAxisMain =
                io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.BoundAxis.paddingAndBorderForAxis(node, mainAxis, direction, ownerWidth);
        final float paddingAndBorderAxisCross =
                BoundAxis.paddingAndBorderForAxis(node, crossAxis, direction, ownerWidth);
        final float leadingPaddingAndBorderCross =
                node.getStyle().computeFlexStartPaddingAndBorder(
                        crossAxis, direction, ownerWidth);

        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode sizingModeMainDim =
                isMainAxisRow ? widthSizingMode : heightSizingMode;
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode sizingModeCrossDim =
                isMainAxisRow ? heightSizingMode : widthSizingMode;

        final float paddingAndBorderAxisRow =
                isMainAxisRow ? paddingAndBorderAxisMain : paddingAndBorderAxisCross;
        final float paddingAndBorderAxisColumn =
                isMainAxisRow ? paddingAndBorderAxisCross : paddingAndBorderAxisMain;

        // STEP 2: DETERMINE AVAILABLE SIZE IN MAIN AND CROSS DIRECTIONS

        float availableInnerWidth = calculateAvailableInnerDimension(
                node,
                direction,
                YogaDimension.WIDTH,
                availableWidth - marginAxisRow,
                paddingAndBorderAxisRow,
                ownerWidth,
                ownerWidth);
        float availableInnerHeight = calculateAvailableInnerDimension(
                node,
                direction,
                YogaDimension.HEIGHT,
                availableHeight - marginAxisColumn,
                paddingAndBorderAxisColumn,
                ownerHeight,
                ownerWidth);

        float availableInnerMainDim =
                isMainAxisRow ? availableInnerWidth : availableInnerHeight;
        final float availableInnerCrossDim =
                isMainAxisRow ? availableInnerHeight : availableInnerWidth;

        // STEP 3: DETERMINE FLEX BASIS FOR EACH ITEM

        // Computed basis + margins + gap
        float totalMainDim = 0;
        totalMainDim += computeFlexBasisForChildren(
                node,
                availableInnerWidth,
                availableInnerHeight,
                widthSizingMode,
                heightSizingMode,
                direction,
                mainAxis,
                performLayout,
                layoutMarkerData,
                depth,
                generationCount);

        if (childCount > 1) {
            totalMainDim +=
                    node.getStyle().computeGapForAxis(mainAxis, availableInnerMainDim) *
                            (float) (childCount - 1);
        }

        final boolean mainAxisOverflows =
                (sizingModeMainDim != io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT) &&
                        totalMainDim > availableInnerMainDim;

        if (isNodeFlexWrap && mainAxisOverflows &&
                sizingModeMainDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT) {
            sizingModeMainDim = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
        }
        // STEP 4: COLLECT FLEX ITEMS INTO FLEX LINES

        // Iterator representing the beginning of the current line
        var startOfLineIterator = node.getLayoutChildren().iterator();

        // Number of lines.
        int lineCount = 0;

        // Accumulated cross dimensions of all lines so far.
        float totalLineCrossDim = 0;

        final float crossAxisGap =
                node.getStyle().computeGapForAxis(crossAxis, availableInnerCrossDim);

        // Max main dimension of all the lines.
        float maxLineMainDim = 0;
        for (; startOfLineIterator.hasNext(); lineCount++) {
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexLine flexLine = calculateFlexLine(
                    node,
                    ownerDirection,
                    ownerWidth,
                    mainAxisOwnerSize,
                    availableInnerWidth,
                    availableInnerMainDim,
                    startOfLineIterator,
                    lineCount);

            // If we don't need to measure the cross axis, we can skip the entire flex
            // step.
            final boolean canSkipFlex =
                    !performLayout && sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;

            // STEP 5: RESOLVING FLEXIBLE LENGTHS ON MAIN AXIS
            // Calculate the remaining available space that needs to be allocated. If
            // the main dimension size isn't known, it is computed based on the line
            // length, so there's no more space left to distribute.

            boolean sizeBasedOnContent = false;
            // If we don't measure with exact main dimension we want to ensure we don't
            // violate min and max
            if (sizingModeMainDim != io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT) {
                final var style = node.getStyle();
                final float minInnerWidth =
                        style
                                .resolveMinDimension(
                                        direction, YogaDimension.WIDTH, ownerWidth, ownerWidth)
                                .unwrap() -
                                paddingAndBorderAxisRow;
                final float maxInnerWidth =
                        style
                                .resolveMaxDimension(
                                        direction, YogaDimension.WIDTH, ownerWidth, ownerWidth)
                                .unwrap() -
                                paddingAndBorderAxisRow;
                final float minInnerHeight =
                        style
                                .resolveMinDimension(
                                        direction, YogaDimension.HEIGHT, ownerHeight, ownerWidth)
                                .unwrap() -
                                paddingAndBorderAxisColumn;
                final float maxInnerHeight =
                        style
                                .resolveMaxDimension(
                                        direction, YogaDimension.HEIGHT, ownerHeight, ownerWidth)
                                .unwrap() -
                                paddingAndBorderAxisColumn;

                final float minInnerMainDim =
                        isMainAxisRow ? minInnerWidth : minInnerHeight;
                final float maxInnerMainDim =
                        isMainAxisRow ? maxInnerWidth : maxInnerHeight;

                if (Comparison.isDefined(minInnerMainDim) &&
                        flexLine.getSizeConsumed() < minInnerMainDim) {
                    availableInnerMainDim = minInnerMainDim;
                } else if (
                        Comparison.isDefined(maxInnerMainDim) &&
                                flexLine.getSizeConsumed() > maxInnerMainDim) {
                    availableInnerMainDim = maxInnerMainDim;
                } else {
                    boolean useLegacyStretchBehaviour =
                            node.hasErrata(YogaErrata.STRETCH_FLEX_BASIS);

                    if (!useLegacyStretchBehaviour &&
                            ((Comparison.isDefined(flexLine.getLayout().getTotalFlexGrowFactors()) &&
                                    flexLine.getLayout().getTotalFlexGrowFactors() == 0) ||
                                    (Comparison.isDefined(node.resolveFlexGrow()) &&
                                            node.resolveFlexGrow() == 0))) {
                        // If we don't have any children to flex or we can't flex the node
                        // itself, space we've used is all space we need. Root node also
                        // should be shrunk to minimum
                        availableInnerMainDim = flexLine.getSizeConsumed();
                    }

                    sizeBasedOnContent = !useLegacyStretchBehaviour;
                }
            }

            if (!sizeBasedOnContent && Comparison.isDefined(availableInnerMainDim)) {
                flexLine.getLayout().setRemainingFreeSpace(
                        availableInnerMainDim - flexLine.getSizeConsumed());
            } else if (flexLine.getSizeConsumed() < 0) {
                // availableInnerMainDim is indefinite which means the node is being sized
                // based on its content. sizeConsumed is negative which means
                // the node will allocate 0 points for its content. Consequently,
                // remainingFreeSpace is 0 - sizeConsumed.
                flexLine.getLayout().setRemainingFreeSpace(-flexLine.getSizeConsumed());
            }

            if (!canSkipFlex) {
                resolveFlexibleLength(
                        node,
                        flexLine,
                        mainAxis,
                        crossAxis,
                        direction,
                        ownerWidth,
                        mainAxisOwnerSize,
                        availableInnerMainDim,
                        availableInnerCrossDim,
                        availableInnerWidth,
                        availableInnerHeight,
                        mainAxisOverflows,
                        sizingModeCrossDim,
                        performLayout,
                        layoutMarkerData,
                        depth,
                        generationCount);
            }

            node.setLayoutHadOverflow(
                    node.getLayout().hadOverflow() ||
                            (flexLine.getLayout().getRemainingFreeSpace() < 0));

            // STEP 6: MAIN-AXIS JUSTIFICATION & CROSS-AXIS SIZE DETERMINATION

            // At this point, all the children have their dimensions set in the main
            // axis. Their dimensions are also set in the cross axis with the exception
            // of items that are aligned "stretch". We need to compute these stretch
            // values and set the final positions.

            justifyMainAxis(
                    node,
                    flexLine,
                    mainAxis,
                    crossAxis,
                    direction,
                    sizingModeMainDim,
                    sizingModeCrossDim,
                    mainAxisOwnerSize,
                    ownerWidth,
                    availableInnerMainDim,
                    availableInnerCrossDim,
                    availableInnerWidth,
                    performLayout);

            float containerCrossAxis = availableInnerCrossDim;
            if (sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT ||
                    sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT) {
                // Compute the cross axis from the max cross dimension of the children.
                containerCrossAxis =
                        boundAxis(
                                node,
                                crossAxis,
                                direction,
                                flexLine.getLayout().getCrossDim() + paddingAndBorderAxisCross,
                                crossAxisOwnerSize,
                                ownerWidth) -
                                paddingAndBorderAxisCross;
            }

            // If there's no flex wrap, the cross dimension is defined by the container.
            if (!isNodeFlexWrap && sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT) {
                flexLine.getLayout().setCrossDim(availableInnerCrossDim);
            }

            // As-per https://www.w3.org/TR/css-flexbox-1/#cross-sizing, the
            // cross-size of the line within a single-line container should be bound to
            // min/max constraints before alignment within the line. In a multi-line
            // container, affecting alignment between the lines.
            if (!isNodeFlexWrap) {
                flexLine.getLayout().setCrossDim(
                        boundAxis(
                                node,
                                crossAxis,
                                direction,
                                flexLine.getLayout().getCrossDim() + paddingAndBorderAxisCross,
                                crossAxisOwnerSize,
                                ownerWidth) -
                                paddingAndBorderAxisCross);
            }

            // STEP 7: CROSS-AXIS ALIGNMENT
            // We can skip child alignment if we're just measuring the container.
            if (performLayout) {
                for (YogaNode child : flexLine.getItemsInFlow()) {
                    float leadingCrossDim = leadingPaddingAndBorderCross;

                    // For a relative children, we're either using alignItems (owner) or
                    // alignSelf (child) in order to determine the position in the cross
                    // axis
                    final YogaAlign alignItem = resolveChildAlignment(node, child);

                    // If the child uses align stretch, we need to lay it out one more
                    // time, this time forcing the cross-axis size to be the computed
                    // cross size for the current line.
                    if (alignItem == YogaAlign.STRETCH &&
                            !child.getStyle().flexStartMarginIsAuto(crossAxis, direction) &&
                            !child.getStyle().flexEndMarginIsAuto(crossAxis, direction)) {
                        // If the child defines a definite size for its cross axis, there's
                        // no need to stretch.
                        if (!child.hasDefiniteLength(
                                FlexDirectionUtil.dimension(crossAxis), availableInnerCrossDim)) {
                            float childMainSize =
                                    child.getLayout().measuredDimension(FlexDirectionUtil.dimension(mainAxis));
                            final var childStyle = child.getStyle();
                            float childCrossSize = childStyle.getAspectRatio().isDefined()
                                    ? child.getStyle().computeMarginForAxis(
                                    crossAxis, availableInnerWidth) +
                                    (isMainAxisRow
                                            ? childMainSize / childStyle.getAspectRatio().unwrap()
                                            : childMainSize * childStyle.getAspectRatio().unwrap())
                                    : flexLine.getLayout().getCrossDim();

                            childMainSize += child.getStyle().computeMarginForAxis(
                                    mainAxis, availableInnerWidth);

                            var maxSizeConstraint = new MaxSizeConstraint();
                            maxSizeConstraint.constrainForMode(
                                    child,
                                    direction,
                                    mainAxis,
                                    availableInnerMainDim,
                                    availableInnerWidth,
                                    io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT,
                                    childMainSize
                            );
                            childMainSize = maxSizeConstraint.size;
                            maxSizeConstraint.constrainForMode(
                                    child,
                                    direction,
                                    crossAxis,
                                    availableInnerCrossDim,
                                    availableInnerWidth,
                                    io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT,
                                    childCrossSize);
                            childCrossSize = maxSizeConstraint.size;

                            final float childWidth =
                                    isMainAxisRow ? childMainSize : childCrossSize;
                            final float childHeight =
                                    !isMainAxisRow ? childMainSize : childCrossSize;

                            final YogaAlign alignContent = node.getStyle().getAlignContent();
                            final boolean crossAxisDoesNotGrow =
                                    alignContent != YogaAlign.STRETCH && isNodeFlexWrap;
                            final io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childWidthSizingMode =
                                    Comparison.isUndefined(childWidth) ||
                                            (!isMainAxisRow && crossAxisDoesNotGrow)
                                            ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT
                                            : io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
                            final io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childHeightSizingMode =
                                    Comparison.isUndefined(childHeight) ||
                                            (isMainAxisRow && crossAxisDoesNotGrow)
                                            ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT
                                            : io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;

                            calculateLayoutInternal(
                                    child,
                                    childWidth,
                                    childHeight,
                                    direction,
                                    childWidthSizingMode,
                                    childHeightSizingMode,
                                    availableInnerWidth,
                                    availableInnerHeight,
                                    true,
                                    LayoutPassReason.STRETCH,
                                    layoutMarkerData,
                                    depth,
                                    generationCount);
                        }
                    } else {
                        final float remainingCrossDim = containerCrossAxis -
                                child.dimensionWithMargin(crossAxis, availableInnerWidth);

                        if (child.getStyle().flexStartMarginIsAuto(crossAxis, direction) &&
                                child.getStyle().flexEndMarginIsAuto(crossAxis, direction)) {
                            leadingCrossDim += Comparison.maxOrDefined(0.0f, remainingCrossDim / 2);
                        } else if (child.getStyle().flexEndMarginIsAuto(crossAxis, direction)) {
                            // No-Op
                        } else if (child.getStyle().flexStartMarginIsAuto(
                                crossAxis, direction)) {
                            leadingCrossDim += Comparison.maxOrDefined(0.0f, remainingCrossDim);
                        } else if (alignItem == YogaAlign.FLEX_START) {
                            // No-Op
                        } else if (alignItem == YogaAlign.CENTER) {
                            leadingCrossDim += remainingCrossDim / 2;
                        } else {
                            leadingCrossDim += remainingCrossDim;
                        }
                    }
                    // And we apply the position
                    child.setLayoutPosition(
                            child.getLayout().position(flexStartEdge(crossAxis)) +
                                    totalLineCrossDim + leadingCrossDim,
                            flexStartEdge(crossAxis));
                }
            }

            final float appliedCrossGap = lineCount != 0 ? crossAxisGap : 0.0f;
            totalLineCrossDim += flexLine.getLayout().getCrossDim() + appliedCrossGap;
            maxLineMainDim =
                    Comparison.maxOrDefined(maxLineMainDim, flexLine.getLayout().getMainDim());
        }

        // STEP 8: MULTI-LINE CONTENT ALIGNMENT
        // currentLead stores the size of the cross dim
        if (performLayout && (isNodeFlexWrap || isBaselineLayout(node))) {
            float leadPerLine = 0;
            float currentLead = leadingPaddingAndBorderCross;
            float extraSpacePerLine = 0;

            final float unclampedCrossDim = sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT
                    ? availableInnerCrossDim + paddingAndBorderAxisCross
                    : node.hasDefiniteLength(FlexDirectionUtil.dimension(crossAxis), crossAxisOwnerSize)
                    ? node.getResolvedDimension(
                            direction,
                            FlexDirectionUtil.dimension(crossAxis),
                            crossAxisOwnerSize,
                            ownerWidth)
                    .unwrap()
                    : totalLineCrossDim + paddingAndBorderAxisCross;

            final float innerCrossDim = boundAxis(
                    node,
                    crossAxis,
                    direction,
                    unclampedCrossDim,
                    crossAxisOwnerSize,
                    ownerWidth) -
                    paddingAndBorderAxisCross;

            final float remainingAlignContentDim = innerCrossDim - totalLineCrossDim;

            final YogaAlign alignContent = remainingAlignContentDim >= 0
                    ? node.getStyle().getAlignContent()
                    : fallbackAlignment(node.getStyle().getAlignContent());

            switch (alignContent) {
                case FLEX_END:
                    currentLead += remainingAlignContentDim;
                    break;
                case CENTER:
                    currentLead += remainingAlignContentDim / 2;
                    break;
                case STRETCH:
                    extraSpacePerLine =
                            remainingAlignContentDim / (float) (lineCount);
                    break;
                case SPACE_AROUND:
                    currentLead +=
                            remainingAlignContentDim / (2 * (float) (lineCount));
                    leadPerLine = remainingAlignContentDim / (float) (lineCount);
                    break;
                case SPACE_EVENLY:
                    currentLead +=
                            remainingAlignContentDim / (float) (lineCount + 1);
                    leadPerLine =
                            remainingAlignContentDim / (float) (lineCount + 1);
                    break;
                case SPACE_BETWEEN:
                    if (lineCount > 1) {
                        leadPerLine =
                                remainingAlignContentDim / (float) (lineCount - 1);
                    }
                    break;
                case AUTO:
                case FLEX_START:
                case BASELINE:
                    break;
            }
            var endIterator = node.getLayoutChildren().iterator();
            for (int i = 0; i < lineCount; i++) {
                var startIterator = endIterator.copy();
                var iterator = startIterator.copy();

                // compute the line's height and find the endIndex
                float lineHeight = 0;
                float maxAscentForCurrentLine = 0;
                float maxDescentForCurrentLine = 0;
                for (; iterator.hasNext(); iterator.next()) {
                    final YogaNode child = iterator.current();
                    if (child.getStyle().getDisplay() == YogaDisplay.NONE) {
                        continue;
                    }
                    if (child.getStyle().getPositionType() != YogaPositionType.ABSOLUTE) {
                        if (child.getLineIndex() != i) {
                            break;
                        }
                        if (child.isLayoutDimensionDefined(crossAxis)) {
                            lineHeight = Comparison.maxOrDefined(
                                    lineHeight,
                                    child.getLayout().measuredDimension(FlexDirectionUtil.dimension(crossAxis)) +
                                            child.getStyle().computeMarginForAxis(
                                                    crossAxis, availableInnerWidth));
                        }
                        if (resolveChildAlignment(node, child) == YogaAlign.BASELINE) {
                            final float ascent = calculateBaseline(child) +
                                    child.getStyle().computeFlexStartMargin(
                                            YogaFlexDirection.COLUMN, direction, availableInnerWidth);
                            final float descent =
                                    child.getLayout().measuredDimension(YogaDimension.HEIGHT) +
                                            child.getStyle().computeMarginForAxis(
                                                    YogaFlexDirection.COLUMN, availableInnerWidth) -
                                            ascent;
                            maxAscentForCurrentLine =
                                    Comparison.maxOrDefined(maxAscentForCurrentLine, ascent);
                            maxDescentForCurrentLine =
                                    Comparison.maxOrDefined(maxDescentForCurrentLine, descent);
                            lineHeight = Comparison.maxOrDefined(
                                    lineHeight, maxAscentForCurrentLine + maxDescentForCurrentLine);
                        }
                    }
                }
                endIterator = iterator.copy();
                currentLead += i != 0 ? crossAxisGap : 0;
                lineHeight += extraSpacePerLine;

                for (iterator = startIterator; !iterator.equals(endIterator); iterator.next()) {
                    final YogaNode child = iterator.current();
                    if (child.getStyle().getDisplay() == YogaDisplay.NONE) {
                        continue;
                    }
                    if (child.getStyle().getPositionType() != YogaPositionType.ABSOLUTE) {
                        switch (resolveChildAlignment(node, child)) {
                            case FLEX_START: {
                                child.setLayoutPosition(
                                        currentLead +
                                                child.getStyle().computeFlexStartPosition(
                                                        crossAxis, direction, availableInnerWidth),
                                        flexStartEdge(crossAxis));
                                break;
                            }
                            case FLEX_END: {
                                child.setLayoutPosition(
                                        currentLead + lineHeight -
                                                child.getStyle().computeFlexEndMargin(
                                                        crossAxis, direction, availableInnerWidth) -
                                                child.getLayout().measuredDimension(
                                                        FlexDirectionUtil.dimension(crossAxis)),
                                        flexStartEdge(crossAxis));
                                break;
                            }
                            case CENTER: {
                                float childHeight =
                                        child.getLayout().measuredDimension(FlexDirectionUtil.dimension(crossAxis));

                                child.setLayoutPosition(
                                        currentLead + (lineHeight - childHeight) / 2,
                                        flexStartEdge(crossAxis));
                                break;
                            }
                            case STRETCH: {
                                child.setLayoutPosition(
                                        currentLead +
                                                child.getStyle().computeFlexStartMargin(
                                                        crossAxis, direction, availableInnerWidth),
                                        flexStartEdge(crossAxis));

                                // Remeasure child with the line height as it as been only
                                // measured with the owners height yet.
                                if (!child.hasDefiniteLength(
                                        FlexDirectionUtil.dimension(crossAxis), availableInnerCrossDim)) {
                                    final float childWidth = isMainAxisRow
                                            ? (child.getLayout().measuredDimension(YogaDimension.WIDTH) +
                                            child.getStyle().computeMarginForAxis(
                                                    mainAxis, availableInnerWidth))
                                            : leadPerLine + lineHeight;

                                    final float childHeight = !isMainAxisRow
                                            ? (child.getLayout().measuredDimension(YogaDimension.HEIGHT) +
                                            child.getStyle().computeMarginForAxis(
                                                    crossAxis, availableInnerWidth))
                                            : leadPerLine + lineHeight;

                                    if (!(Comparison.inexactEquals(
                                            childWidth,
                                            child.getLayout().measuredDimension(
                                                    YogaDimension.WIDTH)) &&
                                            Comparison.inexactEquals(
                                                    childHeight,
                                                    child.getLayout().measuredDimension(
                                                            YogaDimension.HEIGHT)))) {
                                        calculateLayoutInternal(
                                                child,
                                                childWidth,
                                                childHeight,
                                                direction,
                                                io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT,
                                                io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT,
                                                availableInnerWidth,
                                                availableInnerHeight,
                                                true,
                                                LayoutPassReason.MULTILINE_STRETCH,
                                                layoutMarkerData,
                                                depth,
                                                generationCount);
                                    }
                                }
                                break;
                            }
                            case BASELINE: {
                                child.setLayoutPosition(
                                        currentLead + maxAscentForCurrentLine -
                                                calculateBaseline(child) +
                                                child.getStyle().computeFlexStartPosition(
                                                        YogaFlexDirection.COLUMN,
                                                        direction,
                                                        availableInnerCrossDim),
                                        YogaPhysicalEdge.TOP);

                                break;
                            }
                            case AUTO:
                            case SPACE_BETWEEN:
                            case SPACE_AROUND:
                            case SPACE_EVENLY:
                                break;
                        }
                    }
                }

                currentLead = currentLead + leadPerLine + lineHeight;
            }
        }

        // STEP 9: COMPUTING FINAL DIMENSIONS

        node.setLayoutMeasuredDimension(
                boundAxis(
                        node,
                        YogaFlexDirection.ROW,
                        direction,
                        availableWidth - marginAxisRow,
                        ownerWidth,
                        ownerWidth),
                YogaDimension.WIDTH);

        node.setLayoutMeasuredDimension(
                boundAxis(
                        node,
                        YogaFlexDirection.COLUMN,
                        direction,
                        availableHeight - marginAxisColumn,
                        ownerHeight,
                        ownerWidth),
                YogaDimension.HEIGHT);

        // If the user didn't specify a width or height for the node, set the
        // dimensions based on the children.
        if (sizingModeMainDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT ||
                (node.getStyle().getOverflow() != YogaOverflow.SCROLL &&
                        sizingModeMainDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT)) {
            // Clamp the size to the min/max size, if specified, and make sure it
            // doesn't go below the padding and border amount.
            node.setLayoutMeasuredDimension(
                    boundAxis(
                            node,
                            mainAxis,
                            direction,
                            maxLineMainDim,
                            mainAxisOwnerSize,
                            ownerWidth),
                    FlexDirectionUtil.dimension(mainAxis));

        } else if (
                sizingModeMainDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT &&
                        node.getStyle().getOverflow() == YogaOverflow.SCROLL) {
            node.setLayoutMeasuredDimension(
                    Comparison.maxOrDefined(
                            Comparison.minOrDefined(
                                    availableInnerMainDim + paddingAndBorderAxisMain,
                                    boundAxisWithinMinAndMax(
                                            node,
                                            direction,
                                            mainAxis,
                                            FloatOptional.of(maxLineMainDim),
                                            mainAxisOwnerSize,
                                            ownerWidth)
                                            .unwrap()),
                            paddingAndBorderAxisMain),
                    FlexDirectionUtil.dimension(mainAxis));
        }

        if (sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT ||
                (node.getStyle().getOverflow() != YogaOverflow.SCROLL &&
                        sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT)) {
            // Clamp the size to the min/max size, if specified, and make sure it
            // doesn't go below the padding and border amount.
            node.setLayoutMeasuredDimension(
                    boundAxis(
                            node,
                            crossAxis,
                            direction,
                            totalLineCrossDim + paddingAndBorderAxisCross,
                            crossAxisOwnerSize,
                            ownerWidth),
                    FlexDirectionUtil.dimension(crossAxis));

        } else if (
                sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT &&
                        node.getStyle().getOverflow() == YogaOverflow.SCROLL) {
            node.setLayoutMeasuredDimension(
                    Comparison.maxOrDefined(
                            Comparison.minOrDefined(
                                    availableInnerCrossDim + paddingAndBorderAxisCross,
                                    boundAxisWithinMinAndMax(
                                            node,
                                            direction,
                                            crossAxis,
                                            FloatOptional.of(
                                                    totalLineCrossDim + paddingAndBorderAxisCross),
                                            crossAxisOwnerSize,
                                            ownerWidth)
                                            .unwrap()),
                            paddingAndBorderAxisCross),
                    FlexDirectionUtil.dimension(crossAxis));
        }

        // As we only wrapped in normal direction yet, we need to reverse the
        // positions on wrap-reverse.
        if (performLayout && node.getStyle().getFlexWrap() == YogaWrap.WRAP_REVERSE) {
            for (YogaNode child : node.getLayoutChildren()) {
                if (child.getStyle().getPositionType() != YogaPositionType.ABSOLUTE) {
                    child.setLayoutPosition(
                            node.getLayout().measuredDimension(FlexDirectionUtil.dimension(crossAxis)) -
                                    child.getLayout().position(flexStartEdge(crossAxis)) -
                                    child.getLayout().measuredDimension(FlexDirectionUtil.dimension(crossAxis)),
                            flexStartEdge(crossAxis));
                }
            }
        }

        if (performLayout) {
            // STEP 10: SETTING TRAILING POSITIONS FOR CHILDREN
            final boolean needsMainTrailingPos = needsTrailingPosition(mainAxis);
            final boolean needsCrossTrailingPos = needsTrailingPosition(crossAxis);

            if (needsMainTrailingPos || needsCrossTrailingPos) {
                for (YogaNode child : node.getLayoutChildren()) {
                    // Absolute children will be handled by their containing block since we
                    // cannot guarantee that their positions are set when their parents are
                    // done with layout.
                    if (child.getStyle().getDisplay() == YogaDisplay.NONE ||
                            child.getStyle().getPositionType() == YogaPositionType.ABSOLUTE) {
                        continue;
                    }
                    if (needsMainTrailingPos) {
                        setChildTrailingPosition(node, child, mainAxis);
                    }

                    if (needsCrossTrailingPos) {
                        setChildTrailingPosition(node, child, crossAxis);
                    }
                }
            }

            // STEP 11: SIZING AND POSITIONING ABSOLUTE CHILDREN
            // Let the containing block layout its absolute descendants.
            if (node.getStyle().getPositionType() != YogaPositionType.STATIC ||
                    node.alwaysFormsContainingBlock() || depth == 1) {
                layoutAbsoluteDescendants(
                        node,
                        node,
                        isMainAxisRow ? sizingModeMainDim : sizingModeCrossDim,
                        direction,
                        layoutMarkerData,
                        depth,
                        generationCount,
                        0.0f,
                        0.0f,
                        availableInnerWidth,
                        availableInnerHeight);
            }
        }
    }

    private static class MaxSizeConstraint {
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode mode;
        float size;

        public void constrainForMode(
                YogaNode node,
                YogaDirection direction,
                YogaFlexDirection axis,
                float ownerAxisSize,
                float ownerWidth,
                io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode mode,
                float size) {
            this.mode = mode;
            this.size = size;

            FloatOptional maxSize = node.getStyle().resolveMaxDimension(
                            direction,
                            dimension(axis),
                            ownerAxisSize,
                            ownerWidth)
                    .add(FloatOptional.of(node.getStyle().computeMarginForAxis(axis, ownerWidth)));

            switch (this.mode) {
                case STRETCH_FIT:
                case FIT_CONTENT:
                    if (maxSize.isUndefined() || this.size < maxSize.unwrap()) {
                        // size remains unchanged
                    } else {
                        this.size = maxSize.unwrap();
                    }
                    break;
                case MAX_CONTENT:
                    if (maxSize.isDefined()) {
                        this.mode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT;
                        this.size = maxSize.unwrap();
                    }
                    break;
            }
        }
    }

    private static void computeFlexBasisForChild(
            YogaNode node,
            YogaNode child,
            float width,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthMode,
            float height,
            float ownerWidth,
            float ownerHeight,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode heightMode,
            YogaDirection direction,
            LayoutData layoutMarkerData,
            int depth,
            int generationCount) {

        final YogaFlexDirection mainAxis =
                FlexDirectionUtil.resolveDirection(node.getStyle().getFlexDirection(), direction);
        final boolean isMainAxisRow = FlexDirectionUtil.isRow(mainAxis);
        final float mainAxisSize = isMainAxisRow ? width : height;
        final float mainAxisOwnerSize = isMainAxisRow ? ownerWidth : ownerHeight;

        float childWidth = Float.NaN;
        float childHeight = Float.NaN;
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childWidthSizingMode;
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childHeightSizingMode;

        final FloatOptional resolvedFlexBasis = child.resolveFlexBasis(
                direction, mainAxis, mainAxisOwnerSize, ownerWidth);
        final boolean isRowStyleDimDefined =
                child.hasDefiniteLength(YogaDimension.WIDTH, ownerWidth);
        final boolean isColumnStyleDimDefined =
                child.hasDefiniteLength(YogaDimension.HEIGHT, ownerHeight);

        if (resolvedFlexBasis.isDefined() && Comparison.isDefined(mainAxisSize)) {
            if (child.getLayout().computedFlexBasis.isUndefined() ||
                    (child.getConfig().isExperimentalFeatureEnabled(
                            YogaExperimentalFeature.WEB_FLEX_BASIS) &&
                            child.getLayout().computedFlexBasisGeneration != generationCount)) {
                final FloatOptional paddingAndBorder = FloatOptional.of(
                        paddingAndBorderForAxis(child, mainAxis, direction, ownerWidth));
                child.setLayoutComputedFlexBasis(
                        resolvedFlexBasis.maxOrDefined(paddingAndBorder));
            }
        } else if (isMainAxisRow && isRowStyleDimDefined) {
            // The width is definite, so use that as the flex basis.
            final FloatOptional paddingAndBorder =
                    FloatOptional.of(paddingAndBorderForAxis(
                            child, YogaFlexDirection.ROW, direction, ownerWidth));

            child.setLayoutComputedFlexBasis(
                    child.getResolvedDimension(
                            direction, YogaDimension.WIDTH, ownerWidth, ownerWidth).maxOrDefined(
                            paddingAndBorder));
        } else if (!isMainAxisRow && isColumnStyleDimDefined) {
            // The height is definite, so use that as the flex basis.
            final FloatOptional paddingAndBorder =
                    FloatOptional.of(paddingAndBorderForAxis(
                            child, YogaFlexDirection.COLUMN, direction, ownerWidth));
            child.setLayoutComputedFlexBasis(
                    child.getResolvedDimension(
                            direction, YogaDimension.HEIGHT, ownerHeight, ownerWidth).maxOrDefined(
                            paddingAndBorder));
        } else {
            // Compute the flex basis and hypothetical main size (i.e. the clamped flex basis).
            childWidthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT;
            childHeightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT;

            float marginRow =
                    child.getStyle().computeMarginForAxis(YogaFlexDirection.ROW, ownerWidth);
            float marginColumn =
                    child.getStyle().computeMarginForAxis(YogaFlexDirection.COLUMN, ownerWidth);

            if (isRowStyleDimDefined) {
                childWidth = child
                        .getResolvedDimension(
                                direction, YogaDimension.WIDTH, ownerWidth, ownerWidth)
                        .unwrap() +
                        marginRow;
                childWidthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            }
            if (isColumnStyleDimDefined) {
                childHeight =
                        child
                                .getResolvedDimension(
                                        direction, YogaDimension.HEIGHT, ownerHeight, ownerWidth)
                                .unwrap() +
                                marginColumn;
                childHeightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            }

            // The W3C spec doesn't say anything about the 'overflow' property, but all
            // major browsers appear to implement the following logic.
            if ((!isMainAxisRow && node.getStyle().getOverflow() == YogaOverflow.SCROLL) ||
                    node.getStyle().getOverflow() != YogaOverflow.SCROLL) {
                if (Comparison.isUndefined(childWidth) && Comparison.isDefined(width)) {
                    childWidth = width;
                    childWidthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT;
                }
            }

            if ((isMainAxisRow && node.getStyle().getOverflow() == YogaOverflow.SCROLL) ||
                    node.getStyle().getOverflow() != YogaOverflow.SCROLL) {
                if (Comparison.isUndefined(childHeight) && Comparison.isDefined(height)) {
                    childHeight = height;
                    childHeightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT;
                }
            }

            final var childStyle = child.getStyle();
            if (childStyle.getAspectRatio().isDefined()) {
                if (!isMainAxisRow && childWidthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT) {
                    childHeight = marginColumn +
                            (childWidth - marginRow) / childStyle.getAspectRatio().unwrap();
                    childHeightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
                } else if (
                        isMainAxisRow && childHeightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT) {
                    childWidth = marginRow +
                            (childHeight - marginColumn) * childStyle.getAspectRatio().unwrap();
                    childWidthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
                }
            }

            // If child has no defined size in the cross axis and is set to stretch, set
            // the cross axis to be measured exactly with the available inner width

            final boolean hasExactWidth =
                    Comparison.isDefined(width) && widthMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            final boolean childWidthStretch =
                    resolveChildAlignment(node, child) == YogaAlign.STRETCH &&
                            childWidthSizingMode != io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            if (!isMainAxisRow && !isRowStyleDimDefined && hasExactWidth &&
                    childWidthStretch) {
                childWidth = width;
                childWidthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
                if (childStyle.getAspectRatio().isDefined()) {
                    childHeight =
                            (childWidth - marginRow) / childStyle.getAspectRatio().unwrap();
                    childHeightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
                }
            }

            final boolean hasExactHeight =
                    Comparison.isDefined(height) && heightMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            final boolean childHeightStretch =
                    resolveChildAlignment(node, child) == YogaAlign.STRETCH &&
                            childHeightSizingMode != io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            if (isMainAxisRow && !isColumnStyleDimDefined && hasExactHeight &&
                    childHeightStretch) {
                childHeight = height;
                childHeightSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;

                if (childStyle.getAspectRatio().isDefined()) {
                    childWidth =
                            (childHeight - marginColumn) * childStyle.getAspectRatio().unwrap();
                    childWidthSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
                }
            }

            var maxSizeConstraint = new MaxSizeConstraint();
            maxSizeConstraint.constrainForMode(
                    child,
                    direction,
                    YogaFlexDirection.ROW,
                    ownerWidth,
                    ownerWidth,
                    childWidthSizingMode,
                    childWidth);
            childWidthSizingMode = maxSizeConstraint.mode;
            childWidth = maxSizeConstraint.size;
            maxSizeConstraint.constrainForMode(
                    child,
                    direction,
                    YogaFlexDirection.COLUMN,
                    ownerHeight,
                    ownerWidth,
                    childHeightSizingMode,
                    childHeight);
            childHeightSizingMode = maxSizeConstraint.mode;
            childHeight = maxSizeConstraint.size;

            // Measure the child
            calculateLayoutInternal(
                    child,
                    childWidth,
                    childHeight,
                    direction,
                    childWidthSizingMode,
                    childHeightSizingMode,
                    ownerWidth,
                    ownerHeight,
                    false,
                    LayoutPassReason.MEASURE_CHILD,
                    layoutMarkerData,
                    depth,
                    generationCount);

            child.setLayoutComputedFlexBasis(FloatOptional.of(Comparison.maxOrDefined(
                    child.getLayout().measuredDimension(dimension(mainAxis)),
                    paddingAndBorderForAxis(child, mainAxis, direction, ownerWidth))));
        }
        child.setLayoutComputedFlexBasisGeneration(generationCount);
    }

    private static void measureNodeWithMeasureFunc(
            YogaNode node,
            YogaDirection direction,
            float availableWidth,
            float availableHeight,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthSizingMode,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode heightSizingMode,
            float ownerWidth,
            float ownerHeight,
            LayoutData layoutMarkerData,
            LayoutPassReason reason) {
        assert node.hasMeasureFunc() : "Expected node to have custom measure function";

        if (widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT) {
            availableWidth = Float.NaN;
        }
        if (heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT) {
            availableHeight = Float.NaN;
        }

        final LayoutResults layout = node.getLayout();
        final float paddingAndBorderAxisRow = layout.padding(YogaPhysicalEdge.LEFT) +
                layout.padding(YogaPhysicalEdge.RIGHT) + layout.border(YogaPhysicalEdge.LEFT) +
                layout.border(YogaPhysicalEdge.RIGHT);
        final float paddingAndBorderAxisColumn = layout.padding(YogaPhysicalEdge.TOP) +
                layout.padding(YogaPhysicalEdge.BOTTOM) + layout.border(YogaPhysicalEdge.TOP) +
                layout.border(YogaPhysicalEdge.BOTTOM);

        // We want to make sure we don't call measure with negative size
        final float innerWidth = Comparison.isUndefined(availableWidth)
                ? availableWidth
                : Comparison.maxOrDefined(0.0f, availableWidth - paddingAndBorderAxisRow);
        final float innerHeight = Comparison.isUndefined(availableHeight)
                ? availableHeight
                : Comparison.maxOrDefined(0.0f, availableHeight - paddingAndBorderAxisColumn);

        if (widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT &&
                heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT) {
            // Don't bother sizing the text if both dimensions are already defined.
            node.setLayoutMeasuredDimension(
                    boundAxis(
                            node,
                            YogaFlexDirection.ROW,
                            direction,
                            availableWidth,
                            ownerWidth,
                            ownerWidth),
                    YogaDimension.WIDTH);
            node.setLayoutMeasuredDimension(
                    boundAxis(
                            node,
                            YogaFlexDirection.COLUMN,
                            direction,
                            availableHeight,
                            ownerHeight,
                            ownerWidth),
                    YogaDimension.HEIGHT);
        } else {
            YogaEvent.publish(YogaEventType.MEASURE_CALLBACK_START, node);

            // Measure the text under the current constraints.
            final YogaSize measuredSize = node.measure(
                    innerWidth,
                    measureMode(widthSizingMode),
                    innerHeight,
                    measureMode(heightSizingMode));

            layoutMarkerData.measureCallbacks += 1;
            layoutMarkerData.measureCallbackReasonsCount[reason.ordinal()] += 1;

            YogaEvent.publish(
                    node,
                    YogaEventType.MEASURE_CALLBACK_END,
                    new YogaEvent.MeasureCallbackEndData(
                            innerWidth,
                            measureMode(widthSizingMode),
                            innerHeight,
                            measureMode(heightSizingMode),
                            measuredSize.width(),
                            measuredSize.height(),
                            reason));

            node.setLayoutMeasuredDimension(
                    boundAxis(
                            node,
                            YogaFlexDirection.ROW,
                            direction,
                            (widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT ||
                                    widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT)
                                    ? measuredSize.width() + paddingAndBorderAxisRow
                                    : availableWidth,
                            ownerWidth,
                            ownerWidth),
                    YogaDimension.WIDTH);

            node.setLayoutMeasuredDimension(
                    boundAxis(
                            node,
                            YogaFlexDirection.COLUMN,
                            direction,
                            (heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT ||
                                    heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT)
                                    ? measuredSize.height() + paddingAndBorderAxisColumn
                                    : availableHeight,
                            ownerHeight,
                            ownerWidth),
                    YogaDimension.HEIGHT);
        }
    }

    private static void measureNodeWithoutChildren(
            YogaNode node,
            YogaDirection direction,
            float availableWidth,
            float availableHeight,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthSizingMode,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode heightSizingMode,
            float ownerWidth,
            float ownerHeight) {
        LayoutResults layout = node.getLayout();

        float width = availableWidth;
        if (widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT ||
                widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT) {
            width = layout.padding(YogaPhysicalEdge.LEFT) +
                    layout.padding(YogaPhysicalEdge.RIGHT) +
                    layout.border(YogaPhysicalEdge.LEFT) +
                    layout.border(YogaPhysicalEdge.RIGHT);
        }
        node.setLayoutMeasuredDimension(
                boundAxis(
                        node, YogaFlexDirection.ROW, direction, width, ownerWidth, ownerWidth),
                YogaDimension.WIDTH);

        float height = availableHeight;
        if (heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT ||
                heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT) {
            height = layout.padding(YogaPhysicalEdge.TOP) +
                    layout.padding(YogaPhysicalEdge.BOTTOM) +
                    layout.border(YogaPhysicalEdge.TOP) +
                    layout.border(YogaPhysicalEdge.BOTTOM);
        }
        node.setLayoutMeasuredDimension(
                boundAxis(
                        node,
                        YogaFlexDirection.COLUMN,
                        direction,
                        height,
                        ownerHeight,
                        ownerWidth),
                YogaDimension.HEIGHT);
    }

    private static boolean measureNodeWithFixedSize(
            YogaNode node,
            YogaDirection direction,
            float availableWidth,
            float availableHeight,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthSizingMode,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode heightSizingMode,
            float ownerWidth,
            float ownerHeight) {
        if ((Comparison.isDefined(availableWidth) &&
                widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT && availableWidth <= 0.0f) ||
                (Comparison.isDefined(availableHeight) &&
                        heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT && availableHeight <= 0.0f) ||
                (widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT &&
                        heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT)) {
            node.setLayoutMeasuredDimension(
                    boundAxis(
                            node,
                            YogaFlexDirection.ROW,
                            direction,
                            Comparison.isUndefined(availableWidth) ||
                                    (widthSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT &&
                                            availableWidth < 0.0f)
                                    ? 0.0f
                                    : availableWidth,
                            ownerWidth,
                            ownerWidth),
                    YogaDimension.WIDTH);

            node.setLayoutMeasuredDimension(
                    boundAxis(
                            node,
                            YogaFlexDirection.COLUMN,
                            direction,
                            Comparison.isUndefined(availableHeight) ||
                                    (heightSizingMode == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT &&
                                            availableHeight < 0.0f)
                                    ? 0.0f
                                    : availableHeight,
                            ownerHeight,
                            ownerWidth),
                    YogaDimension.HEIGHT);
            return true;
        }

        return false;
    }

    private static void zeroOutLayoutRecursively(YogaNode node) {
        node.getLayout().reset();
        node.setLayoutDimension(0, YogaDimension.WIDTH);
        node.setLayoutDimension(0, YogaDimension.HEIGHT);
        node.setHasNewLayout(true);

        node.cloneChildrenIfNeeded();
        for (YogaNode child : node.getChildren()) {
            zeroOutLayoutRecursively(child);
        }
    }

    private static void cleanupContentsNodesRecursively(YogaNode node) {
        for (YogaNode child : node.getChildren()) {
            if (child.getStyle().getDisplay() == YogaDisplay.CONTENTS) {
                child.getLayout().reset();
                child.setLayoutDimension(0, YogaDimension.WIDTH);
                child.setLayoutDimension(0, YogaDimension.HEIGHT);
                child.setHasNewLayout(true);
                child.setDirty(false);
                child.cloneChildrenIfNeeded();

                cleanupContentsNodesRecursively(child);
            }
        }
    }

    private static float calculateAvailableInnerDimension(
            YogaNode node,
            YogaDirection direction,
            YogaDimension dimension,
            float availableDim,
            float paddingAndBorder,
            float ownerDim,
            float ownerWidth) {
        float availableInnerDim = availableDim - paddingAndBorder;
        // Max dimension overrides predefined dimension value; Min dimension in turn
        // overrides both of the above
        if (Comparison.isDefined(availableInnerDim)) {
            // We want to make sure our available height does not violate min and max
            // constraints
            FloatOptional minDimensionOptional =
                    node.getStyle().resolveMinDimension(
                            direction, dimension, ownerDim, ownerWidth);
            float minInnerDim = minDimensionOptional.isUndefined()
                    ? 0.0f
                    : minDimensionOptional.unwrap() - paddingAndBorder;

            FloatOptional maxDimensionOptional =
                    node.getStyle().resolveMaxDimension(
                            direction, dimension, ownerDim, ownerWidth);

            float maxInnerDim = maxDimensionOptional.isUndefined()
                    ? Float.MAX_VALUE
                    : maxDimensionOptional.unwrap() - paddingAndBorder;
            availableInnerDim = Comparison.maxOrDefined(
                    Comparison.minOrDefined(availableInnerDim, maxInnerDim), minInnerDim);
        }

        return availableInnerDim;
    }

    private static float computeFlexBasisForChildren(
            YogaNode node,
            float availableInnerWidth,
            float availableInnerHeight,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode widthSizingMode,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode heightSizingMode,
            YogaDirection direction,
            YogaFlexDirection mainAxis,
            boolean performLayout,
            LayoutData layoutMarkerData,
            int depth,
            int generationCount) {
        float totalOuterFlexBasis = 0.0f;
        YogaNode singleFlexChild = null;
        LayoutableChildren children = node.getLayoutChildren();
        io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode sizingModeMainDim =
                FlexDirectionUtil.isRow(mainAxis) ? widthSizingMode : heightSizingMode;

        // If there is only one child with flexGrow + flexShrink it means we can set
        // the computedFlexBasis to 0 instead of measuring and shrinking / flexing the
        // child to exactly match the remaining space
        if (sizingModeMainDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT) {
            for (YogaNode child : children) {
                if (child.isNodeFlexible()) {
                    if (singleFlexChild != null ||
                            Comparison.inexactEquals(child.resolveFlexGrow(), 0.0f) ||
                            Comparison.inexactEquals(child.resolveFlexShrink(), 0.0f)) {
                        // There is already a flexible child, or this flexible child doesn't
                        // have flexGrow and flexShrink, abort
                        singleFlexChild = null;
                        break;
                    } else {
                        singleFlexChild = child;
                    }
                }
            }
        }

        for (YogaNode child : children) {
            child.processDimensions();
            if (child.getStyle().getDisplay() == YogaDisplay.NONE) {
                zeroOutLayoutRecursively(child);
                child.setHasNewLayout(true);
                child.setDirty(false);
                continue;
            }
            if (performLayout) {
                // Set the initial position (relative to the owner).
                final YogaDirection childDirection = child.resolveDirection(direction);
                child.setPosition(
                        childDirection, availableInnerWidth, availableInnerHeight);
            }

            if (child.getStyle().getPositionType() == YogaPositionType.ABSOLUTE) {
                continue;
            }
            if (child == singleFlexChild) {
                child.setLayoutComputedFlexBasisGeneration(generationCount);
                child.setLayoutComputedFlexBasis(FloatOptional.of(0));
            } else {
                computeFlexBasisForChild(
                        node,
                        child,
                        availableInnerWidth,
                        widthSizingMode,
                        availableInnerHeight,
                        availableInnerWidth,
                        availableInnerHeight,
                        heightSizingMode,
                        direction,
                        layoutMarkerData,
                        depth,
                        generationCount);
            }

            totalOuterFlexBasis +=
                    (child.getLayout().computedFlexBasis.unwrap() +
                            child.getStyle().computeMarginForAxis(mainAxis, availableInnerWidth));
        }

        return totalOuterFlexBasis;
    }

    /**
     * Distributes the free space to the flexible items and ensures that the size
     * of the flex items abide the min and max constraints. At the end of this
     * function the child nodes would have proper size. Prior using this function
     * please ensure that distributeFreeSpaceFirstPass is called.
     */
    private static float distributeFreeSpaceSecondPass(
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexLine flexLine,
            YogaNode node,
            YogaFlexDirection mainAxis,
            YogaFlexDirection crossAxis,
            YogaDirection direction,
            float ownerWidth,
            float mainAxisOwnerSize,
            float availableInnerMainDim,
            float availableInnerCrossDim,
            float availableInnerWidth,
            float availableInnerHeight,
            boolean mainAxisOverflows,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode sizingModeCrossDim,
            boolean performLayout,
            LayoutData layoutMarkerData,
            int depth,
            int generationCount) {

        float childFlexBasis = 0;
        float flexShrinkScaledFactor = 0;
        float flexGrowFactor = 0;
        float deltaFreeSpace = 0;
        final boolean isMainAxisRow = FlexDirectionUtil.isRow(mainAxis);
        final boolean isNodeFlexWrap = node.getStyle().getFlexWrap() != YogaWrap.NO_WRAP;

        for (YogaNode currentLineChild : flexLine.getItemsInFlow()) {
            childFlexBasis = boundAxisWithinMinAndMax(
                    currentLineChild,
                    direction,
                    mainAxis,
                    currentLineChild.getLayout().computedFlexBasis,
                    mainAxisOwnerSize,
                    ownerWidth)
                    .unwrap();
            float updatedMainSize = childFlexBasis;

            if (Comparison.isDefined(flexLine.getLayout().getRemainingFreeSpace()) &&
                    flexLine.getLayout().getRemainingFreeSpace() < 0) {
                flexShrinkScaledFactor =
                        -currentLineChild.resolveFlexShrink() * childFlexBasis;
                // Is this child able to shrink?
                if (flexShrinkScaledFactor != 0) {
                    float childSize = Float.NaN;

                    if (Comparison.isDefined(flexLine.getLayout().getTotalFlexShrinkScaledFactors()) &&
                            flexLine.getLayout().getTotalFlexShrinkScaledFactors() == 0) {
                        childSize = childFlexBasis + flexShrinkScaledFactor;
                    } else {
                        childSize = childFlexBasis +
                                (flexLine.getLayout().getRemainingFreeSpace() /
                                        flexLine.getLayout().getTotalFlexShrinkScaledFactors()) *
                                        flexShrinkScaledFactor;
                    }

                    updatedMainSize = boundAxis(
                            currentLineChild,
                            mainAxis,
                            direction,
                            childSize,
                            availableInnerMainDim,
                            availableInnerWidth);
                }
            } else if (
                    Comparison.isDefined(flexLine.getLayout().getRemainingFreeSpace()) &&
                            flexLine.getLayout().getRemainingFreeSpace() > 0) {
                flexGrowFactor = currentLineChild.resolveFlexGrow();

                // Is this child able to grow?
                if (!Float.isNaN(flexGrowFactor) && flexGrowFactor != 0) {
                    updatedMainSize = boundAxis(
                            currentLineChild,
                            mainAxis,
                            direction,
                            childFlexBasis +
                                    flexLine.getLayout().getRemainingFreeSpace() /
                                            flexLine.getLayout().getTotalFlexGrowFactors() * flexGrowFactor,
                            availableInnerMainDim,
                            availableInnerWidth);
                }
            }

            deltaFreeSpace += updatedMainSize - childFlexBasis;

            final float marginMain = currentLineChild.getStyle().computeMarginForAxis(
                    mainAxis, availableInnerWidth);
            final float marginCross = currentLineChild.getStyle().computeMarginForAxis(
                    crossAxis, availableInnerWidth);

            float childCrossSize = Float.NaN;
            float childMainSize = updatedMainSize + marginMain;
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childCrossSizingMode;
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childMainSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;

            final var childStyle = currentLineChild.getStyle();
            if (childStyle.getAspectRatio().isDefined()) {
                childCrossSize = isMainAxisRow
                        ? (childMainSize - marginMain) / childStyle.getAspectRatio().unwrap()
                        : (childMainSize - marginMain) * childStyle.getAspectRatio().unwrap();
                childCrossSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;

                childCrossSize += marginCross;
            } else if (
                    !Float.isNaN(availableInnerCrossDim) &&
                            !currentLineChild.hasDefiniteLength(
                                    FlexDirectionUtil.dimension(crossAxis), availableInnerCrossDim) &&
                            sizingModeCrossDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT &&
                            !(isNodeFlexWrap && mainAxisOverflows) &&
                            resolveChildAlignment(node, currentLineChild) == YogaAlign.STRETCH &&
                            !currentLineChild.getStyle().flexStartMarginIsAuto(
                                    crossAxis, direction) &&
                            !currentLineChild.getStyle().flexEndMarginIsAuto(crossAxis, direction)) {
                childCrossSize = availableInnerCrossDim;
                childCrossSizingMode = io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            } else if (!currentLineChild.hasDefiniteLength(
                    FlexDirectionUtil.dimension(crossAxis), availableInnerCrossDim)) {
                childCrossSize = availableInnerCrossDim;
                childCrossSizingMode = Comparison.isUndefined(childCrossSize)
                        ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT
                        : io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT;
            } else {
                childCrossSize = currentLineChild
                        .getResolvedDimension(
                                direction,
                                FlexDirectionUtil.dimension(crossAxis),
                                availableInnerCrossDim,
                                availableInnerWidth)
                        .unwrap() +
                        marginCross;
                final boolean isLoosePercentageMeasurement =
                        currentLineChild.getProcessedDimension(FlexDirectionUtil.dimension(crossAxis))
                                .isPercent() &&
                                sizingModeCrossDim != io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
                childCrossSizingMode =
                        Comparison.isUndefined(childCrossSize) || isLoosePercentageMeasurement
                                ? io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.MAX_CONTENT
                                : io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.STRETCH_FIT;
            }

            var maxSizeConstraint = new MaxSizeConstraint();
            maxSizeConstraint.constrainForMode(
                    currentLineChild,
                    direction,
                    mainAxis,
                    availableInnerMainDim,
                    availableInnerWidth,
                    childMainSizingMode,
                    childMainSize);
            childMainSizingMode = maxSizeConstraint.mode;
            childMainSize = maxSizeConstraint.size;
            maxSizeConstraint.constrainForMode(
                    currentLineChild,
                    direction,
                    crossAxis,
                    availableInnerCrossDim,
                    availableInnerWidth,
                    childCrossSizingMode,
                    childCrossSize);
            childCrossSizingMode = maxSizeConstraint.mode;
            childCrossSize = maxSizeConstraint.size;

            final boolean requiresStretchLayout =
                    !currentLineChild.hasDefiniteLength(
                            FlexDirectionUtil.dimension(crossAxis), availableInnerCrossDim) &&
                            resolveChildAlignment(node, currentLineChild) == YogaAlign.STRETCH &&
                            !currentLineChild.getStyle().flexStartMarginIsAuto(
                                    crossAxis, direction) &&
                            !currentLineChild.getStyle().flexEndMarginIsAuto(crossAxis, direction);

            final float childWidth = isMainAxisRow ? childMainSize : childCrossSize;
            final float childHeight = !isMainAxisRow ? childMainSize : childCrossSize;

            final io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childWidthSizingMode =
                    isMainAxisRow ? childMainSizingMode : childCrossSizingMode;
            final io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode childHeightSizingMode =
                    !isMainAxisRow ? childMainSizingMode : childCrossSizingMode;

            final boolean isLayoutPass = performLayout && !requiresStretchLayout;
            // Recursively call the layout algorithm for this child with the updated
            // main size.
            calculateLayoutInternal(
                    currentLineChild,
                    childWidth,
                    childHeight,
                    node.getLayout().direction(),
                    childWidthSizingMode,
                    childHeightSizingMode,
                    availableInnerWidth,
                    availableInnerHeight,
                    isLayoutPass,
                    isLayoutPass ? LayoutPassReason.FLEX_LAYOUT
                            : LayoutPassReason.FLEX_MEASURE,
                    layoutMarkerData,
                    depth,
                    generationCount);
            node.setLayoutHadOverflow(
                    node.getLayout().hadOverflow() ||
                            currentLineChild.getLayout().hadOverflow());
        }
        return deltaFreeSpace;
    }

    /**
     * Distributes the free space to the flexible items. For those flexible items
     * whose min and max constraints are triggered, those flex item's clamped size
     * is removed from the remaining free space.
     */
    private static void distributeFreeSpaceFirstPass(
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexLine flexLine,
            YogaDirection direction,
            YogaFlexDirection mainAxis,
            float ownerWidth,
            float mainAxisOwnerSize,
            float availableInnerMainDim,
            float availableInnerWidth) {

        float flexShrinkScaledFactor = 0;
        float flexGrowFactor = 0;
        float baseMainSize = 0;
        float boundMainSize = 0;
        float deltaFreeSpace = 0;

        for (YogaNode currentLineChild : flexLine.getItemsInFlow()) {
            float childFlexBasis = boundAxisWithinMinAndMax(
                    currentLineChild,
                    direction,
                    mainAxis,
                    currentLineChild.getLayout().computedFlexBasis,
                    mainAxisOwnerSize,
                    ownerWidth)
                    .unwrap();

            if (flexLine.getLayout().getRemainingFreeSpace() < 0) {
                flexShrinkScaledFactor =
                        -currentLineChild.resolveFlexShrink() * childFlexBasis;

                // Is this child able to shrink?
                if (Comparison.isDefined(flexShrinkScaledFactor) &&
                        flexShrinkScaledFactor != 0) {
                    baseMainSize = childFlexBasis +
                            flexLine.getLayout().getRemainingFreeSpace() /
                                    flexLine.getLayout().getTotalFlexShrinkScaledFactors() *
                                    flexShrinkScaledFactor;
                    boundMainSize = boundAxis(
                            currentLineChild,
                            mainAxis,
                            direction,
                            baseMainSize,
                            availableInnerMainDim,
                            availableInnerWidth);
                    if (Comparison.isDefined(baseMainSize) && Comparison.isDefined(boundMainSize) &&
                            baseMainSize != boundMainSize) {
                        // By excluding this item's size and flex factor from remaining, this
                        // item's min/max constraints should also trigger in the second pass
                        // resulting in the item's size calculation being identical in the
                        // first and second passes.
                        deltaFreeSpace += boundMainSize - childFlexBasis;
                        flexLine.getLayout().setTotalFlexShrinkScaledFactors(
                                flexLine.getLayout().getTotalFlexShrinkScaledFactors() -
                                        (-currentLineChild.resolveFlexShrink() *
                                                currentLineChild.getLayout().computedFlexBasis.unwrap()));
                    }
                }
            } else if (
                    Comparison.isDefined(flexLine.getLayout().getRemainingFreeSpace()) &&
                            flexLine.getLayout().getRemainingFreeSpace() > 0) {
                flexGrowFactor = currentLineChild.resolveFlexGrow();

                // Is this child able to grow?
                if (Comparison.isDefined(flexGrowFactor) && flexGrowFactor != 0) {
                    baseMainSize = childFlexBasis +
                            flexLine.getLayout().getRemainingFreeSpace() /
                                    flexLine.getLayout().getTotalFlexGrowFactors() * flexGrowFactor;
                    boundMainSize = boundAxis(
                            currentLineChild,
                            mainAxis,
                            direction,
                            baseMainSize,
                            availableInnerMainDim,
                            availableInnerWidth);

                    if (Comparison.isDefined(baseMainSize) && Comparison.isDefined(boundMainSize) &&
                            baseMainSize != boundMainSize) {
                        // By excluding this item's size and flex factor from remaining, this
                        // item's min/max constraints should also trigger in the second pass
                        // resulting in the item's size calculation being identical in the
                        // first and second passes.
                        deltaFreeSpace += boundMainSize - childFlexBasis;
                        flexLine.getLayout().setTotalFlexGrowFactors(
                                flexLine.getLayout().getTotalFlexGrowFactors() - flexGrowFactor);
                    }
                }
            }
        }
        flexLine.getLayout().setRemainingFreeSpace(
                flexLine.getLayout().getRemainingFreeSpace() - deltaFreeSpace);
    }

    /**
     * Do two passes over the flex items to figure out how to distribute the
     * remaining space.
     * <p>
     * The first pass finds the items whose min/max constraints trigger, freezes
     * them at those sizes, and excludes those sizes from the remaining space.
     * <p>
     * The second pass sets the size of each flexible item. It distributes the
     * remaining space amongst the items whose min/max constraints didn't trigger in
     * the first pass. For the other items, it sets their sizes by forcing their
     * min/max constraints to trigger again.
     * <p>
     * This two pass approach for resolving min/max constraints deviates from the
     * spec. The spec
     * (https://www.w3.org/TR/CSS-flexbox-1/#resolve-flexible-lengths) describes a
     * process that needs to be repeated a variable number of times. The algorithm
     * implemented here won't handle all cases but it was simpler to implement and
     * it mitigates performance concerns because we know exactly how many passes
     * it'll do.
     * <p>
     * At the end of this function the child nodes would have the proper size
     * assigned to them.
     */
    private static void resolveFlexibleLength(
            YogaNode node,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexLine flexLine,
            YogaFlexDirection mainAxis,
            YogaFlexDirection crossAxis,
            YogaDirection direction,
            float ownerWidth,
            float mainAxisOwnerSize,
            float availableInnerMainDim,
            float availableInnerCrossDim,
            float availableInnerWidth,
            float availableInnerHeight,
            boolean mainAxisOverflows,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode sizingModeCrossDim,
            boolean performLayout,
            LayoutData layoutMarkerData,
            int depth,
            int generationCount) {
        float originalFreeSpace = flexLine.getLayout().getRemainingFreeSpace();
        // First pass: detect the flex items whose min/max constraints trigger
        distributeFreeSpaceFirstPass(
                flexLine,
                direction,
                mainAxis,
                ownerWidth,
                mainAxisOwnerSize,
                availableInnerMainDim,
                availableInnerWidth);

        // Second pass: resolve the sizes of the flexible items
        float distributedFreeSpace = distributeFreeSpaceSecondPass(
                flexLine,
                node,
                mainAxis,
                crossAxis,
                direction,
                ownerWidth,
                mainAxisOwnerSize,
                availableInnerMainDim,
                availableInnerCrossDim,
                availableInnerWidth,
                availableInnerHeight,
                mainAxisOverflows,
                sizingModeCrossDim,
                performLayout,
                layoutMarkerData,
                depth,
                generationCount);

        flexLine.getLayout().setRemainingFreeSpace(originalFreeSpace - distributedFreeSpace);
    }

    private static void justifyMainAxis(
            YogaNode node,
            FlexLine flexLine,
            YogaFlexDirection mainAxis,
            YogaFlexDirection crossAxis,
            YogaDirection direction,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode sizingModeMainDim,
            io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode sizingModeCrossDim,
            float mainAxisOwnerSize,
            float ownerWidth,
            float availableInnerMainDim,
            float availableInnerCrossDim,
            float availableInnerWidth,
            boolean performLayout) {
        var style = node.getStyle();

        float leadingPaddingAndBorderMain =
                node.getStyle().computeFlexStartPaddingAndBorder(
                        mainAxis, direction, ownerWidth);
        float trailingPaddingAndBorderMain =
                node.getStyle().computeFlexEndPaddingAndBorder(
                        mainAxis, direction, ownerWidth);

        float gap = node.getStyle().computeGapForAxis(mainAxis, availableInnerMainDim);

        // If we are using "at most" rules in the main axis, make sure that
        // remainingFreeSpace is 0 when min main dimension is not given
        if (sizingModeMainDim == io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.SizingMode.FIT_CONTENT &&
                flexLine.getLayout().getRemainingFreeSpace() > 0) {
            if (style.getMinDimension(dimension(mainAxis)).isDefined() &&
                    style.resolveMinDimension(
                                    direction, dimension(mainAxis), mainAxisOwnerSize, ownerWidth)
                            .isDefined()) {
                // This condition makes sure that if the size of main dimension(after
                // considering child nodes main dim, leading and trailing padding etc)
                // falls below min dimension, then the remainingFreeSpace is reassigned
                // considering the min dimension

                // `minAvailableMainDim` denotes minimum available space in which child
                // can be laid out, it will exclude space consumed by padding and border.
                float minAvailableMainDim =
                        style.resolveMinDimension(
                                        direction, dimension(mainAxis), mainAxisOwnerSize, ownerWidth)
                                .unwrap() -
                                leadingPaddingAndBorderMain - trailingPaddingAndBorderMain;
                float occupiedSpaceByChildNodes =
                        availableInnerMainDim - flexLine.getLayout().getRemainingFreeSpace();
                flexLine.getLayout().setRemainingFreeSpace(Comparison.maxOrDefined(
                        0.0f, minAvailableMainDim - occupiedSpaceByChildNodes));
            } else {
                flexLine.getLayout().setRemainingFreeSpace(0);
            }
        }

        // In order to position the elements in the main axis, we have two controls.
        // The space between the beginning and the first element and the space between
        // each two elements.
        float leadingMainDim = 0;
        float betweenMainDim = gap;
        YogaJustify justifyContent = flexLine.getLayout().getRemainingFreeSpace() >= 0
                ? node.getStyle().getJustifyContent()
                : fallbackAlignment(node.getStyle().getJustifyContent());

        if (flexLine.getNumberOfAutoMargins() == 0) {
            switch (justifyContent) {
                case CENTER:
                    leadingMainDim = flexLine.getLayout().getRemainingFreeSpace() / 2;
                    break;
                case FLEX_END:
                    leadingMainDim = flexLine.getLayout().getRemainingFreeSpace();
                    break;
                case SPACE_BETWEEN:
                    if (flexLine.getItemsInFlow().size() > 1) {
                        betweenMainDim += flexLine.getLayout().getRemainingFreeSpace() /
                                (float) (flexLine.getItemsInFlow().size() - 1);
                    }
                    break;
                case SPACE_EVENLY:
                    // Space is distributed evenly across all elements
                    leadingMainDim = flexLine.getLayout().getRemainingFreeSpace() /
                            (float) (flexLine.getItemsInFlow().size() + 1);
                    betweenMainDim += leadingMainDim;
                    break;
                case SPACE_AROUND:
                    // Space on the edges is half of the space between elements
                    leadingMainDim = 0.5f * flexLine.getLayout().getRemainingFreeSpace() /
                            (float) (flexLine.getItemsInFlow().size());
                    betweenMainDim += leadingMainDim * 2;
                    break;
                case FLEX_START:
                    break;
            }
        }

        flexLine.getLayout().setMainDim(leadingPaddingAndBorderMain + leadingMainDim);
        flexLine.getLayout().setCrossDim(0);

        float maxAscentForCurrentLine = 0;
        float maxDescentForCurrentLine = 0;
        boolean isNodeBaselineLayout = isBaselineLayout(node);

        for (Iterator<YogaNode> it = flexLine.getItemsInFlow().iterator(); it.hasNext(); ) {
            YogaNode child = it.next();
            LayoutResults childLayout = child.getLayout();

            if (child.getStyle().flexStartMarginIsAuto(mainAxis, direction) &&
                    flexLine.getLayout().getRemainingFreeSpace() > 0.0f) {
                flexLine.getLayout().setMainDim(flexLine.getLayout().getMainDim() + flexLine.getLayout().getRemainingFreeSpace() /
                        (float) (flexLine.getNumberOfAutoMargins()));
            }

            if (performLayout) {
                child.setLayoutPosition(
                        childLayout.position(flexStartEdge(mainAxis)) +
                                flexLine.getLayout().getMainDim(),
                        flexStartEdge(mainAxis));
            }

            if (it.hasNext()) {
                flexLine.getLayout().setMainDim(flexLine.getLayout().getMainDim() + betweenMainDim);
            }

            if (child.getStyle().flexEndMarginIsAuto(mainAxis, direction) &&
                    flexLine.getLayout().getRemainingFreeSpace() > 0.0f) {
                flexLine.getLayout().setMainDim(flexLine.getLayout().getMainDim() + flexLine.getLayout().getRemainingFreeSpace() /
                        (float) (flexLine.getNumberOfAutoMargins()));
            }

            boolean canSkipFlex =
                    !performLayout && sizingModeCrossDim == SizingMode.STRETCH_FIT;

            if (canSkipFlex) {
                // If we skipped the flex step, then we can't rely on the measuredDims
                // because they weren't computed. This means we can't call
                // dimensionWithMargin.
                flexLine.getLayout().setMainDim(flexLine.getLayout().getMainDim() +
                        child.getStyle().computeMarginForAxis(mainAxis, availableInnerWidth) +
                        childLayout.computedFlexBasis.unwrap());
                flexLine.getLayout().setCrossDim(availableInnerCrossDim);
            } else {
                // The main dimension is the sum of all the elements dimension plus
                // the spacing.
                flexLine.getLayout().setMainDim(flexLine.getLayout().getMainDim() +
                        child.dimensionWithMargin(mainAxis, availableInnerWidth));

                if (isNodeBaselineLayout) {
                    // If the child is baseline aligned then the cross dimension is
                    // calculated by adding maxAscent and maxDescent from the baseline.
                    float ascent = calculateBaseline(child) +
                            child.getStyle().computeFlexStartMargin(
                                    YogaFlexDirection.COLUMN, direction, availableInnerWidth);
                    float descent =
                            child.getLayout().measuredDimension(YogaDimension.HEIGHT) +
                                    child.getStyle().computeMarginForAxis(
                                            YogaFlexDirection.COLUMN, availableInnerWidth) -
                                    ascent;

                    maxAscentForCurrentLine =
                            Comparison.maxOrDefined(maxAscentForCurrentLine, ascent);
                    maxDescentForCurrentLine =
                            Comparison.maxOrDefined(maxDescentForCurrentLine, descent);
                } else {
                    // The cross dimension is the max of the elements dimension since
                    // there can only be one element in that cross dimension in the case
                    // when the items are not baseline aligned
                    flexLine.getLayout().setCrossDim(Comparison.maxOrDefined(
                            flexLine.getLayout().getCrossDim(),
                            child.dimensionWithMargin(crossAxis, availableInnerWidth)));
                }
            }
        }

        flexLine.getLayout().setMainDim(flexLine.getLayout().getMainDim() + trailingPaddingAndBorderMain);

        if (isNodeBaselineLayout) {
            flexLine.getLayout().setCrossDim(
                    maxAscentForCurrentLine + maxDescentForCurrentLine);
        }
    }
}
