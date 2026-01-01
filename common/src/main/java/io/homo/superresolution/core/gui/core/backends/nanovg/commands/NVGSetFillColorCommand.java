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
import io.homo.superresolution.core.utils.Color;

public class NVGSetFillColorCommand extends NVGDrawCommand {
    protected final Color color;

    public NVGSetFillColorCommand(NanoVGContext context, Color color) {
        super(context, DrawCommandType.SetFillColor);
        this.color = color;
    }

    public Color color() {
        return color;
    }

    @Override
    protected void execute() {
        context.fillColor(color);
    }
}