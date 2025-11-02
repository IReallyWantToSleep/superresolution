/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaBoxSizing;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDimension;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDisplay;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaJustify;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaOverflow;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPhysicalEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPositionType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaWrap;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.FloatOptional;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleValueHandle;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleValuePool;

/**
 * The Style class for Yoga nodes that contains layout properties.
 */
public class YogaStyle {

    public static final float DEFAULT_FLEX_GROW = 0.0f;
    public static final float DEFAULT_FLEX_SHRINK = 0.0f;
    public static final float WEB_DEFAULT_FLEX_SHRINK = 1.0f;

    private YogaDirection direction = YogaDirection.INHERIT;
    private YogaFlexDirection flexDirection = YogaFlexDirection.COLUMN;
    private YogaJustify justifyContent = YogaJustify.FLEX_START;
    private YogaAlign alignContent = YogaAlign.FLEX_START;
    private YogaAlign alignItems = YogaAlign.STRETCH;
    private YogaAlign alignSelf = YogaAlign.AUTO;
    private YogaPositionType positionType = YogaPositionType.RELATIVE;
    private YogaWrap flexWrap = YogaWrap.NO_WRAP;
    private YogaOverflow overflow = YogaOverflow.VISIBLE;
    private YogaDisplay display = YogaDisplay.FLEX;
    private YogaBoxSizing boxSizing = YogaBoxSizing.BORDER_BOX;

    private StyleValueHandle flex = new StyleValueHandle();
    private StyleValueHandle flexGrow = new StyleValueHandle();
    private StyleValueHandle flexShrink = new StyleValueHandle();
    private StyleValueHandle flexBasis = StyleValueHandle.ofAuto();
    private StyleValueHandle[] margin = new StyleValueHandle[YogaEdge.values().length];
    private StyleValueHandle[] position = new StyleValueHandle[YogaEdge.values().length];
    private StyleValueHandle[] padding = new StyleValueHandle[YogaEdge.values().length];
    private StyleValueHandle[] border = new StyleValueHandle[YogaEdge.values().length];
    private StyleValueHandle[] gap = new StyleValueHandle[YogaGutter.values().length];
    private StyleValueHandle[] dimensions = new StyleValueHandle[]{
            StyleValueHandle.ofAuto(),
            StyleValueHandle.ofAuto()
    };
    private StyleValueHandle[] minDimensions = new StyleValueHandle[]{
            new StyleValueHandle(),
            new StyleValueHandle()
    };
    private StyleValueHandle[] maxDimensions = new StyleValueHandle[]{
            new StyleValueHandle(),
            new StyleValueHandle()
    };
    private StyleValueHandle aspectRatio = new StyleValueHandle();

    private StyleValuePool pool = new StyleValuePool();

    public YogaStyle() {
        // Initialize arrays
        for (int i = 0; i < YogaEdge.values().length; i++) {
            margin[i] = new StyleValueHandle();
            position[i] = new StyleValueHandle();
            padding[i] = new StyleValueHandle();
            border[i] = new StyleValueHandle();
        }

        for (int i = 0; i < YogaGutter.values().length; i++) {
            gap[i] = new StyleValueHandle();
        }
    }

    /**
     * Copy constructor that creates a deep copy of the provided YogaStyle object.
     *
     * @param other The YogaStyle object to copy from
     */
    public YogaStyle(YogaStyle other) {
        // Copy enum values
        this.direction = other.direction;
        this.flexDirection = other.flexDirection;
        this.justifyContent = other.justifyContent;
        this.alignContent = other.alignContent;
        this.alignItems = other.alignItems;
        this.alignSelf = other.alignSelf;
        this.positionType = other.positionType;
        this.flexWrap = other.flexWrap;
        this.overflow = other.overflow;
        this.display = other.display;
        this.boxSizing = other.boxSizing;

        // Copy StyleValueHandle objects
        this.flex = new StyleValueHandle();
        this.flexGrow = new StyleValueHandle();
        this.flexShrink = new StyleValueHandle();
        this.flexBasis = new StyleValueHandle();
        this.aspectRatio = new StyleValueHandle();

        // Initialize arrays
        this.margin = new StyleValueHandle[YogaEdge.values().length];
        this.position = new StyleValueHandle[YogaEdge.values().length];
        this.padding = new StyleValueHandle[YogaEdge.values().length];
        this.border = new StyleValueHandle[YogaEdge.values().length];
        this.gap = new StyleValueHandle[YogaGutter.values().length];
        this.dimensions = new StyleValueHandle[2];
        this.minDimensions = new StyleValueHandle[YogaDimension.values().length];
        this.maxDimensions = new StyleValueHandle[YogaDimension.values().length];

        // Initialize pool
        this.pool = new StyleValuePool();

        // Copy values from the other object's pool to this pool
        setFlex(other.getFlex());
        setFlexGrow(other.getFlexGrow());
        setFlexShrink(other.getFlexShrink());
        setFlexBasis(other.getFlexBasis());
        setAspectRatio(other.getAspectRatio());

        // Copy array values
        for (int i = 0; i < YogaEdge.values().length; i++) {
            YogaEdge edge = YogaEdge.values()[i];
            margin[i] = new StyleValueHandle();
            position[i] = new StyleValueHandle();
            padding[i] = new StyleValueHandle();
            border[i] = new StyleValueHandle();

            setMargin(edge, other.getMargin(edge));
            setPosition(edge, other.getPosition(edge));
            setPadding(edge, other.getPadding(edge));
            setBorder(edge, other.getBorder(edge));
        }

        for (int i = 0; i < YogaGutter.values().length; i++) {
            YogaGutter gutter = YogaGutter.values()[i];
            gap[i] = new StyleValueHandle();
            setGap(gutter, other.getGap(gutter));
        }

        for (int i = 0; i < YogaDimension.values().length; i++) {
            YogaDimension dimension = YogaDimension.values()[i];

            if (i < dimensions.length) {
                dimensions[i] = new StyleValueHandle();
                setDimension(dimension, other.getDimension(dimension));
            }

            minDimensions[i] = new StyleValueHandle();
            maxDimensions[i] = new StyleValueHandle();

            setMinDimension(dimension, other.getMinDimension(dimension));
            setMaxDimension(dimension, other.getMaxDimension(dimension));
        }
    }

    public YogaDirection getDirection() {
        return direction;
    }

    public void setDirection(YogaDirection value) {
        direction = value;
    }

    public YogaFlexDirection getFlexDirection() {
        return flexDirection;
    }

    public void setFlexDirection(YogaFlexDirection value) {
        flexDirection = value;
    }

    public YogaJustify getJustifyContent() {
        return justifyContent;
    }

    public void setJustifyContent(YogaJustify value) {
        justifyContent = value;
    }

    public YogaAlign getAlignContent() {
        return alignContent;
    }

    public void setAlignContent(YogaAlign value) {
        alignContent = value;
    }

    public YogaAlign getAlignItems() {
        return alignItems;
    }

    public void setAlignItems(YogaAlign value) {
        alignItems = value;
    }

    public YogaAlign getAlignSelf() {
        return alignSelf;
    }

    public void setAlignSelf(YogaAlign value) {
        alignSelf = value;
    }

    public YogaPositionType getPositionType() {
        return positionType;
    }

    public void setPositionType(YogaPositionType value) {
        positionType = value;
    }

    public YogaWrap getFlexWrap() {
        return flexWrap;
    }

    public void setFlexWrap(YogaWrap value) {
        flexWrap = value;
    }

    public YogaOverflow getOverflow() {
        return overflow;
    }

    public void setOverflow(YogaOverflow value) {
        overflow = value;
    }

    public YogaDisplay getDisplay() {
        return display;
    }

    public void setDisplay(YogaDisplay value) {
        display = value;
    }

    public FloatOptional getFlex() {
        return pool.getNumber(flex);
    }

    public void setFlex(FloatOptional value) {
        pool.store(flex, value);
    }

    public FloatOptional getFlexGrow() {
        return pool.getNumber(flexGrow);
    }

    public void setFlexGrow(FloatOptional value) {
        pool.store(flexGrow, value);
    }

    public FloatOptional getFlexShrink() {
        return pool.getNumber(flexShrink);
    }

    public void setFlexShrink(FloatOptional value) {
        pool.store(flexShrink, value);
    }

    public StyleSizeLength getFlexBasis() {
        return pool.getSize(flexBasis);
    }

    public void setFlexBasis(StyleSizeLength value) {
        pool.store(flexBasis, value);
    }

    public StyleLength getMargin(YogaEdge edge) {
        return pool.getLength(margin[edge.ordinal()]);
    }

    public void setMargin(YogaEdge edge, StyleLength value) {
        pool.store(margin[edge.ordinal()], value);
    }

    public StyleLength getPosition(YogaEdge edge) {
        return pool.getLength(position[edge.ordinal()]);
    }

    public void setPosition(YogaEdge edge, StyleLength value) {
        pool.store(position[edge.ordinal()], value);
    }

    public StyleLength getPadding(YogaEdge edge) {
        return pool.getLength(padding[edge.ordinal()]);
    }

    public void setPadding(YogaEdge edge, StyleLength value) {
        pool.store(padding[edge.ordinal()], value);
    }

    public StyleLength getBorder(YogaEdge edge) {
        return pool.getLength(border[edge.ordinal()]);
    }

    public void setBorder(YogaEdge edge, StyleLength value) {
        pool.store(border[edge.ordinal()], value);
    }

    public StyleLength getGap(YogaGutter gutter) {
        return pool.getLength(gap[gutter.ordinal()]);
    }

    public void setGap(YogaGutter gutter, StyleLength value) {
        pool.store(gap[gutter.ordinal()], value);
    }

    public StyleSizeLength getDimension(YogaDimension axis) {
        return pool.getSize(dimensions[axis.ordinal()]);
    }

    public void setDimension(YogaDimension axis, StyleSizeLength value) {
        pool.store(dimensions[axis.ordinal()], value);
    }

    public StyleSizeLength getMinDimension(YogaDimension axis) {
        return pool.getSize(minDimensions[axis.ordinal()]);
    }

    public void setMinDimension(YogaDimension axis, StyleSizeLength value) {
        pool.store(minDimensions[axis.ordinal()], value);
    }

    public FloatOptional resolveMinDimension(
            YogaDirection direction,
            YogaDimension axis,
            float referenceLength,
            float ownerWidth) {
        FloatOptional value = getMinDimension(axis).resolve(referenceLength);
        if (boxSizing == YogaBoxSizing.BORDER_BOX) {
            return value;
        }

        FloatOptional dimensionPaddingAndBorder = FloatOptional.of(
                computePaddingAndBorderForDimension(direction, axis, ownerWidth));

        return dimensionPaddingAndBorder.isDefined()
                ? value.add(dimensionPaddingAndBorder)
                : value.add(FloatOptional.of(0.0f));
    }

    public StyleSizeLength getMaxDimension(YogaDimension axis) {
        return pool.getSize(maxDimensions[axis.ordinal()]);
    }

    public void setMaxDimension(YogaDimension axis, StyleSizeLength value) {
        pool.store(maxDimensions[axis.ordinal()], value);
    }

    public FloatOptional resolveMaxDimension(
            YogaDirection direction,
            YogaDimension axis,
            float referenceLength,
            float ownerWidth) {
        FloatOptional value = getMaxDimension(axis).resolve(referenceLength);
        if (boxSizing == YogaBoxSizing.BORDER_BOX) {
            return value;
        }

        FloatOptional dimensionPaddingAndBorder = FloatOptional.of(
                computePaddingAndBorderForDimension(direction, axis, ownerWidth));

        return dimensionPaddingAndBorder.isDefined()
                ? value.add(dimensionPaddingAndBorder)
                : value.add(FloatOptional.of(0.0f));
    }

    public FloatOptional getAspectRatio() {
        return pool.getNumber(aspectRatio);
    }

    public void setAspectRatio(FloatOptional value) {
        // degenerate aspect ratios act as auto.
        // see https://drafts.csswg.org/css-sizing-4/#valdef-aspect-ratio-ratio
        if (value.isDefined() &&
                (value.unwrap() == 0.0f || Float.isInfinite(value.unwrap()))) {
            pool.store(aspectRatio, FloatOptional.of());
        } else {
            pool.store(aspectRatio, value);
        }
    }

    public YogaBoxSizing getBoxSizing() {
        return boxSizing;
    }

    public void setBoxSizing(YogaBoxSizing value) {
        boxSizing = value;
    }

    public boolean horizontalInsetsDefined() {
        return position[YogaEdge.LEFT.ordinal()].isDefined() ||
                position[YogaEdge.RIGHT.ordinal()].isDefined() ||
                position[YogaEdge.ALL.ordinal()].isDefined() ||
                position[YogaEdge.HORIZONTAL.ordinal()].isDefined() ||
                position[YogaEdge.START.ordinal()].isDefined() ||
                position[YogaEdge.END.ordinal()].isDefined();
    }

    public boolean verticalInsetsDefined() {
        return position[YogaEdge.TOP.ordinal()].isDefined() ||
                position[YogaEdge.BOTTOM.ordinal()].isDefined() ||
                position[YogaEdge.ALL.ordinal()].isDefined() ||
                position[YogaEdge.VERTICAL.ordinal()].isDefined();
    }

    public boolean isFlexStartPositionDefined(YogaFlexDirection axis, YogaDirection direction) {
        return computePosition(FlexDirectionUtil.flexStartEdge(axis), direction).isDefined();
    }

    public boolean isFlexStartPositionAuto(YogaFlexDirection axis, YogaDirection direction) {
        return computePosition(FlexDirectionUtil.flexStartEdge(axis), direction).isAuto();
    }

    public boolean isInlineStartPositionDefined(YogaFlexDirection axis, YogaDirection direction) {
        return computePosition(FlexDirectionUtil.inlineStartEdge(axis, direction), direction)
                .isDefined();
    }

    public boolean isInlineStartPositionAuto(YogaFlexDirection axis, YogaDirection direction) {
        return computePosition(FlexDirectionUtil.inlineStartEdge(axis, direction), direction)
                .isAuto();
    }

    public boolean isFlexEndPositionDefined(YogaFlexDirection axis, YogaDirection direction) {
        return computePosition(FlexDirectionUtil.flexEndEdge(axis), direction).isDefined();
    }

    public boolean isFlexEndPositionAuto(YogaFlexDirection axis, YogaDirection direction) {
        return computePosition(FlexDirectionUtil.flexEndEdge(axis), direction).isAuto();
    }

    public boolean isInlineEndPositionDefined(YogaFlexDirection axis, YogaDirection direction) {
        return computePosition(FlexDirectionUtil.inlineEndEdge(axis, direction), direction)
                .isDefined();
    }

    public boolean isInlineEndPositionAuto(YogaFlexDirection axis, YogaDirection direction) {
        return computePosition(FlexDirectionUtil.inlineEndEdge(axis, direction), direction).isAuto();
    }

    public float computeFlexStartPosition(
            YogaFlexDirection axis,
            YogaDirection direction,
            float axisSize) {
        return computePosition(FlexDirectionUtil.flexStartEdge(axis), direction)
                .resolve(axisSize)
                .unwrapOrDefault(0.0f);
    }

    public float computeInlineStartPosition(
            YogaFlexDirection axis,
            YogaDirection direction,
            float axisSize) {
        return computePosition(FlexDirectionUtil.inlineStartEdge(axis, direction), direction)
                .resolve(axisSize)
                .unwrapOrDefault(0.0f);
    }

    public float computeFlexEndPosition(
            YogaFlexDirection axis,
            YogaDirection direction,
            float axisSize) {
        return computePosition(FlexDirectionUtil.flexEndEdge(axis), direction)
                .resolve(axisSize)
                .unwrapOrDefault(0.0f);
    }

    public float computeInlineEndPosition(
            YogaFlexDirection axis,
            YogaDirection direction,
            float axisSize) {
        return computePosition(FlexDirectionUtil.inlineEndEdge(axis, direction), direction)
                .resolve(axisSize)
                .unwrapOrDefault(0.0f);
    }

    public float computeFlexStartMargin(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return computeMargin(FlexDirectionUtil.flexStartEdge(axis), direction)
                .resolve(widthSize)
                .unwrapOrDefault(0.0f);
    }

    public float computeInlineStartMargin(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return computeMargin(FlexDirectionUtil.inlineStartEdge(axis, direction), direction)
                .resolve(widthSize)
                .unwrapOrDefault(0.0f);
    }

    public float computeFlexEndMargin(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return computeMargin(FlexDirectionUtil.flexEndEdge(axis), direction)
                .resolve(widthSize)
                .unwrapOrDefault(0.0f);
    }

    public float computeInlineEndMargin(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return computeMargin(FlexDirectionUtil.inlineEndEdge(axis, direction), direction)
                .resolve(widthSize)
                .unwrapOrDefault(0.0f);
    }

    public float computeFlexStartBorder(YogaFlexDirection axis, YogaDirection direction) {
        return Comparison.maxOrDefined(
                computeBorder(FlexDirectionUtil.flexStartEdge(axis), direction)
                        .resolve(0.0f)
                        .unwrap(),
                0.0f);
    }

    public float computeInlineStartBorder(YogaFlexDirection axis, YogaDirection direction) {
        return Comparison.maxOrDefined(
                computeBorder(FlexDirectionUtil.inlineStartEdge(axis, direction), direction)
                        .resolve(0.0f)
                        .unwrap(),
                0.0f);
    }

    public float computeFlexEndBorder(YogaFlexDirection axis, YogaDirection direction) {
        return Comparison.maxOrDefined(
                computeBorder(FlexDirectionUtil.flexEndEdge(axis), direction)
                        .resolve(0.0f)
                        .unwrap(),
                0.0f);
    }

    public float computeInlineEndBorder(YogaFlexDirection axis, YogaDirection direction) {
        return Comparison.maxOrDefined(
                computeBorder(FlexDirectionUtil.inlineEndEdge(axis, direction), direction)
                        .resolve(0.0f)
                        .unwrap(),
                0.0f);
    }

    public float computeFlexStartPadding(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return Comparison.maxOrDefined(
                computePadding(FlexDirectionUtil.flexStartEdge(axis), direction)
                        .resolve(widthSize)
                        .unwrap(),
                0.0f);
    }

    public float computeInlineStartPadding(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return Comparison.maxOrDefined(
                computePadding(FlexDirectionUtil.inlineStartEdge(axis, direction), direction)
                        .resolve(widthSize)
                        .unwrap(),
                0.0f);
    }

    public float computeFlexEndPadding(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return Comparison.maxOrDefined(
                computePadding(FlexDirectionUtil.flexEndEdge(axis), direction)
                        .resolve(widthSize)
                        .unwrap(),
                0.0f);
    }

    public float computeInlineEndPadding(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return Comparison.maxOrDefined(
                computePadding(FlexDirectionUtil.inlineEndEdge(axis, direction), direction)
                        .resolve(widthSize)
                        .unwrap(),
                0.0f);
    }

    public float computeInlineStartPaddingAndBorder(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return computeInlineStartPadding(axis, direction, widthSize) +
                computeInlineStartBorder(axis, direction);
    }

    public float computeFlexStartPaddingAndBorder(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return computeFlexStartPadding(axis, direction, widthSize) +
                computeFlexStartBorder(axis, direction);
    }

    public float computeInlineEndPaddingAndBorder(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return computeInlineEndPadding(axis, direction, widthSize) +
                computeInlineEndBorder(axis, direction);
    }

    public float computeFlexEndPaddingAndBorder(
            YogaFlexDirection axis,
            YogaDirection direction,
            float widthSize) {
        return computeFlexEndPadding(axis, direction, widthSize) +
                computeFlexEndBorder(axis, direction);
    }

    public float computePaddingAndBorderForDimension(
            YogaDirection direction,
            YogaDimension dimension,
            float widthSize) {
        YogaFlexDirection flexDirectionForDimension = dimension == YogaDimension.WIDTH
                ? YogaFlexDirection.ROW
                : YogaFlexDirection.COLUMN;

        return computeFlexStartPaddingAndBorder(
                flexDirectionForDimension, direction, widthSize) +
                computeFlexEndPaddingAndBorder(
                        flexDirectionForDimension, direction, widthSize);
    }

    public float computeBorderForAxis(YogaFlexDirection axis) {
        return computeInlineStartBorder(axis, YogaDirection.LTR) +
                computeInlineEndBorder(axis, YogaDirection.LTR);
    }

    public float computeMarginForAxis(YogaFlexDirection axis, float widthSize) {
        // The total margin for a given axis does not depend on the direction
        // so hardcoding LTR here to avoid piping direction to this function
        return computeInlineStartMargin(axis, YogaDirection.LTR, widthSize) +
                computeInlineEndMargin(axis, YogaDirection.LTR, widthSize);
    }

    public float computeGapForAxis(YogaFlexDirection axis, float ownerSize) {
        StyleLength gap = FlexDirectionUtil.isRow(axis) ? computeColumnGap() : computeRowGap();
        return Comparison.maxOrDefined(gap.resolve(ownerSize).unwrap(), 0.0f);
    }

    public boolean flexStartMarginIsAuto(YogaFlexDirection axis, YogaDirection direction) {
        return computeMargin(FlexDirectionUtil.flexStartEdge(axis), direction).isAuto();
    }

    public boolean flexEndMarginIsAuto(YogaFlexDirection axis, YogaDirection direction) {
        return computeMargin(FlexDirectionUtil.flexEndEdge(axis), direction).isAuto();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        YogaStyle other = (YogaStyle) obj;
        return direction == other.direction
                && flexDirection == other.flexDirection
                && justifyContent == other.justifyContent
                && alignContent == other.alignContent
                && alignItems == other.alignItems
                && alignSelf == other.alignSelf
                && positionType == other.positionType
                && flexWrap == other.flexWrap
                && overflow == other.overflow
                && display == other.display
                && numbersEqual(flex, pool, other.flex, other.pool)
                && numbersEqual(flexGrow, pool, other.flexGrow, other.pool)
                && numbersEqual(flexShrink, pool, other.flexShrink, other.pool)
                && lengthsEqual(flexBasis, pool, other.flexBasis, other.pool)
                && arraysEqual(margin, pool, other.margin, other.pool)
                && arraysEqual(position, pool, other.position, other.pool)
                && arraysEqual(padding, pool, other.padding, other.pool)
                && arraysEqual(border, pool, other.border, other.pool)
                && arraysEqual(gap, pool, other.gap, other.pool)
                && arraysEqual(dimensions, pool, other.dimensions, other.pool)
                && arraysEqual(minDimensions, pool, other.minDimensions, other.pool)
                && arraysEqual(maxDimensions, pool, other.maxDimensions, other.pool)
                && numbersEqual(aspectRatio, pool, other.aspectRatio, other.pool);
    }

    // Private helper methods

    private StyleLength computeColumnGap() {
        if (gap[YogaGutter.COLUMN.ordinal()].isDefined()) {
            return pool.getLength(gap[YogaGutter.COLUMN.ordinal()]);
        } else {
            return pool.getLength(gap[YogaGutter.ALL.ordinal()]);
        }
    }

    private StyleLength computeRowGap() {
        if (gap[YogaGutter.ROW.ordinal()].isDefined()) {
            return pool.getLength(gap[YogaGutter.ROW.ordinal()]);
        } else {
            return pool.getLength(gap[YogaGutter.ALL.ordinal()]);
        }
    }

    private StyleLength computeLeftEdge(StyleValueHandle[] edges, YogaDirection layoutDirection) {
        if (layoutDirection == YogaDirection.LTR &&
                edges[YogaEdge.START.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.START.ordinal()]);
        } else if (
                layoutDirection == YogaDirection.RTL &&
                        edges[YogaEdge.END.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.END.ordinal()]);
        } else if (edges[YogaEdge.LEFT.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.LEFT.ordinal()]);
        } else if (edges[YogaEdge.HORIZONTAL.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.HORIZONTAL.ordinal()]);
        } else {
            return pool.getLength(edges[YogaEdge.ALL.ordinal()]);
        }
    }

    private StyleLength computeTopEdge(StyleValueHandle[] edges) {
        if (edges[YogaEdge.TOP.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.TOP.ordinal()]);
        } else if (edges[YogaEdge.VERTICAL.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.VERTICAL.ordinal()]);
        } else {
            return pool.getLength(edges[YogaEdge.ALL.ordinal()]);
        }
    }

    private StyleLength computeRightEdge(StyleValueHandle[] edges, YogaDirection layoutDirection) {
        if (layoutDirection == YogaDirection.LTR &&
                edges[YogaEdge.END.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.END.ordinal()]);
        } else if (
                layoutDirection == YogaDirection.RTL &&
                        edges[YogaEdge.START.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.START.ordinal()]);
        } else if (edges[YogaEdge.RIGHT.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.RIGHT.ordinal()]);
        } else if (edges[YogaEdge.HORIZONTAL.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.HORIZONTAL.ordinal()]);
        } else {
            return pool.getLength(edges[YogaEdge.ALL.ordinal()]);
        }
    }

    private StyleLength computeBottomEdge(StyleValueHandle[] edges) {
        if (edges[YogaEdge.BOTTOM.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.BOTTOM.ordinal()]);
        } else if (edges[YogaEdge.VERTICAL.ordinal()].isDefined()) {
            return pool.getLength(edges[YogaEdge.VERTICAL.ordinal()]);
        } else {
            return pool.getLength(edges[YogaEdge.ALL.ordinal()]);
        }
    }

    private StyleLength computePosition(YogaPhysicalEdge edge, YogaDirection direction) {
        switch (edge) {
            case LEFT:
                return computeLeftEdge(position, direction);
            case TOP:
                return computeTopEdge(position);
            case RIGHT:
                return computeRightEdge(position, direction);
            case BOTTOM:
                return computeBottomEdge(position);
            default:
                throw new IllegalArgumentException("Invalid physical edge");
        }
    }

    private StyleLength computeMargin(YogaPhysicalEdge edge, YogaDirection direction) {
        switch (edge) {
            case LEFT:
                return computeLeftEdge(margin, direction);
            case TOP:
                return computeTopEdge(margin);
            case RIGHT:
                return computeRightEdge(margin, direction);
            case BOTTOM:
                return computeBottomEdge(margin);
            default:
                throw new IllegalArgumentException("Invalid physical edge");
        }
    }

    private StyleLength computePadding(YogaPhysicalEdge edge, YogaDirection direction) {
        switch (edge) {
            case LEFT:
                return computeLeftEdge(padding, direction);
            case TOP:
                return computeTopEdge(padding);
            case RIGHT:
                return computeRightEdge(padding, direction);
            case BOTTOM:
                return computeBottomEdge(padding);
            default:
                throw new IllegalArgumentException("Invalid physical edge");
        }
    }

    private StyleLength computeBorder(YogaPhysicalEdge edge, YogaDirection direction) {
        switch (edge) {
            case LEFT:
                return computeLeftEdge(border, direction);
            case TOP:
                return computeTopEdge(border);
            case RIGHT:
                return computeRightEdge(border, direction);
            case BOTTOM:
                return computeBottomEdge(border);
            default:
                throw new IllegalArgumentException("Invalid physical edge");
        }
    }

    // Static helpers for comparing values

    private static boolean numbersEqual(
            StyleValueHandle lhsHandle,
            StyleValuePool lhsPool,
            StyleValueHandle rhsHandle,
            StyleValuePool rhsPool) {
        return (lhsHandle.isUndefined() && rhsHandle.isUndefined()) ||
                (lhsPool.getNumber(lhsHandle).equals(rhsPool.getNumber(rhsHandle)));
    }

    private static boolean lengthsEqual(
            StyleValueHandle lhsHandle,
            StyleValuePool lhsPool,
            StyleValueHandle rhsHandle,
            StyleValuePool rhsPool) {
        return (lhsHandle.isUndefined() && rhsHandle.isUndefined()) ||
                (lhsPool.getLength(lhsHandle).equals(rhsPool.getLength(rhsHandle)));
    }

    private static boolean arraysEqual(
            StyleValueHandle[] lhs,
            StyleValuePool lhsPool,
            StyleValueHandle[] rhs,
            StyleValuePool rhsPool) {
        if (lhs.length != rhs.length) {
            return false;
        }

        for (int i = 0; i < lhs.length; i++) {
            if (!lengthsEqual(lhs[i], lhsPool, rhs[i], rhsPool)) {
                return false;
            }
        }

        return true;
    }
}