/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.CalculateLayout;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.algorithm.FlexDirectionUtil;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.MutableYogaConfig;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaConfig;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.YogaEvent;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event.YogaEventType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node.LayoutResults;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node.LayoutableChildren;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.FloatOptional;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.YogaStyle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.Comparison.*;

public class YogaNode implements YogaProps {
    private boolean hasNewLayout = true;
    private boolean isReferenceBaseline = false;
    private boolean isDirty = true;
    private boolean alwaysFormsContainingBlock = false;
    private YogaNodeType nodeType = YogaNodeType.DEFAULT;
    private Object context = null;
    private YogaMeasureFunction measureFunc = null;
    private YogaBaselineFunction baselineFunc = null;
    private YogaDirtiedFunction dirtiedFunc = null;
    private YogaStyle style;
    private LayoutResults layout;
    private int lineIndex = 0;
    private int contentsChildrenCount = 0;
    private YogaNode owner = null;
    private List<YogaNode> children;
    private YogaConfig config;
    private StyleSizeLength[] processedDimensions = new StyleSizeLength[2];
    @Nullable
    private String debugName;

    // Constructors
    public YogaNode() {
        this(YogaConfig.getDefault());
    }

    public YogaNode(YogaConfig config) {
        Objects.requireNonNull(config, "Attempting to construct Node with null config");
        this.config = config;
        this.style = new YogaStyle();
        this.layout = new LayoutResults();
        this.children = new ArrayList<>();

        this.processedDimensions[YogaDimension.WIDTH.ordinal()] = StyleSizeLength.undefined();
        this.processedDimensions[YogaDimension.HEIGHT.ordinal()] = StyleSizeLength.undefined();

        if (config.useWebDefaults()) {
            useWebDefaults();
        }
    }

    // Copy constructor - note we don't perform deep copy of children
    public YogaNode(YogaNode node) {
        this.hasNewLayout = node.hasNewLayout;
        this.isReferenceBaseline = node.isReferenceBaseline;
        this.isDirty = node.isDirty;
        this.alwaysFormsContainingBlock = node.alwaysFormsContainingBlock;
        this.nodeType = node.nodeType;
        this.context = node.context;
        this.measureFunc = node.measureFunc;
        this.baselineFunc = node.baselineFunc;
        this.dirtiedFunc = node.dirtiedFunc;
        this.style = new YogaStyle(node.style);
        this.layout = new LayoutResults(node.layout);
        this.lineIndex = node.lineIndex;
        this.contentsChildrenCount = node.contentsChildrenCount;
        this.owner = node.owner;
        this.children = new ArrayList<>(node.children);
        this.config = node.config;
        this.processedDimensions = node.processedDimensions.clone();
    }

    private static String f(float f) {
        if ((float) (int) f == f) {
            return String.valueOf((int) f);
        }
        return String.valueOf(f);
    }

    // Getters
    public Object getContext() {
        return context;
    }

    // Setters
    public void setContext(Object context) {
        this.context = context;
    }

    public boolean alwaysFormsContainingBlock() {
        return alwaysFormsContainingBlock;
    }

    public boolean hasNewLayout() {
        return hasNewLayout;
    }

    public YogaNodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(YogaNodeType nodeType) {
        this.nodeType = nodeType;
    }

    public boolean hasMeasureFunc() {
        return measureFunc != null;
    }

    public YogaSize measure(
            float availableWidth,
            YogaMeasureMode widthMode,
            float availableHeight,
            YogaMeasureMode heightMode) {
        YogaSize size = measureFunc.measure(
                this,
                availableWidth,
                widthMode,
                availableHeight,
                heightMode);

        if (isUndefined(size.width()) || size.width() < 0 ||
                isUndefined(size.height()) || size.height() < 0) {
            // TODO: Add logging mechanism
            System.err.printf(
                    "Measure function returned an invalid dimension to Yoga: [width=%f, height=%f]%n",
                    size.width(),
                    size.height());
            return new YogaSize(
                    maxOrDefined(0.0f, size.width()),
                    maxOrDefined(0.0f, size.height()));
        }

        return size;
    }

    public boolean isBaselineDefined() {
        return baselineFunc != null;
    }

    public float baseline(float width, float height) {
        return baselineFunc.baseline(this, width, height);
    }

    public float dimensionWithMargin(YogaFlexDirection axis, float widthSize) {
        return getLayout().measuredDimension(FlexDirectionUtil.dimension(axis)) +
                style.computeMarginForAxis(axis, widthSize);
    }

    public boolean isLayoutDimensionDefined(YogaFlexDirection axis) {
        float value = getLayout().measuredDimension(FlexDirectionUtil.dimension(axis));
        return isDefined(value) && value >= 0.0f;
    }

    /**
     * Whether the node has a "definite length" along the given axis.
     * https://www.w3.org/TR/css-sizing-3/#definite
     */
    public boolean hasDefiniteLength(YogaDimension dimension, float ownerSize) {
        FloatOptional usedValue = getProcessedDimension(dimension).resolve(ownerSize);
        return usedValue.isDefined() && usedValue.unwrap() >= 0.0f;
    }

    public boolean hasErrata(YogaErrata errata) {
        return config.hasErrata(errata);
    }

    public YogaDirtiedFunction getDirtiedFunc() {
        return dirtiedFunc;
    }

    public void setDirtiedFunc(YogaDirtiedFunction dirtiedFunc) {
        this.dirtiedFunc = dirtiedFunc;
    }

    public YogaStyle getStyle() {
        return style;
    }

    public void setStyle(YogaStyle style) {
        this.style = style;
    }

    public LayoutResults getLayout() {
        return layout;
    }

    public void setLayout(LayoutResults layout) {
        this.layout = layout;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public void setLineIndex(int lineIndex) {
        this.lineIndex = lineIndex;
    }

    public boolean isReferenceBaseline() {
        return isReferenceBaseline;
    }

    public YogaNode getOwner() {
        return owner;
    }

    public void setOwner(YogaNode owner) {
        this.owner = owner;
    }

    public List<YogaNode> getChildren() {
        return children;
    }

    public void setChildren(List<YogaNode> children) {
        if (children == null || children.isEmpty()) {
            if (this.getChildCount() > 0) {
                for (YogaNode child : this.getChildren()) {
                    child.setLayout(new LayoutResults());
                    child.setOwner(null);
                }
                this.children = new ArrayList<>();
                this.markDirtyAndPropagate();
            }
        } else {
            if (this.getChildCount() > 0) {
                for (YogaNode oldChild : this.getChildren()) {
                    // Our new children may have nodes in common with the old children. We
                    // don't reset these common nodes.
                    if (!children.contains(oldChild)) {
                        oldChild.setLayout(new LayoutResults());
                        oldChild.setOwner(null);
                    }
                }
            }
            this.children = new ArrayList<>(children);
            for (YogaNode child : children) {
                child.setOwner(this);
            }
            this.markDirtyAndPropagate();
        }
    }

    public YogaNode getChild(int index) {
        return children.get(index);
    }

    public int getChildCount() {
        return children.size();
    }

    public LayoutableChildren getLayoutChildren() {
        return new LayoutableChildren(this);
    }

    public int getLayoutChildCount() {
        if (contentsChildrenCount == 0) {
            return children.size();
        } else {
            int count = 0;
            for (var iter = getLayoutChildren().iterator(); iter.hasNext(); iter.next()) {
                count++;
            }
            return count;
        }
    }

    public YogaConfig getConfig() {
        return config;
    }

    public void setConfig(MutableYogaConfig config) {
        Objects.requireNonNull(config, "Attempting to set a null config on a Node");

        if (config.useWebDefaults() != this.config.useWebDefaults()) {
            throw new IllegalArgumentException(
                    "UseWebDefaults may not be changed after constructing a Node");
        }

        if (YogaConfig.configUpdateInvalidatesLayout(this.config, config)) {
            markDirtyAndPropagate();
            layout.configVersion = 0;
        } else {
            // If the config is functionally the same, then align the configVersion so
            // that we can reuse the layout cache
            layout.configVersion = config.getVersion();
        }

        this.config = config;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean isDirty) {
        if (this.isDirty == isDirty) {
            return;
        }
        this.isDirty = isDirty;
        if (isDirty && (dirtiedFunc != null)) {
            dirtiedFunc.onDirtied(this);
        }
    }

    public StyleSizeLength getProcessedDimension(YogaDimension dimension) {
        return processedDimensions[dimension.ordinal()];
    }

    public FloatOptional getResolvedDimension(
            YogaDirection direction,
            YogaDimension dimension,
            float referenceLength,
            float ownerWidth) {
        FloatOptional value = getProcessedDimension(dimension).resolve(referenceLength);
        if (style.getBoxSizing() == YogaBoxSizing.BORDER_BOX) {
            return value;
        }

        FloatOptional dimensionPaddingAndBorder = FloatOptional.of(
                style.computePaddingAndBorderForDimension(direction, dimension, ownerWidth));

        return value.add(
                dimensionPaddingAndBorder.isDefined() ? dimensionPaddingAndBorder
                        : FloatOptional.of(0.0f));
    }

    public void setAlwaysFormsContainingBlock(boolean alwaysFormsContainingBlock) {
        this.alwaysFormsContainingBlock = alwaysFormsContainingBlock;
    }

    public void setHasNewLayout(boolean hasNewLayout) {
        this.hasNewLayout = hasNewLayout;
    }

    public void markLayoutSeen() {
        setHasNewLayout(false);
    }

    public void setLayoutMargin(float margin, YogaPhysicalEdge edge) {
        layout.setMargin(edge, margin);
    }

    public void setLayoutBorder(float border, YogaPhysicalEdge edge) {
        layout.setBorder(edge, border);
    }

    public void setLayoutPadding(float padding, YogaPhysicalEdge edge) {
        layout.setPadding(edge, padding);
    }

    public void setLayoutLastOwnerDirection(YogaDirection direction) {
        layout.lastOwnerDirection = direction;
    }

    public void setLayoutComputedFlexBasis(FloatOptional computedFlexBasis) {
        layout.computedFlexBasis = computedFlexBasis;
    }

    public void setLayoutPosition(float position, YogaPhysicalEdge edge) {
        layout.setPosition(edge, position);
    }

    public void setLayoutComputedFlexBasisGeneration(long computedFlexBasisGeneration) {
        layout.computedFlexBasisGeneration = computedFlexBasisGeneration;
    }

    public void setLayoutMeasuredDimension(float measuredDimension, YogaDimension dimension) {
        layout.setMeasuredDimension(dimension, measuredDimension);
    }

    public void setLayoutHadOverflow(boolean hadOverflow) {
        layout.setHadOverflow(hadOverflow);
    }

    public void setLayoutDimension(float lengthValue, YogaDimension dimension) {
        layout.setDimension(dimension, lengthValue);
    }

    public @Nullable String getDebugName() {
        return debugName;
    }

    public void setDebugName(@Nullable String debugName) {
        this.debugName = debugName;
    }

    // Other methods
    public StyleSizeLength processFlexBasis() {
        StyleSizeLength flexBasis = style.getFlexBasis();
        if (!flexBasis.isAuto() && !flexBasis.isUndefined()) {
            return flexBasis;
        }
        if (style.getFlex().isDefined() && style.getFlex().unwrap() > 0.0f) {
            return config.useWebDefaults() ? StyleSizeLength.ofAuto()
                    : StyleSizeLength.points(0);
        }
        return StyleSizeLength.ofAuto();
    }

    public FloatOptional resolveFlexBasis(
            YogaDirection direction,
            YogaFlexDirection flexDirection,
            float referenceLength,
            float ownerWidth) {
        FloatOptional value = processFlexBasis().resolve(referenceLength);
        if (style.getBoxSizing() == YogaBoxSizing.BORDER_BOX) {
            return value;
        }

        YogaDimension dim = FlexDirectionUtil.dimension(flexDirection);
        FloatOptional dimensionPaddingAndBorder = FloatOptional.of(
                style.computePaddingAndBorderForDimension(direction, dim, ownerWidth));

        return value.add(
                dimensionPaddingAndBorder.isDefined() ? dimensionPaddingAndBorder
                        : FloatOptional.of(0.0f));
    }

    public void processDimensions() {
        for (YogaDimension dim : List.of(YogaDimension.WIDTH, YogaDimension.HEIGHT)) {
            if (style.getMaxDimension(dim).isDefined() &&
                    style.getMaxDimension(dim).inexactEquals(style.getMinDimension(dim))) {
                processedDimensions[dim.ordinal()] = style.getMaxDimension(dim);
            } else {
                processedDimensions[dim.ordinal()] = style.getDimension(dim);
            }
        }
    }

    public YogaDirection resolveDirection(YogaDirection ownerDirection) {
        if (style.getDirection() == YogaDirection.INHERIT) {
            return ownerDirection != YogaDirection.INHERIT ? ownerDirection
                    : YogaDirection.LTR;
        } else {
            return style.getDirection();
        }
    }

    public void clearChildren() {
        children.clear();
    }

    // Methods related to child management
    public void replaceChild(YogaNode oldChild, YogaNode newChild) {
        if (oldChild.getStyle().getDisplay() == YogaDisplay.CONTENTS &&
                newChild.getStyle().getDisplay() != YogaDisplay.CONTENTS) {
            contentsChildrenCount--;
        } else if (
                oldChild.getStyle().getDisplay() != YogaDisplay.CONTENTS &&
                        newChild.getStyle().getDisplay() == YogaDisplay.CONTENTS) {
            contentsChildrenCount++;
        }

        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == oldChild) {
                children.set(i, newChild);
                break;
            }
        }
    }

    public void replaceChild(YogaNode child, int index) {
        YogaNode previousChild = children.get(index);
        if (previousChild.getStyle().getDisplay() == YogaDisplay.CONTENTS &&
                child.getStyle().getDisplay() != YogaDisplay.CONTENTS) {
            contentsChildrenCount--;
        } else if (
                previousChild.getStyle().getDisplay() != YogaDisplay.CONTENTS &&
                        child.getStyle().getDisplay() == YogaDisplay.CONTENTS) {
            contentsChildrenCount++;
        }

        children.set(index, child);
    }

    public void addChildAt(YogaNode child, int index) {
        insertChild(child, index);
        child.setOwner(this);
        markDirtyAndPropagate();
    }

    public void insertChild(YogaNode child, int index) {
        if (child.getOwner() != null) {
            throw new IllegalArgumentException("Child already has a owner, it must be removed first.");
        }

        if (measureFunc != null) {
            throw new IllegalArgumentException("Cannot add child: Nodes with measure functions cannot have children.");
        }

        if (child.getStyle().getDisplay() == YogaDisplay.CONTENTS) {
            contentsChildrenCount++;
        }

        children.add(index, child);
    }

    public boolean removeChild(YogaNode child) {
        if (getChildCount() == 0) {
            // This is an empty set. Nothing to remove.
            return false;
        }

        int index = children.indexOf(child);
        if (index != -1) {
            if (child.getStyle().getDisplay() == YogaDisplay.CONTENTS) {
                contentsChildrenCount--;
            }
            children.remove(index);
            return true;
        }
        return false;
    }

    /**
     * Implements YGNodeRemoveChild
     */
    public boolean removeChildAndInvalidate(YogaNode child) {
        var childOwner = child.getOwner();
        if (removeChild(child)) {
            if (this == childOwner) {
                child.setLayout(new LayoutResults()); // layout is no longer valid
                child.setOwner(null);
            }
            markDirtyAndPropagate();
            return true;
        }
        return false;
    }

    public void removeChild(int index) {
        if (children.get(index).getStyle().getDisplay() == YogaDisplay.CONTENTS) {
            contentsChildrenCount--;
        }
        children.remove(index);
    }

    public void cloneChildrenIfNeeded() {
        for (int i = 0; i < children.size(); i++) {
            YogaNode child = children.get(i);
            if (child.getOwner() != this) {
                YogaNode clone = config.cloneNode(child, this, i);
                clone.setOwner(this);
                children.set(i, clone);
            }
        }
    }

    public void markDirtyAndPropagate() {
        if (!isDirty) {
            setDirty(true);
            setLayoutComputedFlexBasis(FloatOptional.of());
            if (owner != null) {
                owner.markDirtyAndPropagate();
            }
        }
    }

    public float resolveFlexGrow() {
        // Root nodes flexGrow should always be 0
        if (owner == null) {
            return 0.0f;
        }
        if (style.getFlexGrow().isDefined()) {
            return style.getFlexGrow().unwrap();
        }
        if (style.getFlex().isDefined() && style.getFlex().unwrap() > 0.0f) {
            return style.getFlex().unwrap();
        }
        return YogaStyle.DEFAULT_FLEX_GROW;
    }

    public float resolveFlexShrink() {
        if (owner == null) {
            return 0.0f;
        }
        if (style.getFlexShrink().isDefined()) {
            return style.getFlexShrink().unwrap();
        }
        if (!config.useWebDefaults() && style.getFlex().isDefined() &&
                style.getFlex().unwrap() < 0.0f) {
            return -style.getFlex().unwrap();
        }
        return config.useWebDefaults() ? YogaStyle.WEB_DEFAULT_FLEX_SHRINK
                : YogaStyle.DEFAULT_FLEX_SHRINK;
    }

    public boolean isNodeFlexible() {
        return (style.getPositionType() != YogaPositionType.ABSOLUTE) &&
                (resolveFlexGrow() != 0 || resolveFlexShrink() != 0);
    }

    public void reset() {
        if (!children.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot reset a node which still has children attached");
        }
        if (owner != null) {
            throw new IllegalStateException(
                    "Cannot reset a node still attached to an owner");
        }

        // Re-initialize this node
        var savedConfig = this.config;
        this.hasNewLayout = true;
        this.isReferenceBaseline = false;
        this.isDirty = true;
        this.alwaysFormsContainingBlock = false;
        this.nodeType = YogaNodeType.DEFAULT;
        this.context = null;
        this.measureFunc = null;
        this.baselineFunc = null;
        this.dirtiedFunc = null;
        this.style = new YogaStyle();
        this.layout = new LayoutResults();
        this.lineIndex = 0;
        this.contentsChildrenCount = 0;
        this.owner = null;
        this.children = new ArrayList<>();
        this.config = savedConfig;
        this.processedDimensions[YogaDimension.WIDTH.ordinal()] = StyleSizeLength.undefined();
        this.processedDimensions[YogaDimension.HEIGHT.ordinal()] = StyleSizeLength.undefined();

        if (config.useWebDefaults()) {
            useWebDefaults();
        }
    }

    private void useWebDefaults() {
        style.setFlexDirection(YogaFlexDirection.ROW);
        style.setAlignContent(YogaAlign.STRETCH);
    }

    private float relativePosition(
            YogaFlexDirection axis,
            YogaDirection direction,
            float axisSize) {
        if (style.getPositionType() == YogaPositionType.STATIC) {
            return 0;
        }
        if (style.isInlineStartPositionDefined(axis, direction) &&
                !style.isInlineStartPositionAuto(axis, direction)) {
            return style.computeInlineStartPosition(axis, direction, axisSize);
        }

        return -1 * style.computeInlineEndPosition(axis, direction, axisSize);
    }

    public void setPosition(
            YogaDirection direction,
            float ownerWidth,
            float ownerHeight) {
        /* Root nodes should be always layouted as LTR, so we don't return negative values. */
        YogaDirection directionRespectingRoot = owner != null ? direction : YogaDirection.LTR;
        YogaFlexDirection mainAxis = FlexDirectionUtil.resolveDirection(
                style.getFlexDirection(), directionRespectingRoot);
        YogaFlexDirection crossAxis = FlexDirectionUtil.resolveCrossDirection(
                mainAxis, directionRespectingRoot);

        // In the case of position static these are just 0. See:
        // https://www.w3.org/TR/css-position-3/#valdef-position-static
        float relativePositionMain = relativePosition(
                mainAxis,
                directionRespectingRoot,
                FlexDirectionUtil.isRow(mainAxis) ? ownerWidth : ownerHeight);
        float relativePositionCross = relativePosition(
                crossAxis,
                directionRespectingRoot,
                FlexDirectionUtil.isRow(mainAxis) ? ownerHeight : ownerWidth);

        YogaPhysicalEdge mainAxisLeadingEdge = FlexDirectionUtil.inlineStartEdge(mainAxis, direction);
        YogaPhysicalEdge mainAxisTrailingEdge = FlexDirectionUtil.inlineEndEdge(mainAxis, direction);
        YogaPhysicalEdge crossAxisLeadingEdge = FlexDirectionUtil.inlineStartEdge(crossAxis, direction);
        YogaPhysicalEdge crossAxisTrailingEdge = FlexDirectionUtil.inlineEndEdge(crossAxis, direction);

        setLayoutPosition(
                (style.computeInlineStartMargin(mainAxis, direction, ownerWidth) +
                        relativePositionMain),
                mainAxisLeadingEdge);
        setLayoutPosition(
                (style.computeInlineEndMargin(mainAxis, direction, ownerWidth) +
                        relativePositionMain),
                mainAxisTrailingEdge);
        setLayoutPosition(
                (style.computeInlineStartMargin(crossAxis, direction, ownerWidth) +
                        relativePositionCross),
                crossAxisLeadingEdge);
        setLayoutPosition(
                (style.computeInlineEndMargin(crossAxis, direction, ownerWidth) +
                        relativePositionCross),
                crossAxisTrailingEdge);
    }

    public YogaWrap getWrap() {
        return style.getFlexWrap();
    }

    @Override
    public void setWrap(YogaWrap flexWrap) {
        if (style.getFlexWrap() != flexWrap) {
            style.setFlexWrap(flexWrap);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setIsReferenceBaseline(boolean isReferenceBaseline) {
        this.isReferenceBaseline = isReferenceBaseline;
    }

    public void setMeasureFunction(YogaMeasureFunction measureFunc) {
        if (measureFunc == null) {
            setNodeType(YogaNodeType.DEFAULT);
        } else {
            if (!children.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot set measure function: Nodes with measure functions cannot have children.");
            }
            setNodeType(YogaNodeType.TEXT);
        }

        this.measureFunc = measureFunc;
    }

    public void setBaselineFunction(YogaBaselineFunction baselineFunc) {
        this.baselineFunc = baselineFunc;
    }

    @Override
    public YogaValue getWidth() {
        return style.getDimension(YogaDimension.WIDTH).asYogaValue();
    }

    @Override
    public void setWidth(StyleSizeLength length) {
        if (!style.getDimension(YogaDimension.WIDTH).equals(length)) {
            style.setDimension(YogaDimension.WIDTH, length);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaValue getMinWidth() {
        return style.getMinDimension(YogaDimension.WIDTH).asYogaValue();
    }

    @Override
    public void setMinWidth(StyleSizeLength length) {
        if (!style.getMinDimension(YogaDimension.WIDTH).equals(length)) {
            style.setMinDimension(YogaDimension.WIDTH, length);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaValue getMaxWidth() {
        return style.getMaxDimension(YogaDimension.WIDTH).asYogaValue();
    }

    @Override
    public void setMaxWidth(StyleSizeLength length) {
        if (!style.getMaxDimension(YogaDimension.WIDTH).equals(length)) {
            style.setMaxDimension(YogaDimension.WIDTH, length);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaValue getHeight() {
        return style.getDimension(YogaDimension.HEIGHT).asYogaValue();
    }

    @Override
    public void setHeight(StyleSizeLength length) {
        if (!style.getDimension(YogaDimension.HEIGHT).equals(length)) {
            style.setDimension(YogaDimension.HEIGHT, length);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaValue getMinHeight() {
        return style.getMinDimension(YogaDimension.HEIGHT).asYogaValue();
    }

    @Override
    public void setMinHeight(StyleSizeLength length) {
        if (!style.getMinDimension(YogaDimension.HEIGHT).equals(length)) {
            style.setMinDimension(YogaDimension.HEIGHT, length);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaValue getMaxHeight() {
        return style.getMaxDimension(YogaDimension.HEIGHT).asYogaValue();
    }

    @Override
    public void setMaxHeight(StyleSizeLength length) {
        if (!style.getMaxDimension(YogaDimension.HEIGHT).equals(length)) {
            style.setMaxDimension(YogaDimension.HEIGHT, length);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaDirection getStyleDirection() {
        return style.getDirection();
    }

    @Override
    public YogaFlexDirection getFlexDirection() {
        return style.getFlexDirection();
    }

    @Override
    public void setFlexDirection(YogaFlexDirection flexDirection) {
        if (style.getFlexDirection() != flexDirection) {
            style.setFlexDirection(flexDirection);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaJustify getJustifyContent() {
        return style.getJustifyContent();
    }

    @Override
    public void setJustifyContent(YogaJustify justifyContent) {
        if (style.getJustifyContent() != justifyContent) {
            style.setJustifyContent(justifyContent);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaAlign getAlignItems() {
        return style.getAlignItems();
    }

    @Override
    public void setAlignItems(YogaAlign alignItems) {
        if (style.getAlignItems() != alignItems) {
            style.setAlignItems(alignItems);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaAlign getAlignSelf() {
        return style.getAlignSelf();
    }

    @Override
    public void setAlignSelf(YogaAlign alignSelf) {
        if (style.getAlignSelf() != alignSelf) {
            style.setAlignSelf(alignSelf);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaAlign getAlignContent() {
        return style.getAlignContent();
    }

    @Override
    public void setAlignContent(YogaAlign alignContent) {
        if (style.getAlignContent() != alignContent) {
            style.setAlignContent(alignContent);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaPositionType getPositionType() {
        return style.getPositionType();
    }

    @Override
    public void setPositionType(YogaPositionType positionType) {
        if (style.getPositionType() != positionType) {
            style.setPositionType(positionType);
            markDirtyAndPropagate();
        }
    }

    @Override
    public float getFlexGrow() {
        return style.getFlexGrow().unwrapOrDefault(YogaStyle.DEFAULT_FLEX_GROW);
    }

    @Override
    public void setFlexGrow(float flexGrow) {
        var value = FloatOptional.of(flexGrow);
        if (!style.getFlexGrow().equals(value)) {
            style.setFlexGrow(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public float getFlexShrink() {
        return style.getFlexShrink().unwrapOrDefault(
                config.useWebDefaults() ? YogaStyle.WEB_DEFAULT_FLEX_SHRINK
                        : YogaStyle.DEFAULT_FLEX_SHRINK);
    }

    @Override
    public void setFlexShrink(float flexShrink) {
        var value = FloatOptional.of(flexShrink);
        if (!style.getFlexShrink().equals(value)) {
            style.setFlexShrink(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaValue getFlexBasis() {
        return style.getFlexBasis().asYogaValue();
    }

    @Override
    public void setFlexBasis(float flexBasis) {
        var value = StyleSizeLength.points(flexBasis);
        if (!style.getFlexBasis().equals(value)) {
            style.setFlexBasis(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public float getAspectRatio() {
        return style.getAspectRatio().unwrapOrDefault(YogaConstants.UNDEFINED);
    }

    @Override
    public void setAspectRatio(float aspectRatio) {
        var value = FloatOptional.of(aspectRatio);
        if (!style.getAspectRatio().equals(value)) {
            style.setAspectRatio(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaValue getMargin(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge) {
        return style.getMargin(edge).asYogaValue();
    }

    @Override
    public YogaValue getPadding(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge) {
        return style.getPadding(edge).asYogaValue();
    }

    @Override
    public YogaValue getPosition(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge) {
        return style.getPosition(edge).asYogaValue();
    }

    @Override
    public float getBorder(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge) {
        var border = style.getBorder(edge);
        if (border.isUndefined() || border.isAuto()) {
            return YogaConstants.UNDEFINED;
        }

        return border.asYogaValue().value;
    }

    public YogaValue getGap(YogaGutter gutter) {
        return style.getGap(gutter).asYogaValue();
    }

    public void setGap(YogaGutter gutter, StyleLength value) {
        if (!style.getGap(gutter).equals(value)) {
            style.setGap(gutter, value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public YogaBoxSizing getBoxSizing() {
        return style.getBoxSizing();
    }

    @Override
    public void setBoxSizing(YogaBoxSizing boxSizing) {
        if (style.getBoxSizing() != boxSizing) {
            style.setBoxSizing(boxSizing);
            markDirtyAndPropagate();
        }
    }

    public YogaOverflow getOverflow() {
        return style.getOverflow();
    }

    public void setOverflow(YogaOverflow overflow) {
        if (style.getOverflow() != overflow) {
            style.setOverflow(overflow);
            markDirtyAndPropagate();
        }
    }

    public YogaDisplay getDisplay() {
        return style.getDisplay();
    }

    public void setDisplay(YogaDisplay display) {
        if (style.getDisplay() != display) {
            style.setDisplay(display);
            markDirtyAndPropagate();
        }
    }

    public float getFlex() {
        return style.getFlex().unwrapOrDefault(YogaConstants.UNDEFINED);
    }

    @Override
    public void setFlex(float flex) {
        var value = FloatOptional.of(flex);
        if (!style.getFlex().equals(value)) {
            style.setFlex(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setFlexBasisAuto() {
        var value = StyleSizeLength.ofAuto();
        if (!style.getFlexBasis().equals(value)) {
            style.setFlexBasis(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setFlexBasisPercent(float percent) {
        var value = StyleSizeLength.percent(percent);
        if (!style.getFlexBasis().equals(value)) {
            style.setFlexBasis(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setFlexBasisMaxContent() {
        var value = StyleSizeLength.ofMaxContent();
        if (!style.getFlexBasis().equals(value)) {
            style.setFlexBasis(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setFlexBasisFitContent() {
        var value = StyleSizeLength.ofFitContent();
        if (!style.getFlexBasis().equals(value)) {
            style.setFlexBasis(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setFlexBasisStretch() {
        var value = StyleSizeLength.ofStretch();
        if (!style.getFlexBasis().equals(value)) {
            style.setFlexBasis(value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setDirection(YogaDirection direction) {
        if (style.getDirection() != direction) {
            style.setDirection(direction);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setBorder(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float border) {
        var length = StyleLength.points(border);
        if (!style.getBorder(edge).equals(length)) {
            style.setBorder(edge, length);
            markDirtyAndPropagate();
        }
    }

    public boolean isMeasureDefined() {
        return measureFunc != null;
    }

    public @Nullable Object getData() {
        return getContext();
    }

    public void setData(Object data) {
        setContext(data);
    }

    /**
     * 这    是    相    对    于    父    节    点    的
     */
    public float getLayoutX() {
        return layout.position(YogaPhysicalEdge.LEFT);
    }

    /**
     * 这    是    相    对    于    父    节    点    的
     */
    public float getLayoutY() {
        return layout.position(YogaPhysicalEdge.TOP);
    }

    public float getLayoutRight() {
        return layout.position(YogaPhysicalEdge.RIGHT);
    }

    public float getLayoutBottom() {
        return layout.position(YogaPhysicalEdge.BOTTOM);
    }

    public float getLayoutWidth() {
        return layout.dimension(YogaDimension.WIDTH);
    }

    public float getLayoutHeight() {
        return layout.dimension(YogaDimension.HEIGHT);
    }

    public float getLayoutMargin(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge) {
        return switch (edge) {
            case LEFT -> layout.margin(YogaPhysicalEdge.LEFT);
            case TOP -> layout.margin(YogaPhysicalEdge.TOP);
            case RIGHT -> layout.margin(YogaPhysicalEdge.RIGHT);
            case BOTTOM -> layout.margin(YogaPhysicalEdge.BOTTOM);
            case START -> getLayoutDirection() == YogaDirection.RTL
                    ? layout.margin(YogaPhysicalEdge.RIGHT)
                    : layout.margin(YogaPhysicalEdge.LEFT);
            case END -> getLayoutDirection() == YogaDirection.RTL
                    ? layout.margin(YogaPhysicalEdge.LEFT)
                    : layout.margin(YogaPhysicalEdge.RIGHT);
            default -> throw new IllegalArgumentException("Cannot get layout margins of multi-edge shorthands");
        };
    }

    public float getLayoutPadding(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge) {
        return switch (edge) {
            case LEFT -> layout.padding(YogaPhysicalEdge.LEFT);
            case TOP -> layout.padding(YogaPhysicalEdge.TOP);
            case RIGHT -> layout.padding(YogaPhysicalEdge.RIGHT);
            case BOTTOM -> layout.padding(YogaPhysicalEdge.BOTTOM);
            case START -> getLayoutDirection() == YogaDirection.RTL
                    ? layout.padding(YogaPhysicalEdge.RIGHT)
                    : layout.padding(YogaPhysicalEdge.LEFT);
            case END -> getLayoutDirection() == YogaDirection.RTL
                    ? layout.padding(YogaPhysicalEdge.LEFT)
                    : layout.padding(YogaPhysicalEdge.RIGHT);
            default -> throw new IllegalArgumentException("Cannot get layout padding of multi-edge shorthands");
        };
    }

    public float getLayoutBorder(YogaEdge edge) {
        return switch (edge) {
            case LEFT -> layout.border(YogaPhysicalEdge.LEFT);
            case TOP -> layout.border(YogaPhysicalEdge.TOP);
            case RIGHT -> layout.border(YogaPhysicalEdge.RIGHT);
            case BOTTOM -> layout.border(YogaPhysicalEdge.BOTTOM);
            case START -> getLayoutDirection() == YogaDirection.RTL
                    ? layout.border(YogaPhysicalEdge.RIGHT)
                    : layout.border(YogaPhysicalEdge.LEFT);
            case END -> getLayoutDirection() == YogaDirection.RTL
                    ? layout.border(YogaPhysicalEdge.LEFT)
                    : layout.border(YogaPhysicalEdge.RIGHT);
            default -> throw new IllegalArgumentException("Cannot get layout border of multi-edge shorthands");
        };
    }

    public YogaDirection getLayoutDirection() {
        return layout.direction();
    }

    public void setLayoutDirection(YogaDirection direction) {
        layout.setDirection(direction);
    }

    public void calculateLayout(float width, float height) {
        calculateLayout(width, height, style.getDirection());
    }

    public void calculateLayout(float width, float height, YogaDirection direction) {
        freeze(null);

        ArrayList<YogaNode> n = new ArrayList<>();
        n.add(this);
        for (int i = 0; i < n.size(); ++i) {
            final YogaNode parent = n.get(i);
            List<YogaNode> children = parent.children;
            if (children != null) {
                for (YogaNode child : children) {
                    child.freeze(parent);
                }
            }
        }

        CalculateLayout.calculateLayout(
                this,
                width,
                height,
                direction
        );
    }

    private void freeze(YogaNode parent) {
        Object data = getData();
        if (data instanceof Inputs inputs) {
            inputs.freeze(this, parent);
        }
    }

    public void copyStyle(YogaNode other) {
        if (!style.equals(other.style)) {
            style = new YogaStyle(other.style);
            markDirtyAndPropagate();
        }
    }

    public YogaNode cloneWithChildren() {
        var node = new YogaNode(this);
        YogaEvent.publish(node, YogaEventType.NODE_ALLOCATION, new YogaEvent.NodeAllocationData(config));
        node.setOwner(null);
        return node;
    }

    public YogaNode cloneWithoutChildren() {
        var node = new YogaNode(this);
        YogaEvent.publish(node, YogaEventType.NODE_ALLOCATION, new YogaEvent.NodeAllocationData(config));
        node.setOwner(null);
        node.clearChildren();
        return node;
    }

    public float getAbsolutePositionX() {
        if (getPositionType() == YogaPositionType.ABSOLUTE){
            return getLayoutX();
        }
        if (owner != null) {
            return owner.getAbsolutePositionX() + getLayoutX();
        }
        return getLayoutX();
    }

    public float getAbsolutePositionY() {
        if (getPositionType() == YogaPositionType.ABSOLUTE){
            return getLayoutY();
        }
        if (owner != null) {
            return owner.getAbsolutePositionY() + getLayoutY();
        }
        return getLayoutY();
    }

    @Override
    public String toString() {
        if (debugName != null) {
            return debugName;
        } else if (context != null) {
            return "context[" + context + "]";
        }
        return super.toString();
    }

    @Override
    public void setMargin(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, StyleLength length) {
        if (!style.getMargin(edge).equals(length)) {
            style.setMargin(edge, length);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setPadding(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, StyleLength value) {
        if (!style.getPadding(edge).equals(value)) {
            style.setPadding(edge, value);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setPaddingPercent(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float percent) {
        var length = StyleLength.percent(percent);
        if (!style.getPadding(edge).equals(length)) {
            style.setPadding(edge, length);
            markDirtyAndPropagate();
        }
    }

    @Override
    public void setPosition(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, StyleLength value) {
        if (!style.getPosition(edge).equals(value)) {
            style.setPosition(edge, value);
            markDirtyAndPropagate();
        }
    }

    /**
     * The interface the {@link #getData()} object can optionally implement.
     */
    public interface Inputs {

        /**
         * Requests the data object to disable mutations of its inputs.
         */
        void freeze(final YogaNode node, final @Nullable YogaNode parent);
    }


}