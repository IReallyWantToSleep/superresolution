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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.graphics.nanovg.NanoVGFont;
import io.homo.superresolution.core.graphics.nanovg.renderer.TextAlign;
import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.utils.Color;

public class MaterialSymbol {
    private final String name;
    private final String codepoint;
    private final NanoVGFont iconFont;

    protected MaterialSymbol(
            String name,
            String codepoint,
            NanoVGFont iconFont
    ) {
        this.name = name;
        this.codepoint = codepoint;
        this.iconFont = iconFont;
    }

    public String name() {
        return name;
    }

    public String codepoint() {
        return codepoint;
    }

    public void render(
            UIDrawContext drawContext,
            Color color,
            float iconSize,
            Vector2f position) {
        drawContext.text().drawAlignedText(
                iconFont,
                iconSize,
                codepoint,
                position.x,
                position.y,
                iconSize,
                iconSize,
                color,
                TextAlign.of(TextAlign.ALIGN_CENTER, TextAlign.ALIGN_MIDDLE),
                false
        );
    }
}
