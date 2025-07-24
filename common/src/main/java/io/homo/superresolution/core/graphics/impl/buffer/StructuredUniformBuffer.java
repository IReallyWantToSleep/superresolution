package io.homo.superresolution.core.graphics.impl.buffer;

import io.homo.superresolution.core.graphics.impl.IUniformStruct;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.math.Vector3f;
import io.homo.superresolution.core.math.Vector4f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

public class StructuredUniformBuffer implements IUniformStruct, IBufferData {
    protected final int size;
    protected final ByteBuffer container;
    private final Map<String, Entry> entries;

    protected StructuredUniformBuffer(ByteBuffer buffer, Map<String, Entry> entries, int size) {
        this.container = buffer;
        this.entries = entries;
        this.size = size;
    }

    public long size() {
        return size;
    }


    @Override
    public void put(byte[] src, long offset) {
        throw new RuntimeException();
    }

    @Override
    public void updatePartial(Buffer data, long offset, long length) {
        Objects.requireNonNull(data, "Data buffer cannot be null");
        if (offset < 0 || length < 0 || offset + length > size) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        if (data.remaining() < length) {
            throw new IllegalArgumentException("Not enough data in input buffer");
        }

        MemoryUtil.memCopy(
                MemoryUtil.memAddress(data),
                MemoryUtil.memAddress(container) + offset,
                length
        );
    }

    @Override
    public void update(Buffer data) {
        Objects.requireNonNull(data, "Data buffer cannot be null");
        if (data.limit() != size) {
            throw new IllegalArgumentException("Data size must match buffer size");
        }

        MemoryUtil.memCopy(
                MemoryUtil.memAddress(data),
                MemoryUtil.memAddress(container),
                size
        );
    }

    public ByteBuffer container() {
        return container;
    }

    public StructuredUniformBuffer setFloat(String name, float value) {
        Entry entry = entries.get(name);
        if (entry instanceof FloatEntry) {
            ((FloatEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a float");
        }
        return this;
    }


    public StructuredUniformBuffer setInt(String name, int value) {
        Entry entry = entries.get(name);
        if (entry instanceof IntEntry) {
            ((IntEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not an int");
        }
        return this;
    }

    public StructuredUniformBuffer setBool(String name, boolean value) {
        Entry entry = entries.get(name);
        if (entry instanceof BoolEntry) {
            ((BoolEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a boolean");
        }
        return this;
    }

    public StructuredUniformBuffer setVec2(String name, Vector2f value) {
        return setVec2(name, value.x, value.y);
    }

    public StructuredUniformBuffer setVec2(String name, float x, float y) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec2Entry) {
            ((Vec2Entry) entry).setValue(x, y);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec2");
        }
        return this;
    }

    public StructuredUniformBuffer setVec3(String name, Vector3f value) {
        return setVec3(name, value.x, value.y, value.z);
    }

    public StructuredUniformBuffer setVec3(String name, float x, float y, float z) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec3Entry) {
            ((Vec3Entry) entry).setValue(x, y, z);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec3");
        }
        return this;
    }

    public StructuredUniformBuffer setVec4(String name, Vector4f value) {
        return setVec4(name, value.x, value.y, value.z, value.w);
    }

    public StructuredUniformBuffer setVec4(String name, float x, float y, float z, float w) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec4Entry) {
            ((Vec4Entry) entry).setValue(x, y, z, w);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec4");
        }
        return this;
    }

    public StructuredUniformBuffer setMat4(String name, Matrix4f value) {
        Entry entry = entries.get(name);
        if (entry instanceof Mat4Entry) {
            ((Mat4Entry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a mat4");
        }
        return this;
    }

    public StructuredUniformBuffer setUint(String name, int value) {
        Entry entry = entries.get(name);
        if (entry instanceof UintEntry) {
            ((UintEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uint");
        }
        return this;
    }

    public void free() {
        this.entries.clear();
        MemoryUtil.memFree(container);
    }

    public void fillBuffer() {
        for (Entry entry : entries.values()) {
            entry.update(container);
        }
    }

    abstract static class Entry {
        protected final int offset;

        protected Entry(int offset) {
            this.offset = offset;
        }

        public abstract void update(ByteBuffer buffer);
    }

    static class FloatEntry extends Entry {
        private float value;

        public FloatEntry(int offset) {
            super(offset);
        }

        public void setValue(float value) {
            this.value = value;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putFloat(offset, value);
        }
    }

    static class IntEntry extends Entry {
        private int value;

        public IntEntry(int offset) {
            super(offset);
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putInt(offset, value);
        }
    }

    static class BoolEntry extends Entry {
        private boolean value;

        public BoolEntry(int offset) {
            super(offset);
        }

        public void setValue(boolean value) {
            this.value = value;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putInt(offset, value ? 1 : 0);
        }
    }

    static class Vec2Entry extends Entry {
        private float x, y;

        public Vec2Entry(int offset) {
            super(offset);
        }

        public void setValue(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putFloat(offset, x);
            buffer.putFloat(offset + 4, y);
        }
    }

    static class Vec3Entry extends Entry {
        private float x, y, z;

        public Vec3Entry(int offset) {
            super(offset);
        }

        public void setValue(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putFloat(offset, x);
            buffer.putFloat(offset + 4, y);
            buffer.putFloat(offset + 8, z);
        }
    }

    static class Vec4Entry extends Entry {
        private float x, y, z, w;

        public Vec4Entry(int offset) {
            super(offset);
        }

        public void setValue(float x, float y, float z, float w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putFloat(offset, x);
            buffer.putFloat(offset + 4, y);
            buffer.putFloat(offset + 8, z);
            buffer.putFloat(offset + 12, w);
        }
    }

    static class Mat4Entry extends Entry {
        private final Matrix4f value = new Matrix4f();

        public Mat4Entry(int offset) {
            super(offset);
        }

        public void setValue(Matrix4f matrix) {
            value.set(matrix);
        }

        @Override
        public void update(ByteBuffer buffer) {
            value.get(offset, buffer);
        }
    }

    static class UintEntry extends Entry {
        private int value;

        public UintEntry(int offset) {
            super(offset);
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putInt(offset, value);
        }
    }
}