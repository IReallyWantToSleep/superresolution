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

package io.homo.superresolution.iris_velocity_ext.v26_1;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.irisshaders.iris.vertices.IrisVertexFormats;

public final class IrisExtVertexFormats {
    public static final VertexFormatElement VELOCITY_ELEMENT;
    public static final VertexFormat ENTITY_VELOCITY;

    static {
        VELOCITY_ELEMENT = VertexFormatElement.register(nextElementId(), 0, VertexFormatElement.Type.FLOAT, false, 3);
        ENTITY_VELOCITY = VertexFormat.builder()
                .add("Position", VertexFormatElement.POSITION)
                .add("Color", VertexFormatElement.COLOR)
                .add("UV0", VertexFormatElement.UV0)
                .add("UV1", VertexFormatElement.UV1)
                .add("UV2", VertexFormatElement.UV2)
                .add("Normal", VertexFormatElement.NORMAL)
                .padding(1)
                .add("iris_Entity", IrisVertexFormats.ENTITY_ID_ELEMENT)
                .add("mc_midTexCoord", IrisVertexFormats.MID_TEXTURE_ELEMENT)
                .add("at_tangent", IrisVertexFormats.TANGENT_ELEMENT)
                .add("irisExt_velocity", VELOCITY_ELEMENT)
                .build();
    }

    private IrisExtVertexFormats() {
    }

    public static int velocityAttributeLocation() {
        return ENTITY_VELOCITY.getElements().indexOf(VELOCITY_ELEMENT);
    }

    private static int nextElementId() {
        int id = 0;
        while (VertexFormatElement.byId(id) != null) {
            if (++id >= VertexFormatElement.MAX_COUNT) {
                throw new RuntimeException("Too many mods registering VertexFormatElements");
            }
        }
        return id;
    }
}
