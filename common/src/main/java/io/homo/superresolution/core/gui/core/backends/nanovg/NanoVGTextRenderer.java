/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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
import io.homo.superresolution.core.gui.core.backends.interfaces.ITextRenderer;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.nanovg.NanoVGColor;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;


public class NanoVGTextRenderer extends NanoVGRendererBase implements ITextRenderer {
    public static NanoVGTextRenderer INSTANCE;

    public NanoVGTextRenderer(NanoVGContext context) {
        INSTANCE = this;
    }

    private List<String> splitText(String text, float maxWidth, float fontSize) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return lines;
        }

        int lineStart = 0;
        int textLength = text.length();
        while (lineStart < textLength) {
            int lineEnd = findLineEnd(text, lineStart, maxWidth, fontSize);
            lines.add(text.substring(lineStart, lineEnd));
            lineStart = lineEnd;
        }

        return lines;
    }

    private int findLineEnd(String text, int start, float maxWidth, float fontSize) {
        int end = start;
        float width = 0;
        char[] characters = text.toCharArray();

        while (end < characters.length) {
            width += measureTextWidth(String.valueOf(characters[end]), fontSize);
            if (width > maxWidth) {
                break;
            }
            end++;
        }

        return end;
    }

    public float measureTextWidth(String text, float fontSize) {
        if (text == null || text.isEmpty()) return 0;
        contextPtr.fontSize(fontSize);
        float[] bounds = contextPtr.textBounds(0, 0, text).bounds;
        return (bounds[2] - bounds[0]);
    }

    public float measureTextHeight(String text, float fontSize) {
        contextPtr.fontSize(fontSize);
        float[] bounds = contextPtr.textBounds(0, 0, text).bounds;
        return (bounds[3] - bounds[1]) - 2;
    }

    public Vector2f measureText(String text, float fontSize) {
        contextPtr.fontSize(fontSize);
        float[] bounds = contextPtr.textBounds(0, 0, text).bounds;
        return new Vector2f(
                (bounds[2] - bounds[0]),
                (bounds[3] - bounds[1]) - 2.5f
        );
    }

    public void drawAlignedText(
            IFont font,
            float fontSize,
            String text,
            float startX,
            float startY,
            float maxWidth,
            float lineHeight,
            Color color,
            TextAlign align,
            boolean wrap) {
        color = color.copy().alpha((int) (nvg.globalAlpha() * color.alpha()));
        NanoVGColor vgColor = contextPtr.colorRGBA(color.red(), color.green(), color.blue(), color.alpha());
        String fontName = ((NanoVGFont) font).name;
        contextPtr.save();
        TextMetrics metrics = calculateTextMetrics(fontName, fontSize, text, maxWidth, lineHeight, wrap);
        contextPtr.textAlign(toNvgAlign(align.horizontal()) | toNvgAlign(align.vertical()));
        contextPtr.fontSize(fontSize);
        contextPtr.fontFace(fontName);
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

    private float calculateHorizontalPosition(String line, float maxWidth, TextAlign align, float fontSize) {
        float lineWidth = measureTextWidth(line, fontSize);
        return switch (align.horizontal()) {
            case ALIGN_CENTER -> (maxWidth - lineWidth) / 2;
            case ALIGN_RIGHT -> maxWidth - lineWidth;
            default -> 0;
        };
    }

    public TextMetrics calculateTextMetrics(NanoVGFont fontName, float fontSize,
                                            String text, float maxWidth,
                                            float lineHeight, boolean wrap) {
        return calculateTextMetrics(
                fontName.name,
                fontSize,
                text,
                maxWidth,
                lineHeight,
                wrap
        );
    }

    public TextMetrics calculateTextMetrics(String fontName, float fontSize,
                                            String text, float maxWidth,
                                            float lineHeight, boolean wrap) {
        contextPtr.save();

        contextPtr.fontSize(fontSize);
        contextPtr.fontFace(fontName);

        List<String> lines = wrap ? splitText(text, maxWidth, fontSize) : List.of(text.split("\n"));
        float maxLineWidth = 0;
        for (String line : lines) {
            float width = measureTextWidth(line, fontSize);
            if (width > maxLineWidth) maxLineWidth = width;
        }
        contextPtr.restore();
        return new TextMetrics(lines, lineHeight, maxLineWidth);
    }

    private float calculateVerticalOffset(int align, TextMetrics metrics) {
        return switch (align) {
            case TextAlign.ALIGN_BOTTOM -> -metrics.totalHeight;
            case TextAlign.ALIGN_TOP -> 0;
            case TextAlign.ALIGN_MIDDLE -> -metrics.totalHeight * 0.5f;
            default -> 0;
        };
    }

    public static class TextMetrics {
        public final List<String> lines;
        public final float totalHeight;
        public final float maxLineWidth;

        public TextMetrics(List<String> lines, float lineHeight, float maxLineWidth) {
            this.lines = lines;
            this.totalHeight = Math.max(lines.size() * lineHeight - 4, 0);
            this.maxLineWidth = maxLineWidth;
        }
    }
}