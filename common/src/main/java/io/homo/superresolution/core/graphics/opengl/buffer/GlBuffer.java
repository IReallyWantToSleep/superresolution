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

package io.homo.superresolution.core.graphics.opengl.buffer;

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.BufferUsage;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.buffer.IBufferData;
import io.homo.superresolution.core.graphics.opengl.Gl;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL45C;

import static org.lwjgl.opengl.GL45.*;

public class GlBuffer implements IBuffer {
    private final int glId;
    private final long size;
    private final BufferUsage usage;

    private IBufferData bufferData;

    public GlBuffer(BufferDescription description) {
        this.size = description.size();
        this.usage = description.usage();
        this.glId = Gl.DSA.createBuffer();
        if (Gl.isSupportDSA()) {
            GL45C.glNamedBufferData(this.glId, new int[]{}, getGlUsage());
        } else {
            GL41.glBufferData(this.glId, new int[]{}, getGlUsage());
        }
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
        Gl.DSA.bufferData(this.glId, getGlTarget(), bufferData.container(), getGlUsage());
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

    @Override
    public void destroy() {
        Gl.DSA.deleteBuffer(glId);
    }
}
