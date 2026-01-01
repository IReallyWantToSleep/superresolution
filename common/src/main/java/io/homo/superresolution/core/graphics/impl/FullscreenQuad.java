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

package io.homo.superresolution.core.graphics.impl;

import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.vertex.*;

public class FullscreenQuad {
    public static IVertexBuffer create(IDevice device) {
        VertexBufferDescription desc = new VertexBufferDescription(
                ((
                        2 * Float.BYTES // per vec2
                ) * 2 // per vertex
                ) * 4,
                false,
                getVertexFormat()
        );
        IVertexBuffer vertices = device.createVertexBuffer(desc);
        VertexBuilder.of(vertices)
                .addVertex().attribute(0, -1f, 1f).attribute(1, 0f, 1f).endVertex()
                .addVertex().attribute(0, 1f, 1f).attribute(1, 1f, 1f).endVertex()
                .addVertex().attribute(0, -1f, -1f).attribute(1, 0f, 0f).endVertex()
                .addVertex().attribute(0, 1f, -1f).attribute(1, 1f, 0f).endVertex()
                .upload();
        return vertices;
    }

    public static VertexFormat getVertexFormat() {
        return VertexFormat.builder()
                .addAttribute(0, VertexAttributeFormat.FLOAT2)
                .addAttribute(1, VertexAttributeFormat.FLOAT2)
                .build();
    }
}
