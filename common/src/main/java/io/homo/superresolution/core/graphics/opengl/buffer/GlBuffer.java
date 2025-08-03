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
            case UBO -> GL_UNIFORM_BUFFER;
            case COPY_SRC -> GL_COPY_READ_BUFFER;
            case COPY_DST -> GL_COPY_WRITE_BUFFER;
            default -> GL_ARRAY_BUFFER;
        };
    }

    private int getGlUsage() {
        return switch (usage) {
            case STATIC_DRAW -> GL_STATIC_DRAW;
            case DYNAMIC_DRAW -> GL_DYNAMIC_DRAW;
            case UBO -> GL_DYNAMIC_DRAW;
            default -> GL_STATIC_DRAW;
        };
    }

    @Override
    public void destroy() {
        Gl.DSA.deleteBuffer(glId);
    }
}
