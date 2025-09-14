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

package io.homo.superresolution.thirdparty.fsr2.common;

import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.math.Vector2f;

public class Fsr2ResourceCreateDescription {
    public Vector2f size;
    public TextureFormat format;
    public int dim;
    public String label;
    public int mipCount;

    public Fsr2ResourceCreateDescription(Vector2f size, TextureFormat format, int dim, String label) {
        this(size, format, dim, label, -1);
    }

    public Fsr2ResourceCreateDescription(Vector2f size, TextureFormat format, int dim, String label, int mipCount) {
        this.size = size;
        this.format = format;
        this.dim = dim;
        this.label = label;
        this.mipCount = mipCount;
    }
}
