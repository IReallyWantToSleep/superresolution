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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.core.gui.core.backends.interfaces.IFont;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextMetrics;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.nanovg.NVGtextRow;
import io.homo.superresolution.thirdparty.nanovg.NanoVGColor;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;


public class NanoVGTextRenderer extends NanoVGRendererBase {
    public static NanoVGTextRenderer INSTANCE;

    public NanoVGTextRenderer(NanoVGContextWrapper context) {
        INSTANCE = this;
    }

    public float measureTextWidth(IFont font, String text, float fontSize, float lineHeight, float weight) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        contextPtr.fontFace(((NanoVGFont) font).name);
        contextPtr.fontSize(fontSize);
        contextPtr.textLineHeight(lineHeight);
        contextPtr.fontSetVariationAxis(((NanoVGFont) font).id, "wght", weight);
        float[] bounds = contextPtr.textBounds(0, 0, text).bounds;
        return (bounds[2] - bounds[0]);
    }

    public float measureTextHeight(IFont font, String text, float fontSize, float lineHeight, float weight) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        contextPtr.fontFace(((NanoVGFont) font).name);
        contextPtr.fontSize(fontSize);
        contextPtr.textLineHeight(lineHeight);
        contextPtr.fontSetVariationAxis(((NanoVGFont) font).id, "wght", weight);
        float[] bounds = contextPtr.textBounds(0, 0, text).bounds;
        return (bounds[3] - bounds[1]) - 2;
    }

    public Vector2f measureText(IFont font, String text, float fontSize, float lineHeight, float weight) {
        if (text == null || text.isEmpty()) {
            return new Vector2f(0);
        }
        contextPtr.fontFace(((NanoVGFont) font).name);
        contextPtr.fontSize(fontSize);
        contextPtr.textLineHeight(lineHeight);
        contextPtr.fontSetVariationAxis(((NanoVGFont) font).id, "wght", weight);
        float[] bounds = contextPtr.textBounds(0, 0, text).bounds;
        return new Vector2f(
                (bounds[2] - bounds[0]),
                (bounds[3] - bounds[1]) - 2.5f
        );
    }

    public void drawAlignedText(
            IFont font, float fontSize, String text,
            float startX, float startY, float maxWidth, float lineHeight,
            float weight, Color color, TextAlign align, boolean wrap) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (align == null) {
            align = TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP);
        }
        color = color.copy().alpha((int) (nvg.globalAlpha() * color.alpha()));
        NanoVGColor vgColor = contextPtr.colorRGBA(color.red(), color.green(), color.blue(), color.alpha());
        String fontName = ((NanoVGFont) font).name;

        contextPtr.save();
        TextMetrics metrics = calculateTextMetrics(font, fontSize, text, maxWidth, lineHeight, wrap, weight);
        contextPtr.textAlign(toNvgAlign(align.horizontal()) | toNvgAlign(align.vertical()));
        contextPtr.fontSize(fontSize);
        contextPtr.fontFace(fontName);
        contextPtr.fontSetVariationAxis(((NanoVGFont) font).id, "wght", weight);
        contextPtr.fillColor(vgColor);

        float yPos = startY + 1.5f;
        for (String line : metrics.lines) {
            contextPtr.text(startX, yPos, line);
            yPos += lineHeight;
        }
        contextPtr.restore();
    }

    public void drawAlignedText(
            IFont font, float fontSize, TextMetrics metrics,
            float startX, float startY, float maxWidth, float lineHeight,
            float weight, Color color, TextAlign align, boolean wrap) {
        if (align == null) {
            align = TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP);
        }
        color = color.copy().alpha((int) (nvg.globalAlpha() * color.alpha()));
        NanoVGColor vgColor = contextPtr.colorRGBA(color.red(), color.green(), color.blue(), color.alpha());
        String fontName = ((NanoVGFont) font).name;

        contextPtr.save();
        contextPtr.textAlign(toNvgAlign(align.horizontal()) | toNvgAlign(align.vertical()));
        contextPtr.fontSize(fontSize);
        contextPtr.fontFace(fontName);
        contextPtr.fontSetVariationAxis(((NanoVGFont) font).id, "wght", weight);
        contextPtr.fillColor(vgColor);

        float yPos = startY + 1.5f;
        for (String line : metrics.lines) {
            contextPtr.text(startX, yPos, line);
            yPos += lineHeight;
        }
        contextPtr.restore();
    }

    private int toNvgAlign(TextAlignType alignType) {
        return switch (alignType) {
            case ALIGN_LEFT -> 1;
            case ALIGN_CENTER -> 2;
            case ALIGN_RIGHT -> 4;
            case ALIGN_TOP -> 8;
            case ALIGN_MIDDLE -> 16;
            case ALIGN_BOTTOM -> 32;
        };
    }

    public TextMetrics calculateTextMetrics(IFont font, float fontSize,
                                            String text, float maxWidth,
                                            float lineHeight, boolean wrap,
                                            float weight) {
        if (text == null || text.isEmpty()) {
            return new TextMetrics(List.of(), 0, 0);
        }
        contextPtr.save();
        contextPtr.fontSize(fontSize);
        contextPtr.textLineHeight(lineHeight);
        contextPtr.fontFace(((NanoVGFont) font).name);
        contextPtr.fontSetVariationAxis(((NanoVGFont) font).id, "wght", weight);

        List<String> lines = new ArrayList<>();
        String[] paragraphs = text.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (wrap && maxWidth > 0) {
                List<NVGtextRow> rows = contextPtr.textBreakLines(paragraph, maxWidth);
                if (rows != null && !rows.isEmpty()) {
                    for (NVGtextRow row : rows) {
                        lines.add(extractRowText(row));
                    }
                } else {
                    lines.add("");
                }
            } else {
                lines.add(paragraph);
            }
        }
        float maxLineWidth = 0;
        for (String line : lines) {
            // 直接使用已设置字重的上下文测量
            float[] bounds = contextPtr.textBounds(0, 0, line).bounds;
            float width = bounds[2] - bounds[0];
            if (width > maxLineWidth) {
                maxLineWidth = width;
            }
        }
        contextPtr.restore();
        return new TextMetrics(lines, Math.max(fontSize, lineHeight), maxLineWidth);
    }

    private String extractRowText(NVGtextRow row) {
        if (row == null || row.start == null) {
            return "";
        }
        if (row.end == null) {
            return row.start;
        }
        int endLength = row.end.length();
        int startLength = row.start.length();
        int length = Math.max(startLength - endLength, 0);
        return row.start.substring(0, length);
    }

    private float calculateVerticalOffset(int align, TextMetrics metrics) {
        return switch (align) {
            case TextAlign.ALIGN_BOTTOM -> -metrics.totalHeight;
            case TextAlign.ALIGN_TOP -> 0;
            case TextAlign.ALIGN_MIDDLE -> -metrics.totalHeight * 0.5f;
            default -> 0;
        };
    }

}