/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.iris_velocity_ext.v26_1.vertex_serializer;


import io.homo.superresolution.iris_velocity_ext.v26_1.IrisExtVertexFormats;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.MemoryAccess;

public class IrisEntityToTerrainVertexSerializer implements VertexSerializer {
    public IrisEntityToTerrainVertexSerializer() {
        super();
    }

    public void serialize(long src, long dst, int vertexCount) {
        for(int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            MemoryAccess.setFloat(dst, MemoryAccess.getFloat(src));
            MemoryAccess.setFloat(dst + 4L, MemoryAccess.getFloat(src + 4L));
            MemoryAccess.setFloat(dst + 8L, MemoryAccess.getFloat(src + 8L));
            MemoryAccess.setInt(dst + 12L, MemoryAccess.getInt(src + 12L));
            MemoryAccess.setFloat(dst + 16L, MemoryAccess.getFloat(src + 16L));
            MemoryAccess.setFloat(dst + 20L, MemoryAccess.getFloat(src + 20L));
            MemoryAccess.setInt(dst + 24L, MemoryAccess.getInt(src + 28L));
            MemoryAccess.setInt(dst + 28L, MemoryAccess.getInt(src + 32L));
            MemoryAccess.setInt(dst + 32L, 0);
            MemoryAccess.setInt(dst + 36L, MemoryAccess.getInt(src + 36L));
            MemoryAccess.setInt(dst + 40L, MemoryAccess.getInt(src + 40L));
            MemoryAccess.setInt(dst + 44L, MemoryAccess.getInt(src + 44L));
            src += IrisExtVertexFormats.ENTITY_VELOCITY.getVertexSize();
            dst += IrisVertexFormats.TERRAIN.getVertexSize();
        }

    }
}
