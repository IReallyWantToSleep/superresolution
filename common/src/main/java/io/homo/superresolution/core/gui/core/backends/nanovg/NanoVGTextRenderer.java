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
import io.homo.superresolution.thirdparty.nanovg.TextBoundsResult;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class NanoVGTextRenderer extends NanoVGRendererBase {
    private static final int TEXT_BOUNDS_CACHE_CAPACITY = 256;

    private final Map<TextBoundsCacheKey, CachedTextBounds> textBoundsCache =
            new LinkedHashMap<>(TEXT_BOUNDS_CACHE_CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<TextBoundsCacheKey, CachedTextBounds> eldest) {
                    return size() > TEXT_BOUNDS_CACHE_CAPACITY;
                }
            };
    private long lastContextHandle = Long.MIN_VALUE;
    private int lastDevicePixelRatioBits = Integer.MIN_VALUE;

    public NanoVGTextRenderer(NanoVGContextWrapper context) {
    }

    public void clearTextBoundsCache() {
        textBoundsCache.clear();
        lastContextHandle = Long.MIN_VALUE;
        lastDevicePixelRatioBits = Integer.MIN_VALUE;
    }

    private CachedTextBounds measureTextBounds(IFont font, String text, float fontSize, float lineHeight, float weight) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        invalidateCacheIfContextChanged();
        contextPtr.fontFace(font.name());
        contextPtr.fontSize(fontSize);
        contextPtr.textLineHeight(lineHeight);
        contextPtr.fontSetVariationAxis(font.nativeId(), "wght", weight);

        TextBoundsCacheKey key = new TextBoundsCacheKey(
                font.nativeId(),
                font.name(),
                text,
                canonicalFloatBits(fontSize),
                canonicalFloatBits(lineHeight),
                canonicalFloatBits(weight),
                contextPtr.textMeasureStateVersion()
        );
        CachedTextBounds cached = textBoundsCache.get(key);
        if (cached != null) {
            return cached;
        }

        TextBoundsResult result = contextPtr.textBounds(0, 0, text);
        cached = CachedTextBounds.from(result);
        textBoundsCache.put(key, cached);
        return cached;
    }

    public float measureTextWidth(IFont font, String text, float fontSize, float lineHeight, float weight) {
        CachedTextBounds result = measureTextBounds(font, text, fontSize, lineHeight, weight);
        return result == null ? 0f : result.advance;
    }

    public float measureTextHeight(IFont font, String text, float fontSize, float lineHeight, float weight) {
        CachedTextBounds result = measureTextBounds(font, text, fontSize, lineHeight, weight);
        if (result == null) return 0;
        return (result.maxY - result.minY) - 2;
    }

    public Vector2f measureText(IFont font, String text, float fontSize, float lineHeight, float weight) {
        CachedTextBounds result = measureTextBounds(font, text, fontSize, lineHeight, weight);
        if (result == null) return new Vector2f(0);
        return new Vector2f(
                result.advance,
                (result.maxY - result.minY) - 2.5f
        );
    }

    private void invalidateCacheIfContextChanged() {
        long contextHandle = contextPtr.getNativeHandle();
        int devicePixelRatioBits = canonicalFloatBits(nvg.devicePixelRatio());
        if (contextHandle != lastContextHandle || devicePixelRatioBits != lastDevicePixelRatioBits) {
            textBoundsCache.clear();
            lastContextHandle = contextHandle;
            lastDevicePixelRatioBits = devicePixelRatioBits;
        }
    }

    private static int canonicalFloatBits(float value) {
        return value == 0.0f ? 0 : Float.floatToIntBits(value);
    }

    private record TextBoundsCacheKey(
            int fontId,
            String fontName,
            String text,
            int fontSizeBits,
            int lineHeightBits,
            int weightBits,
            long textMeasureStateVersion
    ) {
    }

    private record CachedTextBounds(
            float advance,
            float minX,
            float minY,
            float maxX,
            float maxY
    ) {
        private static CachedTextBounds from(TextBoundsResult result) {
            float[] bounds = result.bounds;
            return new CachedTextBounds(
                    result.advance,
                    bounds[0],
                    bounds[1],
                    bounds[2],
                    bounds[3]
            );
        }
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
        String fontName = font.name();

        contextPtr.save();
        TextMetrics metrics = calculateTextMetrics(font, fontSize, text, maxWidth, lineHeight, wrap, weight);
        contextPtr.textAlign(toNvgAlign(align.horizontal()) | toNvgAlign(align.vertical()));
        contextPtr.fontSize(fontSize);
        contextPtr.fontFace(fontName);
        contextPtr.fontSetVariationAxis(font.nativeId(), "wght", weight);
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
        String fontName = font.name();

        contextPtr.save();
        contextPtr.textAlign(toNvgAlign(align.horizontal()) | toNvgAlign(align.vertical()));
        contextPtr.fontSize(fontSize);
        contextPtr.fontFace(fontName);
        contextPtr.fontSetVariationAxis(font.nativeId(), "wght", weight);
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
        contextPtr.fontFace(font.name());
        contextPtr.fontSetVariationAxis(font.nativeId(), "wght", weight);

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
            float width = contextPtr.textBounds(0, 0, line).advance;
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

}
