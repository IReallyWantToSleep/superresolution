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

package io.homo.superresolution.iris_velocity_ext.v26_3.vertex_serializer;


import io.homo.superresolution.iris_velocity_ext.v26_3.IrisExtVertexFormats;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.lwjgl.system.MemoryUtil;

public class IrisEntityToTerrainVertexSerializer implements VertexSerializer {
    public IrisEntityToTerrainVertexSerializer() {
        super();
    }

    public void serialize(long src, long dst, int vertexCount) {
        for(int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            MemoryUtil.memPutFloat(dst, MemoryUtil.memGetFloat(src));
            MemoryUtil.memPutFloat(dst + 4L, MemoryUtil.memGetFloat(src + 4L));
            MemoryUtil.memPutFloat(dst + 8L, MemoryUtil.memGetFloat(src + 8L));
            MemoryUtil.memPutInt(dst + 12L, MemoryUtil.memGetInt(src + 12L));
            MemoryUtil.memPutFloat(dst + 16L, MemoryUtil.memGetFloat(src + 16L));
            MemoryUtil.memPutFloat(dst + 20L, MemoryUtil.memGetFloat(src + 20L));
            MemoryUtil.memPutInt(dst + 24L, MemoryUtil.memGetInt(src + 28L));
            MemoryUtil.memPutInt(dst + 28L, MemoryUtil.memGetInt(src + 32L));
            MemoryUtil.memPutInt(dst + 32L, 0);
            MemoryUtil.memPutInt(dst + 36L, MemoryUtil.memGetInt(src + 36L));
            MemoryUtil.memPutInt(dst + 40L, MemoryUtil.memGetInt(src + 40L));
            MemoryUtil.memPutInt(dst + 44L, MemoryUtil.memGetInt(src + 44L));
            src += IrisExtVertexFormats.ENTITY_VELOCITY.getVertexSize();
            dst += IrisVertexFormats.TERRAIN.getVertexSize();
        }

    }
}
