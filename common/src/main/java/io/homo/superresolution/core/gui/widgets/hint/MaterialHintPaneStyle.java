/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core.gui.widgets.hint;

import io.homo.superresolution.core.gui.core.WidgetStyle;

public class MaterialHintPaneStyle extends WidgetStyle<MaterialHintPaneStyle> {
    private float cornerRadius = 8f;
    private float paddingHorizontal = 12f;
    private float paddingVertical = 12f;
    private float textHorizontalPadding = 8f;
    private float iconSize = 18f;
    private float headerGap = 8f;
    private float headerContentGap = 6f;
    private float titleFontSize = 14f;
    private float titleFontWeight = 600;
    private float contentFontSize = 13f;
    private float contentFontWeight = 400;
    private float lineHeightRatio = 1.4f;

    public float cornerRadius() { return cornerRadius; }
    public MaterialHintPaneStyle cornerRadius(float v) { this.cornerRadius = v; return this; }

    public float paddingHorizontal() { return paddingHorizontal; }
    public MaterialHintPaneStyle paddingHorizontal(float v) { this.paddingHorizontal = v; return this; }

    public float paddingVertical() { return paddingVertical; }
    public MaterialHintPaneStyle paddingVertical(float v) { this.paddingVertical = v; return this; }

    public float textHorizontalPadding() { return textHorizontalPadding; }
    public MaterialHintPaneStyle textHorizontalPadding(float v) { this.textHorizontalPadding = v; return this; }

    public float iconSize() { return iconSize; }
    public MaterialHintPaneStyle iconSize(float v) { this.iconSize = v; return this; }

    public float headerGap() { return headerGap; }
    public MaterialHintPaneStyle headerGap(float v) { this.headerGap = v; return this; }

    public float headerContentGap() { return headerContentGap; }
    public MaterialHintPaneStyle headerContentGap(float v) { this.headerContentGap = v; return this; }

    public float titleFontSize() { return titleFontSize; }
    public MaterialHintPaneStyle titleFontSize(float v) { this.titleFontSize = v; return this; }

    public float titleFontWeight() { return titleFontWeight; }
    public MaterialHintPaneStyle titleFontWeight(float v) { this.titleFontWeight = v; return this; }

    public float contentFontSize() { return contentFontSize; }
    public MaterialHintPaneStyle contentFontSize(float v) { this.contentFontSize = v; return this; }

    public float contentFontWeight() { return contentFontWeight; }
    public MaterialHintPaneStyle contentFontWeight(float v) { this.contentFontWeight = v; return this; }

    public float lineHeightRatio() { return lineHeightRatio; }
    public MaterialHintPaneStyle lineHeightRatio(float v) { this.lineHeightRatio = v; return this; }
}
