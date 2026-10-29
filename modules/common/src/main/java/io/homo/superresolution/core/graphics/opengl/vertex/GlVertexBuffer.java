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

package io.homo.superresolution.core.graphics.opengl.vertex;

import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;
import io.homo.superresolution.core.graphics.impl.vertex.VertexFormat;
import io.homo.superresolution.core.graphics.opengl.Gl;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

public class GlVertexBuffer implements IVertexBuffer {
    private final int id;
    private final int size;
    private final boolean dynamic;
    private final VertexFormat vertexFormat;
    private final GlVertexArray vao;
    private ByteBuffer mappedBuffer;
    private boolean isMapped;

    private GlVertexBuffer(int id, int size, boolean dynamic, VertexFormat vertexFormat) {
        this.id = id;
        this.size = size;
        this.dynamic = dynamic;
        this.vertexFormat = vertexFormat;
        this.vao = new GlVertexArray();
        this.vao.setFormat(this);
        this.mappedBuffer = null;
        this.isMapped = false;
    }

    public static GlVertexBuffer create(VertexBufferDescription description) {
        int bufferId = Gl.DSA.createBuffer();
        int usage = description.isDynamic() ? GL15.GL_DYNAMIC_DRAW : GL15.GL_STATIC_DRAW;
        Gl.DSA.bufferData(bufferId, GL15.GL_ARRAY_BUFFER, description.getSizeInBytes(), usage);
        return new GlVertexBuffer(bufferId, description.getSizeInBytes(), description.isDynamic(),
                description.getVertexFormat());
    }

    public GlVertexArray getVao() {
        return vao;
    }

    @Override
    public long handle() {
        return id;
    }

    @Override
    public int getSizeInBytes() {
        return size;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public VertexFormat getVertexFormat() {
        return vertexFormat;
    }

    @Override
    public ByteBuffer map(int offsetInBytes, int lengthInBytes, boolean write) {
        if (isMapped) {
            throw new IllegalStateException("Buffer is already mapped");
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
        int access = write ? GL15.GL_WRITE_ONLY : GL15.GL_READ_ONLY;
        mappedBuffer = org.lwjgl.opengl.GL30.glMapBufferRange(
                GL15.GL_ARRAY_BUFFER,
                offsetInBytes,
                lengthInBytes,
                write ? org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT : org.lwjgl.opengl.GL30.GL_MAP_READ_BIT
        );
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (mappedBuffer == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        isMapped = true;
        return mappedBuffer;
    }

    @Override
    public void unmap() {
        if (!isMapped) {
            throw new IllegalStateException("Buffer is not mapped");
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, id);
        boolean success = GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        if (!success) {
            throw new RuntimeException("Failed to unmap buffer");
        }

        mappedBuffer = null;
        isMapped = false;
    }

    @Override
    public void updateData(ByteBuffer data, int offsetInBytes) {
        Gl.DSA.bufferSubData(this.id, offsetInBytes, data);
    }

    @Override
    public void updateData(byte[] data, int offsetInBytes, int lengthInBytes) {
        ByteBuffer tempBuffer = BufferUtils.createByteBuffer(lengthInBytes);
        tempBuffer.put(data, offsetInBytes, lengthInBytes);
        tempBuffer.flip();
        Gl.DSA.bufferSubData(this.id, offsetInBytes, tempBuffer);
    }

    @Override
    public void destroy() {
        if (isMapped) {
            unmap();
        }
        vao.destroy();
        Gl.DSA.deleteBuffer(id);
    }
}