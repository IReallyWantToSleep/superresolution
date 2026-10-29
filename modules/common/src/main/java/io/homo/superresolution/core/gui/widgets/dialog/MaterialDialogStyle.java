/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.gui.widgets.dialog;

import io.homo.superresolution.core.gui.core.WidgetStyle;
import io.homo.superresolution.core.utils.Color;

public class MaterialDialogStyle extends WidgetStyle<MaterialDialogStyle> {
    private float cornerRadius = 28f;
    private float minWidth = 280f;
    private float maxWidth = 560f;
    private float padding = 24f;
    private float iconSize = 24f;
    private float headlineFontSize = 24f;
    private float supportingTextFontSize = 14f;
    private float buttonFontSize = 14f;
    private float buttonSpacing = 8f;
    private float sectionSpacing = 16f;
    private float dividerHeight = 1f;
    private boolean scrimDismiss = true;
    private Color scrimColor = Color.rgba(0, 0, 0, 80);

    public float cornerRadius() {
        return cornerRadius;
    }

    public MaterialDialogStyle cornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    public float minWidth() {
        return minWidth;
    }

    public MaterialDialogStyle minWidth(float minWidth) {
        this.minWidth = minWidth;
        return this;
    }

    public float maxWidth() {
        return maxWidth;
    }

    public MaterialDialogStyle maxWidth(float maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public float padding() {
        return padding;
    }

    public MaterialDialogStyle padding(float padding) {
        this.padding = padding;
        return this;
    }

    public float iconSize() {
        return iconSize;
    }

    public MaterialDialogStyle iconSize(float iconSize) {
        this.iconSize = iconSize;
        return this;
    }

    public float headlineFontSize() {
        return headlineFontSize;
    }

    public MaterialDialogStyle headlineFontSize(float headlineFontSize) {
        this.headlineFontSize = headlineFontSize;
        return this;
    }

    public float supportingTextFontSize() {
        return supportingTextFontSize;
    }

    public MaterialDialogStyle supportingTextFontSize(float supportingTextFontSize) {
        this.supportingTextFontSize = supportingTextFontSize;
        return this;
    }

    public float buttonFontSize() {
        return buttonFontSize;
    }

    public MaterialDialogStyle buttonFontSize(float buttonFontSize) {
        this.buttonFontSize = buttonFontSize;
        return this;
    }

    public float buttonSpacing() {
        return buttonSpacing;
    }

    public MaterialDialogStyle buttonSpacing(float buttonSpacing) {
        this.buttonSpacing = buttonSpacing;
        return this;
    }

    public float sectionSpacing() {
        return sectionSpacing;
    }

    public MaterialDialogStyle sectionSpacing(float sectionSpacing) {
        this.sectionSpacing = sectionSpacing;
        return this;
    }

    public float dividerHeight() {
        return dividerHeight;
    }

    public MaterialDialogStyle dividerHeight(float dividerHeight) {
        this.dividerHeight = dividerHeight;
        return this;
    }

    public boolean scrimDismiss() {
        return scrimDismiss;
    }

    public MaterialDialogStyle scrimDismiss(boolean scrimDismiss) {
        this.scrimDismiss = scrimDismiss;
        return this;
    }

    public Color scrimColor() {
        return scrimColor;
    }

    public MaterialDialogStyle scrimColor(Color scrimColor) {
        this.scrimColor = scrimColor;
        return this;
    }
}
