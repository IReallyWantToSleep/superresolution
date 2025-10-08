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

import io.homo.superresolution.core.gui.core.backends.interfaces.DrawCommand;
import io.homo.superresolution.core.gui.core.backends.interfaces.DrawCommandType;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGContext;
import org.lwjgl.nanovg.NanoVG;

public class NVGPathArcCommand extends NVGDrawCommand {
    protected final float x;
    protected final float y;
    protected final float radius;
    protected final float a0;
    protected final float a1;

    public NVGPathArcCommand(NanoVGContext context, float x, float y, float radius, float a0, float a1) {
        super(context, DrawCommandType.PathArc);
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.a0 = a0;
        this.a1 = a1;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float radius() {
        return radius;
    }

    public float a0() {
        return a0;
    }

    public float a1() {
        return a1;
    }

    @Override
    protected void execute() {
        NanoVG.nvgArc(
                context.contextPtr,
                x,
                y,
                radius,
                a0,
                a1,
                NanoVG.NVG_CCW
        );
    }
}