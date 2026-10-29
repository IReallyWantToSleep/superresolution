/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;

public interface YogaProps {

    /* Width properties */

    default void setWidthPercent(float percent) {
        setWidth(StyleSizeLength.percent(percent));
    }

    default void setWidthAuto() {
        setWidth(StyleSizeLength.ofAuto());
    }

    default void setWidthMaxContent() {
        setWidth(StyleSizeLength.ofMaxContent());
    }

    default void setWidthFitContent() {
        setWidth(StyleSizeLength.ofFitContent());
    }

    default void setWidthStretch() {
        setWidth(StyleSizeLength.ofStretch());
    }

    default void setMinWidthPercent(float percent) {
        setMinWidth(StyleSizeLength.percent(percent));
    }

    default void setMinWidthMaxContent() {
        setMinWidth(StyleSizeLength.ofMaxContent());
    }

    default void setMinWidthFitContent() {
        setMinWidth(StyleSizeLength.ofFitContent());
    }

    default void setMinWidthStretch() {
        setMinWidth(StyleSizeLength.ofStretch());
    }

    default void setMaxWidthPercent(float percent) {
        setMaxWidth(StyleSizeLength.percent(percent));
    }

    default void setMaxWidthMaxContent() {
        setMaxWidth(StyleSizeLength.ofMaxContent());
    }

    default void setMaxWidthFitContent() {
        setMaxWidth(StyleSizeLength.ofFitContent());
    }

    default void setMaxWidthStretch() {
        setMaxWidth(StyleSizeLength.ofStretch());
    }

    default void setHeightPercent(float percent) {
        setHeight(StyleSizeLength.percent(percent));
    }

    default void setHeightAuto() {
        setHeight(StyleSizeLength.ofAuto());
    }

    default void setHeightMaxContent() {
        setHeight(StyleSizeLength.ofMaxContent());
    }

    default void setHeightFitContent() {
        setHeight(StyleSizeLength.ofFitContent());
    }

    default void setHeightStretch() {
        setHeight(StyleSizeLength.ofStretch());
    }

    default void setMinHeightPercent(float percent) {
        setMinHeight(StyleSizeLength.percent(percent));
    }

    /* Height properties */

    default void setMinHeightMaxContent() {
        setMinHeight(StyleSizeLength.ofMaxContent());
    }

    default void setMinHeightFitContent() {
        setMinHeight(StyleSizeLength.ofFitContent());
    }

    default void setMinHeightStretch() {
        setMinHeight(StyleSizeLength.ofStretch());
    }

    default void setMaxHeightPercent(float percent) {
        setMaxHeight(StyleSizeLength.percent(percent));
    }

    default void setMaxHeightMaxContent() {
        setMaxHeight(StyleSizeLength.ofMaxContent());
    }

    default void setMaxHeightFitContent() {
        setMaxHeight(StyleSizeLength.ofFitContent());
    }

    default void setMaxHeightStretch() {
        setMaxHeight(StyleSizeLength.ofStretch());
    }

    void setMargin(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, StyleLength length);

    default void setMargin(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float margin) {
        setMargin(edge, StyleLength.points(margin));
    }

    default void setMarginPercent(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float percent) {
        setMargin(edge, StyleLength.percent(percent));
    }

    default void setMarginAuto(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge) {
        setMargin(edge, StyleLength.ofAuto());
    }

    void setPadding(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, StyleLength length);

    default void setPadding(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float padding) {
        setPadding(edge, StyleLength.points(padding));
    }

    default void setPaddingPercent(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float percent) {
        setPadding(edge, StyleLength.percent(percent));
    }

    void setPosition(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, StyleLength length);

    default void setPosition(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float position) {
        setPosition(edge, StyleLength.points(position));
    }

    default void setPositionPercent(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float percent) {
        setPosition(edge, StyleLength.percent(percent));
    }

    default void setPositionAuto(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge) {
        setPosition(edge, StyleLength.ofAuto());
    }

    void setFlex(float flex);

    /* Margin properties */

    void setFlexBasisAuto();

    void setFlexBasisPercent(float percent);

    void setFlexBasisMaxContent();

    void setFlexBasisFitContent();

    /* Padding properties */

    void setFlexBasisStretch();

    void setDirection(YogaDirection direction);

    void setBorder(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge, float value);

    /* Position properties */

    void setWrap(YogaWrap wrap);

    void setIsReferenceBaseline(boolean isReferenceBaseline);

    void setMeasureFunction(YogaMeasureFunction measureFunction);

    void setBaselineFunction(YogaBaselineFunction yogaBaselineFunction);

    YogaValue getWidth();

    /* Alignment properties */

    void setWidth(StyleSizeLength length);

    default void setWidth(float width) {
        setWidth(StyleSizeLength.points(width));
    }

    YogaValue getMinWidth();

    /* Flex properties */

    void setMinWidth(StyleSizeLength length);

    default void setMinWidth(float minWidth) {
        setMinWidth(StyleSizeLength.points(minWidth));
    }

    YogaValue getMaxWidth();

    void setMaxWidth(StyleSizeLength length);

    default void setMaxWidth(float maxWidth) {
        setMaxWidth(StyleSizeLength.points(maxWidth));
    }

    YogaValue getHeight();

    void setHeight(StyleSizeLength length);

    default void setHeight(float height) {
        setHeight(StyleSizeLength.points(height));
    }

    YogaValue getMinHeight();

    void setMinHeight(StyleSizeLength length);

    /* Other properties */

    default void setMinHeight(float minHeight) {
        setMinHeight(StyleSizeLength.points(minHeight));
    }

    YogaValue getMaxHeight();

    void setMaxHeight(StyleSizeLength length);

    default void setMaxHeight(float maxHeight) {
        setMaxHeight(StyleSizeLength.points(maxHeight));
    }

    YogaDirection getStyleDirection();

    YogaFlexDirection getFlexDirection();

    void setFlexDirection(YogaFlexDirection direction);

    YogaJustify getJustifyContent();

    void setJustifyContent(YogaJustify justifyContent);

    /* Getters */

    io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign getAlignItems();

    void setAlignItems(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign alignItems);

    io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign getAlignSelf();

    void setAlignSelf(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign alignSelf);

    YogaAlign getAlignContent();

    void setAlignContent(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign alignContent);

    YogaPositionType getPositionType();

    void setPositionType(YogaPositionType positionType);

    float getFlexGrow();

    void setFlexGrow(float flexGrow);

    float getFlexShrink();

    void setFlexShrink(float flexShrink);

    YogaValue getFlexBasis();

    void setFlexBasis(float flexBasis);

    float getAspectRatio();

    void setAspectRatio(float aspectRatio);

    YogaValue getMargin(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge);

    YogaValue getPadding(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge);

    YogaValue getPosition(io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge edge);

    float getBorder(YogaEdge edge);

    YogaValue getGap(YogaGutter gutter);

    void setGap(YogaGutter gutter, StyleLength value);

    default void setGap(YogaGutter gutter, float value) {
        setGap(gutter, StyleLength.points(value));
    }

    default void setGapPercent(YogaGutter gutter, float percent) {
        setGap(gutter, StyleLength.percent(percent));
    }

    YogaBoxSizing getBoxSizing();

    void setBoxSizing(YogaBoxSizing boxSizing);
}
