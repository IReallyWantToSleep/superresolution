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

package io.homo.superresolution.core.graphics.opengl.vertex;

import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.opengl.Gl;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class GlVertexBuffer implements IVertexBuffer {
    private final int id;
    private final int size;
    private final boolean dynamic;
    private final ByteBuffer buffer;

    private GlVertexBuffer(int id, int size, boolean dynamic, ByteBuffer buffer) {
        this.id = id;
        this.size = size;
        this.dynamic = dynamic;
        this.buffer = buffer;
    }

    public static GlVertexBuffer create(VertexBufferDescription description) {
        int bufferId = Gl.DSA.createBuffer();
        int usage = description.isDynamic() ? GL15.GL_DYNAMIC_DRAW : GL15.GL_STATIC_DRAW;
        ByteBuffer buffer = MemoryUtil.memAlloc(description.getSizeInBytes());
        Gl.DSA.bufferData(bufferId, GL15.GL_ARRAY_BUFFER, buffer, usage);
        return new GlVertexBuffer(bufferId, description.getSizeInBytes(), description.isDynamic(), buffer);
    }


    @Override
    public long handle() {
        return id;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public void updateData(float[] data, int offset, int length) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(length);
        buffer.put(data, offset, length);
        buffer.flip();
        Gl.DSA.bufferSubData(this.id, 0, buffer);
    }

    @Override
    public void destroy() {
        Gl.DSA.deleteBuffer(id);
        MemoryUtil.memFree(buffer);
    }
}