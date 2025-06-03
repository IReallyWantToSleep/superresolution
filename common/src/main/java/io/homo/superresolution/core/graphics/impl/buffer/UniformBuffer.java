package io.homo.superresolution.core.graphics.impl.buffer;

import io.homo.superresolution.core.graphics.impl.IUniformStruct;
import io.homo.superresolution.core.graphics.impl.shader.uniform.ShaderUniformBuffer;
import io.homo.superresolution.core.impl.Vec2;
import io.homo.superresolution.core.impl.Vec3;
import io.homo.superresolution.core.impl.Vec4;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class UniformBuffer extends ShaderUniformBuffer implements IUniformStruct {
    private final ByteBuffer buffer;
    private final Map<String, Entry> entries;
    private final int size;

    private UniformBuffer(ByteBuffer buffer, Map<String, Entry> entries, int size) {
        super(size);
        this.buffer = buffer;
        this.entries = entries;
        this.size = size;
    }

    public static Builder create() {
        return new Builder();
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int getSize() {
        return size;
    }

    public UniformBuffer setFloat(String name, float value) {
        Entry entry = entries.get(name);
        if (entry instanceof FloatEntry) {
            ((FloatEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a float");
        }
        return this;
    }

    public UniformBuffer setInt(String name, int value) {
        Entry entry = entries.get(name);
        if (entry instanceof IntEntry) {
            ((IntEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not an int");
        }
        return this;
    }

    public UniformBuffer setBool(String name, boolean value) {
        Entry entry = entries.get(name);
        if (entry instanceof BoolEntry) {
            ((BoolEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a boolean");
        }
        return this;
    }

    public UniformBuffer setVec2(String name, Vec2 value) {
        return setVec2(name, value.x, value.y);
    }

    public UniformBuffer setVec2(String name, float x, float y) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec2Entry) {
            ((Vec2Entry) entry).setValue(x, y);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec2");
        }
        return this;
    }

    public UniformBuffer setVec3(String name, Vec3 value) {
        return setVec3(name, value.x, value.y, value.z);
    }

    public UniformBuffer setVec3(String name, float x, float y, float z) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec3Entry) {
            ((Vec3Entry) entry).setValue(x, y, z);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec3");
        }
        return this;
    }

    public UniformBuffer setVec4(String name, Vec4 value) {
        return setVec4(name, value.x, value.y, value.z, value.w);
    }

    public UniformBuffer setVec4(String name, float x, float y, float z, float w) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec4Entry) {
            ((Vec4Entry) entry).setValue(x, y, z, w);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec4");
        }
        return this;
    }

    public UniformBuffer setMat4(String name, Matrix4f value) {
        Entry entry = entries.get(name);
        if (entry instanceof Mat4Entry) {
            ((Mat4Entry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a mat4");
        }
        return this;
    }

    public void destroy() {
        MemoryUtil.memFree(buffer);
    }

    @Override
    public void fillBuffer() {
        for (Entry entry : entries.values()) {
            entry.update(buffer);
        }
    }

    public static class Builder {
        private final Map<String, Entry> entries = new HashMap<>();
        private int currentOffset = 0;

        public Builder floatEntry(String name) {
            addEntry(name, new FloatEntry(currentOffset));
            currentOffset += 4;
            return this;
        }

        public Builder intEntry(String name) {
            addEntry(name, new IntEntry(currentOffset));
            currentOffset += 4;
            return this;
        }

        public Builder boolEntry(String name) {
            addEntry(name, new BoolEntry(currentOffset));
            currentOffset += 4;
            return this;
        }

        public Builder vec2Entry(String name) {
            currentOffset = alignOffset(currentOffset, 8);
            addEntry(name, new Vec2Entry(currentOffset));
            currentOffset += 8;
            return this;
        }

        public Builder vec3Entry(String name) {
            currentOffset = alignOffset(currentOffset, 16);
            addEntry(name, new Vec3Entry(currentOffset));
            currentOffset += 12;
            return this;
        }

        public Builder vec4Entry(String name) {
            currentOffset = alignOffset(currentOffset, 16);
            addEntry(name, new Vec4Entry(currentOffset));
            currentOffset += 16;
            return this;
        }

        public Builder mat4Entry(String name) {
            currentOffset = alignOffset(currentOffset, 16);
            addEntry(name, new Mat4Entry(currentOffset));
            currentOffset += 64;
            return this;
        }

        private void addEntry(String name, Entry entry) {
            if (entries.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate entry name: " + name);
            }
            entries.put(name, entry);
        }

        private int alignOffset(int offset, int alignment) {
            return (offset + alignment - 1) & -alignment;
        }

        public UniformBuffer build() {
            int totalSize = alignOffset(currentOffset, 16);
            ByteBuffer buffer = MemoryUtil.memAlloc(totalSize);
            for (int i = 0; i < totalSize; i++) {
                buffer.put(i, (byte) 0);
            }

            return new UniformBuffer(buffer, entries, totalSize);
        }
    }

    private abstract static class Entry {
        protected final int offset;

        protected Entry(int offset) {
            this.offset = offset;
        }

        public abstract void update(ByteBuffer buffer);
    }

    private static class FloatEntry extends Entry {
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

    private static class IntEntry extends Entry {
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

    private static class BoolEntry extends Entry {
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

    private static class Vec2Entry extends Entry {
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

    private static class Vec3Entry extends Entry {
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

    private static class Vec4Entry extends Entry {
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

    private static class Mat4Entry extends Entry {
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
}