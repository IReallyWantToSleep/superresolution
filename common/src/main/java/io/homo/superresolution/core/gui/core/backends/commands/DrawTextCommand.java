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

package io.homo.superresolution.core.gui.core.backends.commands;

import io.homo.superresolution.core.gui.core.backends.interfaces.DrawCommand;
import io.homo.superresolution.core.gui.core.backends.interfaces.DrawCommandType;
import io.homo.superresolution.core.gui.core.backends.interfaces.IFont;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.utils.Color;

public abstract class DrawTextCommand extends DrawCommand {
    protected final IFont font;
    protected final float fontSize;
    protected final String text;
    protected final float startX;
    protected final float startY;
    protected final float maxWidth;
    protected final float lineHeight;
    protected final Color color;
    protected final TextAlign align;
    protected final boolean wrap;

    public DrawTextCommand(IFont font, float fontSize, String text, float startX, float startY, float maxWidth, float lineHeight, Color color, TextAlign align, boolean wrap) {
        super(DrawCommandType.DrawText);
        this.font = font;
        this.fontSize = fontSize;
        this.text = text;
        this.startX = startX;
        this.startY = startY;
        this.maxWidth = maxWidth;
        this.lineHeight = lineHeight;
        this.color = color;
        this.align = align;
        this.wrap = wrap;
    }

    public IFont font() {
        return font;
    }

    public float fontSize() {
        return fontSize;
    }

    public String text() {
        return text;
    }

    public float startX() {
        return startX;
    }

    public float startY() {
        return startY;
    }

    public float maxWidth() {
        return maxWidth;
    }

    public float lineHeight() {
        return lineHeight;
    }

    public Color color() {
        return color;
    }

    public TextAlign align() {
        return align;
    }

    public boolean wrap() {
        return wrap;
    }
}