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

package io.homo.superresolution.core.gui.core.backends.interfaces;

import org.joml.Vector2f;
import io.homo.superresolution.core.utils.Color;

public interface ITextRenderer {

    float measureTextWidth(String text, float fontSize);

    float measureTextHeight(String text, float fontSize);

    Vector2f measureText(String text, float fontSize);

    default void drawText(IFont font, float fontSize, String text,
                          float startX, float startY, float maxWidth,
                          float lineHeight, Color color) {
        drawAlignedText(
                font,
                fontSize,
                text,
                startX,
                startY,
                maxWidth,
                lineHeight,
                color,
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                false
        );
    }

    default void drawWrappedText(IFont font, float fontSize, String text,
                                 float startX, float startY, float maxWidth,
                                 float lineHeight, Color color) {
        drawAlignedText(
                font,
                fontSize,
                text,
                startX,
                startY,
                maxWidth,
                lineHeight,
                color,
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                true
        );
    }

    default void drawWrappedAlignedText(IFont font, float fontSize, String text,
                                        float startX, float startY, float maxWidth,
                                        float lineHeight, Color color, TextAlign align) {
        drawAlignedText(
                font,
                fontSize,
                text,
                startX,
                startY,
                maxWidth,
                lineHeight,
                color,
                align,
                true
        );
    }

    void drawAlignedText(
            IFont font,
            float fontSize,
            String text,
            float startX,
            float startY,
            float maxWidth,
            float lineHeight,
            Color color,
            TextAlign align,
            boolean wrap);
}
