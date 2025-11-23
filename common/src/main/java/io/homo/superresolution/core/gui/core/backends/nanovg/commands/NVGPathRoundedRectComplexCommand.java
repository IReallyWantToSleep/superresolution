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

package io.homo.superresolution.core.gui.core.backends.nanovg.commands;

import io.homo.superresolution.core.gui.core.backends.interfaces.DrawCommandType;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGContext;

public class NVGPathRoundedRectComplexCommand extends NVGDrawCommand {
    protected final float x;
    protected final float y;
    protected final float width;
    protected final float height;
    protected final float bottomLeftRadius;
    protected final float bottomRightRadius;
    protected final float topLeftRadius;
    protected final float topRightRadius;


    public NVGPathRoundedRectComplexCommand(NanoVGContext context, float x, float y, float width, float height, float bottomLeftRadius, float bottomRightRadius, float topLeftRadius, float topRightRadius) {
        super(context, DrawCommandType.PathRoundedRect);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bottomLeftRadius = bottomLeftRadius;
        this.bottomRightRadius = bottomRightRadius;
        this.topLeftRadius = topLeftRadius;
        this.topRightRadius = topRightRadius;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float bottomLeftRadius() {
        return bottomLeftRadius;
    }

    public float bottomRightRadius() {
        return bottomRightRadius;
    }

    public float topLeftRadius() {
        return topLeftRadius;
    }

    public float topRightRadius() {
        return topRightRadius;
    }

    @Override
    protected void execute() {
        //PR:https://github.com/memononen/nanovg/pull/189
        float tl = topLeftRadius;
        float tr = topRightRadius;
        float bl = bottomLeftRadius;
        float br = bottomRightRadius;
        float w = width;
        float h = height;
        context.contextPtr.moveTo(x + tl, y);
        context.contextPtr.lineTo(x + w - tr, y);
        context.contextPtr.bezierTo(x + w - tr * (1 - 0.5522847493f), y, x + w, y + tr * (1 - 0.5522847493f), x + w, y + tr);
        context.contextPtr.lineTo(x + w, y + h - br);
        context.contextPtr.bezierTo(x + w, y + h - br * (1 - 0.5522847493f), x + w - br * (1 - 0.5522847493f), y + h, x + w - br, y + h);
        context.contextPtr.lineTo(x + bl, y + h);
        context.contextPtr.bezierTo(x + bl * (1 - 0.5522847493f), y + h, x, y + h - bl * (1 - 0.5522847493f), x, y + h - bl);
        context.contextPtr.lineTo(x, y + tl);
        context.contextPtr.bezierTo(x, y + tl * (1 - 0.5522847493f), x + tl * (1 - 0.5522847493f), y, x + tl, y);
    }
}
