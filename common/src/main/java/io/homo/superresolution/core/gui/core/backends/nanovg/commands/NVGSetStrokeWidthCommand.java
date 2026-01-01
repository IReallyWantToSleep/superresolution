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

package io.homo.superresolution.core.gui.core.backends.nanovg.commands;


import io.homo.superresolution.core.gui.core.backends.interfaces.DrawCommandType;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGContext;

public class NVGSetStrokeWidthCommand extends NVGDrawCommand {
    protected final float width;

    public NVGSetStrokeWidthCommand(NanoVGContext context, float width) {
        super(context, DrawCommandType.SetStrokeWidth);
        this.width = width;
    }

    public float width() {
        return width;
    }

    @Override
    protected void execute() {
        context.strokeWidth(width);
    }
}