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

package io.homo.superresolution.core.graphics.impl.texture;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.impl.Destroyable;

public interface ITexture extends Destroyable, GpuObject {
    TextureFormat getTextureFormat();

    TextureUsages getTextureUsages();

    TextureType getTextureType();

    TextureFilterMode getTextureFilterMode();

    TextureWrapMode getTextureWrapMode();

    TextureMipmapSettings getMipmapSettings();

    TextureDescription getTextureDescription();


    int getWidth();

    int getHeight();

    default String string() {
        return getTextureDescription().getLabel() != null ? getTextureDescription().getLabel() : "ITexture{" +
                "id=" + handle() +
                "format=" + getTextureFormat() +
                "width=" + getWidth() +
                "height=" + getHeight() +
                '}';
    }
}
