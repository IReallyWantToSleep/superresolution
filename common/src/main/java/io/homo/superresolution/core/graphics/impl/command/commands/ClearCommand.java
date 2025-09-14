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

package io.homo.superresolution.core.graphics.impl.command.commands;

import io.homo.superresolution.core.graphics.impl.texture.ITexture;

public class ClearCommand extends GpuCommand {
    public int clearMode = 0; // 0-RGBA 1-Depth 2-Stencil
    public float[] colorRGBA = new float[]{0, 0, 0, 0};
    public float depth = 0;
    public int stencil = 0;

    public ITexture target;

    public ClearCommand(ITexture target) {
        this.target = target;
    }


    @Override
    public GpuCommandType getCommandType() {
        return GpuCommandType.Clear;
    }
}
