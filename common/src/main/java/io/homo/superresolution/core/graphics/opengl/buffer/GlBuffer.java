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

package io.homo.superresolution.core.graphics.opengl.buffer;

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.buffer.IBufferData;
import io.homo.superresolution.core.graphics.opengl.Gl;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL45.*;

public class GlBuffer implements IBuffer {
    private final int glId;
    private final long size;
    private final BufferUsage usage;

    private IBufferData bufferData;
    private ByteBuffer mappedBuffer;
    private boolean mapped;

    public GlBuffer(BufferDescription description) {
        this.size = description.size();
        this.usage = description.usage();
        this.glId = Gl.DSA.createBuffer();
        int target = getGlTarget();
        int previous = GL15.glGetInteger(getGlBindingQuery(target));
        GL15.glBindBuffer(target, glId);
        GL15.glBufferData(target, this.size, getGlUsage());
        GL15.glBindBuffer(target, previous);
    }

    @Override
    public IBufferData data() {
        return bufferData;
    }

    @Override
    public void upload() {
        if (bufferData == null) {
            throw new RuntimeException();
        }
        writeNow(bufferData.asByteBuffer(), 0);
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public BufferUsage getUsage() {
        return usage;
    }

    @Override
    public ByteBuffer map(int offsetInBytes, int lengthInBytes, boolean write) {
        validateRange(offsetInBytes, lengthInBytes);
        if (mapped) {
            throw new IllegalStateException("Buffer is already mapped");
        }

        int target = getGlTarget();
        int previous = GL15.glGetInteger(getGlBindingQuery(target));
        GL15.glBindBuffer(target, glId);
        mappedBuffer = GL30.glMapBufferRange(
                target,
                offsetInBytes,
                lengthInBytes,
                write ? GL30.GL_MAP_WRITE_BIT : GL30.GL_MAP_READ_BIT
        );
        GL15.glBindBuffer(target, previous);

        if (mappedBuffer == null) {
            throw new RuntimeException("Failed to map buffer");
        }

        mapped = true;
        return mappedBuffer;
    }

    @Override
    public void unmap() {
        if (!mapped) {
            throw new IllegalStateException("Buffer is not mapped");
        }

        int target = getGlTarget();
        int previous = GL15.glGetInteger(getGlBindingQuery(target));
        GL15.glBindBuffer(target, glId);
        boolean success = GL15.glUnmapBuffer(target);
        GL15.glBindBuffer(target, previous);
        mappedBuffer = null;
        mapped = false;

        if (!success) {
            throw new RuntimeException("Failed to unmap buffer");
        }
    }

    @Override
    public void setBufferData(IBufferData bufferData) {
        this.bufferData = bufferData;
    }

    @Override
    public long handle() {
        return glId;
    }

    private int getGlTarget() {
        return switch (usage) {
            case Ubo -> GL_UNIFORM_BUFFER;
            case CopySrc -> GL_COPY_READ_BUFFER;
            case CopyDst -> GL_COPY_WRITE_BUFFER;
            default -> GL_ARRAY_BUFFER;
        };
    }

    private int getGlUsage() {
        return switch (usage) {
            case StaticDraw -> GL_STATIC_DRAW;
            case DynamicDraw -> GL_DYNAMIC_DRAW;
            case Ubo -> GL_DYNAMIC_DRAW;
            default -> GL_STATIC_DRAW;
        };
    }

    private int getGlBindingQuery(int target) {
        return switch (target) {
            case GL_UNIFORM_BUFFER -> GL_UNIFORM_BUFFER_BINDING;
            case GL_COPY_READ_BUFFER -> GL_COPY_READ_BUFFER_BINDING;
            case GL_COPY_WRITE_BUFFER -> GL_COPY_WRITE_BUFFER_BINDING;
            default -> GL_ARRAY_BUFFER_BINDING;
        };
    }

    private void validateRange(int offsetInBytes, int lengthInBytes) {
        if (offsetInBytes < 0 || lengthInBytes < 0) {
            throw new IllegalArgumentException("Buffer range cannot be negative");
        }
        if ((long) offsetInBytes + lengthInBytes > size) {
            throw new IllegalArgumentException("Buffer range exceeds buffer size");
        }
    }

    private void writeNow(ByteBuffer data, int offsetInBytes) {
        ByteBuffer src = data.duplicate();
        int lengthInBytes = src.remaining();
        validateRange(offsetInBytes, lengthInBytes);
        if (mapped) {
            throw new IllegalStateException("Cannot update a mapped buffer");
        }

        int target = getGlTarget();
        int previous = GL15.glGetInteger(getGlBindingQuery(target));
        GL15.glBindBuffer(target, glId);
        GL15.glBufferSubData(target, offsetInBytes, src);
        GL15.glBindBuffer(target, previous);
    }

    @Override
    public void destroy() {
        if (mapped) {
            unmap();
        }
        Gl.DSA.deleteBuffer(glId);
    }
}
