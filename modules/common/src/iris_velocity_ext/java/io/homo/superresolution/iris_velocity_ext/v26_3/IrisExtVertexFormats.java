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

package io.homo.superresolution.iris_velocity_ext.v26_3;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.homo.superresolution.iris_velocity_ext.v26_3.vertex_serializer.IrisEntityToTerrainVertexSerializer;
import io.homo.superresolution.iris_velocity_ext.v26_3.vertex_serializer.ModelToEntityVertexSerializer;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import net.irisshaders.iris.vertices.IrisVertexFormats;

public final class IrisExtVertexFormats {
    public static final String VELOCITY_ATTRIBUTE = "at_velocity";
    public static final VertexFormat ENTITY_VELOCITY;

    static {
        ENTITY_VELOCITY = VertexFormat.builder(0)
                .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
                .addAttribute(DefaultVertexFormat.COLOR_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM)
                .addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
                .addAttribute(DefaultVertexFormat.UV1_SEMANTIC_NAME, GpuFormat.RG16_SINT)
                .addAttribute(DefaultVertexFormat.UV2_SEMANTIC_NAME, GpuFormat.RG16_SINT)
                .addAttribute(DefaultVertexFormat.NORMAL_SEMANTIC_NAME, GpuFormat.RGBA8_SNORM)
                .addAttribute(IrisVertexFormats.ENTITY_ID_ATTRIBUTE, GpuFormat.RGBA16_UINT)
                .addAttribute(IrisVertexFormats.MID_TEXTURE_ATTRIBUTE, GpuFormat.RG32_FLOAT)
                .addAttribute(IrisVertexFormats.TANGENT_ATTRIBUTE, GpuFormat.RGBA8_SNORM)
                .addAttribute(VELOCITY_ATTRIBUTE, GpuFormat.RGB32_FLOAT)
                .build();

        VertexSerializerRegistry.instance().registerSerializer(ENTITY_VELOCITY, IrisVertexFormats.TERRAIN, new IrisEntityToTerrainVertexSerializer());
        VertexSerializerRegistry.instance().registerSerializer(DefaultVertexFormat.ENTITY, ENTITY_VELOCITY, new ModelToEntityVertexSerializer());
    }

    private IrisExtVertexFormats() {
    }

    public static int velocityAttributeLocation() {
        for (int i = 0; i < ENTITY_VELOCITY.getElements().size(); i++) {
            if (VELOCITY_ATTRIBUTE.equals(ENTITY_VELOCITY.getElements().get(i).name())) {
                return i;
            }
        }
        throw new IllegalStateException("Velocity attribute is missing from the entity vertex format");
    }

    public static int velocityOffset() {
        return ENTITY_VELOCITY.getElement(VELOCITY_ATTRIBUTE).offset();
    }
}
