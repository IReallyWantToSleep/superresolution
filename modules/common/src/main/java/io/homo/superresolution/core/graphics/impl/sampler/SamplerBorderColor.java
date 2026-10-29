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

package io.homo.superresolution.core.graphics.impl.sampler;

import static org.lwjgl.vulkan.VK10.*;

public enum SamplerBorderColor {
    TransparentBlack,
    OpaqueBlack,
    OpaqueWhite;

    public int vk() {
        return switch (this) {
            case TransparentBlack -> VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK;
            case OpaqueBlack -> VK_BORDER_COLOR_FLOAT_OPAQUE_BLACK;
            case OpaqueWhite -> VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE;
        };
    }

    public float[] glRGBA() {
        return switch (this) {
            case TransparentBlack -> new float[]{0f, 0f, 0f, 0f};
            case OpaqueBlack -> new float[]{0f, 0f, 0f, 1f};
            case OpaqueWhite -> new float[]{1f, 1f, 1f, 1f};
        };
    }
}
