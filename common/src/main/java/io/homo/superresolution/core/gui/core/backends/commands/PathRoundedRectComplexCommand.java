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

package io.homo.superresolution.core.gui.core.backends.commands;

import io.homo.superresolution.core.gui.core.backends.interfaces.DrawCommand;
import io.homo.superresolution.core.gui.core.backends.interfaces.DrawCommandType;

public abstract class PathRoundedRectComplexCommand extends DrawCommand {
    protected final float x;
    protected final float y;
    protected final float width;
    protected final float height;
    protected final float bottomLeftRadius;
    protected final float bottomRightRadius;
    protected final float topLeftRadius;
    protected final float topRightRadius;

    public PathRoundedRectComplexCommand(float x, float y, float width, float height, float bottomLeftRadius, float bottomRightRadius, float topLeftRadius, float topRightRadius) {
        super(DrawCommandType.PathRoundedRect);
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
}
