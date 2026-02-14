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

package io.homo.superresolution.core.graphics.impl.buffer;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

public class StructuredData implements IBufferData {
    protected final int size;
    protected final ByteBuffer container;
    private final Map<String, Entry> entries;

    protected StructuredData(ByteBuffer buffer, Map<String, Entry> entries, int size) {
        this.container = buffer;
        this.entries = entries;
        this.size = size;
    }

    public ByteBuffer container() {
        return container;
    }

    public long size() {
        return size;
    }

    public void free() {
        this.entries.clear();
        MemoryUtil.memFree(container);
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

    public StructuredData setFloat(String name, float value) {
        Entry entry = entries.get(name);
        if (entry instanceof FloatEntry) {
            ((FloatEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a float");
        }
        return this;
    }

    public StructuredData setInt(String name, int value) {
        Entry entry = entries.get(name);
        if (entry instanceof IntEntry) {
            ((IntEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not an int");
        }
        return this;
    }

    public StructuredData setBool(String name, boolean value) {
        Entry entry = entries.get(name);
        if (entry instanceof BoolEntry) {
            ((BoolEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a boolean");
        }
        return this;
    }

    public StructuredData setVec2(String name, Vector2f value) {
        return setVec2(name, value.x, value.y);
    }

    public StructuredData setVec2(String name, float x, float y) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec2Entry) {
            ((Vec2Entry) entry).setValue(x, y);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec2");
        }
        return this;
    }

    public StructuredData setVec3(String name, Vector3f value) {
        return setVec3(name, value.x, value.y, value.z);
    }

    public StructuredData setVec3(String name, float x, float y, float z) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec3Entry) {
            ((Vec3Entry) entry).setValue(x, y, z);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec3");
        }
        return this;
    }

    public StructuredData setVec4(String name, Vector4f value) {
        return setVec4(name, value.x, value.y, value.z, value.w);
    }

    public StructuredData setVec4(String name, float x, float y, float z, float w) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec4Entry) {
            ((Vec4Entry) entry).setValue(x, y, z, w);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec4");
        }
        return this;
    }

    public StructuredData setMat4(String name, Matrix4f value) {
        Entry entry = entries.get(name);
        if (entry instanceof Mat4Entry) {
            ((Mat4Entry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a mat4");
        }
        return this;
    }

    public StructuredData setUint(String name, int value) {
        Entry entry = entries.get(name);
        if (entry instanceof UintEntry) {
            ((UintEntry) entry).setValue(value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uint");
        }
        return this;
    }

    public StructuredData setFloatArray(String name, float[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof FloatArrayEntry) {
            ((FloatArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a float array");
        }
        return this;
    }

    public StructuredData setFloatArrayElement(String name, int index, float value) {
        Entry entry = entries.get(name);
        if (entry instanceof FloatArrayEntry) {
            ((FloatArrayEntry) entry).set(index, value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a float array");
        }
        return this;
    }

    public StructuredData setIntArray(String name, int[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof IntArrayEntry) {
            ((IntArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not an int array");
        }
        return this;
    }

    public StructuredData setIntArrayElement(String name, int index, int value) {
        Entry entry = entries.get(name);
        if (entry instanceof IntArrayEntry) {
            ((IntArrayEntry) entry).set(index, value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not an int array");
        }
        return this;
    }

    public StructuredData setBoolArray(String name, boolean[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof BoolArrayEntry) {
            ((BoolArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a bool array");
        }
        return this;
    }

    public StructuredData setBoolArrayElement(String name, int index, boolean value) {
        Entry entry = entries.get(name);
        if (entry instanceof BoolArrayEntry) {
            ((BoolArrayEntry) entry).set(index, value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a bool array");
        }
        return this;
    }

    public StructuredData setVec2Array(String name, Vector2f[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec2ArrayEntry) {
            ((Vec2ArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec2 array");
        }
        return this;
    }

    public StructuredData setVec2ArrayElement(String name, int index, Vector2f value) {
        return setVec2ArrayElement(name, index, value.x, value.y);
    }

    public StructuredData setVec2ArrayElement(String name, int index, float x, float y) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec2ArrayEntry) {
            ((Vec2ArrayEntry) entry).set(index, x, y);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec2 array");
        }
        return this;
    }

    public StructuredData setVec3Array(String name, Vector3f[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec3ArrayEntry) {
            ((Vec3ArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec3 array");
        }
        return this;
    }

    public StructuredData setVec3ArrayElement(String name, int index, Vector3f value) {
        return setVec3ArrayElement(name, index, value.x, value.y, value.z);
    }

    public StructuredData setVec3ArrayElement(String name, int index, float x, float y, float z) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec3ArrayEntry) {
            ((Vec3ArrayEntry) entry).set(index, x, y, z);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec3 array");
        }
        return this;
    }

    public StructuredData setVec4Array(String name, Vector4f[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec4ArrayEntry) {
            ((Vec4ArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec4 array");
        }
        return this;
    }

    public StructuredData setVec4ArrayElement(String name, int index, Vector4f value) {
        return setVec4ArrayElement(name, index, value.x, value.y, value.z, value.w);
    }

    public StructuredData setVec4ArrayElement(String name, int index, float x, float y, float z, float w) {
        Entry entry = entries.get(name);
        if (entry instanceof Vec4ArrayEntry) {
            ((Vec4ArrayEntry) entry).set(index, x, y, z, w);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a vec4 array");
        }
        return this;
    }

    public StructuredData setMat4Array(String name, Matrix4f[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof Mat4ArrayEntry) {
            ((Mat4ArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a mat4 array");
        }
        return this;
    }

    public StructuredData setMat4ArrayElement(String name, int index, Matrix4f value) {
        Entry entry = entries.get(name);
        if (entry instanceof Mat4ArrayEntry) {
            ((Mat4ArrayEntry) entry).set(index, value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a mat4 array");
        }
        return this;
    }

    public StructuredData setUintArray(String name, int[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof UintArrayEntry) {
            ((UintArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uint array");
        }
        return this;
    }

    public StructuredData setUintArrayElement(String name, int index, int value) {
        Entry entry = entries.get(name);
        if (entry instanceof UintArrayEntry) {
            ((UintArrayEntry) entry).set(index, value);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uint array");
        }
        return this;
    }

    public StructuredData setUvec2(String name, Vector2i value) {
        return setUvec2(name, value.x, value.y);
    }

    public StructuredData setUvec2(String name, int x, int y) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec2Entry) {
            ((Uvec2Entry) entry).setValue(x, y);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec2");
        }
        return this;
    }

    public StructuredData setUvec3(String name, Vector3i value) {
        return setUvec3(name, value.x, value.y, value.z);
    }

    public StructuredData setUvec3(String name, int x, int y, int z) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec3Entry) {
            ((Uvec3Entry) entry).setValue(x, y, z);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec3");
        }
        return this;
    }

    public StructuredData setUvec4(String name, Vector4i value) {
        return setUvec4(name, value.x, value.y, value.z, value.w);
    }

    public StructuredData setUvec4(String name, int x, int y, int z, int w) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec4Entry) {
            ((Uvec4Entry) entry).setValue(x, y, z, w);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec4");
        }
        return this;
    }

    public StructuredData setUvec2Array(String name, Vector2i[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec2ArrayEntry) {
            ((Uvec2ArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec2 array");
        }
        return this;
    }

    public StructuredData setUvec2ArrayElement(String name, int index, Vector2i value) {
        return setUvec2ArrayElement(name, index, value.x, value.y);
    }

    public StructuredData setUvec2ArrayElement(String name, int index, int x, int y) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec2ArrayEntry) {
            ((Uvec2ArrayEntry) entry).set(index, x, y);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec2 array");
        }
        return this;
    }

    public StructuredData setUvec3Array(String name, Vector3i[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec3ArrayEntry) {
            ((Uvec3ArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec3 array");
        }
        return this;
    }

    public StructuredData setUvec3ArrayElement(String name, int index, Vector3i value) {
        return setUvec3ArrayElement(name, index, value.x, value.y, value.z);
    }

    public StructuredData setUvec3ArrayElement(String name, int index, int x, int y, int z) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec3ArrayEntry) {
            ((Uvec3ArrayEntry) entry).set(index, x, y, z);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec3 array");
        }
        return this;
    }

    public StructuredData setUvec4Array(String name, Vector4i[] values) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec4ArrayEntry) {
            ((Uvec4ArrayEntry) entry).set(values);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec4 array");
        }
        return this;
    }

    public StructuredData setUvec4ArrayElement(String name, int index, Vector4i value) {
        return setUvec4ArrayElement(name, index, value.x, value.y, value.z, value.w);
    }

    public StructuredData setUvec4ArrayElement(String name, int index, int x, int y, int z, int w) {
        Entry entry = entries.get(name);
        if (entry instanceof Uvec4ArrayEntry) {
            ((Uvec4ArrayEntry) entry).set(index, x, y, z, w);
        } else {
            throw new IllegalArgumentException("Entry '" + name + "' is not a uvec4 array");
        }
        return this;
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

    static class FloatArrayEntry extends Entry {
        private final float[] values;

        public FloatArrayEntry(int offset, int count) {
            super(offset);
            this.values = new float[count];
        }

        public void set(float[] v) {
            if (v.length > values.length) {
                throw new IllegalArgumentException("Length mismatch");
            }
            System.arraycopy(v, 0, values, 0, v.length);
        }

        public void set(int index, float v) {
            values[index] = v;
        }

        @Override
        public void update(ByteBuffer buffer) {
            for (int i = 0; i < values.length; i++) {
                buffer.putFloat(offset + i * 16, values[i]);
            }
        }
    }

    static class IntArrayEntry extends Entry {
        private final int[] values;

        public IntArrayEntry(int offset, int count) {
            super(offset);
            this.values = new int[count];
        }

        public void set(int[] v) {
            if (v.length > values.length) {
                throw new IllegalArgumentException("Length mismatch");
            }
            System.arraycopy(v, 0, values, 0, v.length);
        }

        public void set(int index, int v) {
            values[index] = v;
        }

        @Override
        public void update(ByteBuffer buffer) {
            for (int i = 0; i < values.length; i++) {
                buffer.putInt(offset + i * 16, values[i]);
            }
        }
    }

    static class BoolArrayEntry extends Entry {
        private final boolean[] values;

        public BoolArrayEntry(int offset, int count) {
            super(offset);
            this.values = new boolean[count];
        }

        public void set(boolean[] v) {
            if (v.length > values.length) {
                throw new IllegalArgumentException("Length mismatch");
            }
            System.arraycopy(v, 0, values, 0, v.length);
        }

        public void set(int index, boolean v) {
            values[index] = v;
        }

        @Override
        public void update(ByteBuffer buffer) {
            for (int i = 0; i < values.length; i++) {
                buffer.putInt(offset + i * 16, values[i] ? 1 : 0);
            }
        }
    }

    static class Vec2ArrayEntry extends Entry {
        private final float[] values; // stored as x, y, x, y...

        public Vec2ArrayEntry(int offset, int count) {
            super(offset);
            this.values = new float[count * 2];
        }

        public void set(Vector2f[] v) {
            if (v.length > values.length / 2) {
                throw new IllegalArgumentException("Length mismatch");
            }
            for (int i = 0; i < v.length; i++) {
                set(i, v[i].x, v[i].y);
            }
        }

        public void set(int index, float x, float y) {
            values[index * 2] = x;
            values[index * 2 + 1] = y;
        }

        @Override
        public void update(ByteBuffer buffer) {
            int count = values.length / 2;
            for (int i = 0; i < count; i++) {
                buffer.putFloat(offset + i * 16, values[i * 2]);
                buffer.putFloat(offset + i * 16 + 4, values[i * 2 + 1]);
            }
        }
    }

    static class Vec3ArrayEntry extends Entry {
        private final float[] values; // stored as x, y, z...

        public Vec3ArrayEntry(int offset, int count) {
            super(offset);
            this.values = new float[count * 3];
        }

        public void set(Vector3f[] v) {
            if (v.length > values.length / 3) {
                throw new IllegalArgumentException("Length mismatch");
            }
            for (int i = 0; i < v.length; i++) {
                set(i, v[i].x, v[i].y, v[i].z);
            }
        }

        public void set(int index, float x, float y, float z) {
            values[index * 3] = x;
            values[index * 3 + 1] = y;
            values[index * 3 + 2] = z;
        }

        @Override
        public void update(ByteBuffer buffer) {
            int count = values.length / 3;
            for (int i = 0; i < count; i++) {
                buffer.putFloat(offset + i * 16, values[i * 3]);
                buffer.putFloat(offset + i * 16 + 4, values[i * 3 + 1]);
                buffer.putFloat(offset + i * 16 + 8, values[i * 3 + 2]);
            }
        }
    }

    static class Vec4ArrayEntry extends Entry {
        private final float[] values; // stored as x, y, z, w...

        public Vec4ArrayEntry(int offset, int count) {
            super(offset);
            this.values = new float[count * 4];
        }

        public void set(Vector4f[] v) {
            if (v.length > values.length / 4) {
                throw new IllegalArgumentException("Length mismatch");
            }
            for (int i = 0; i < v.length; i++) {
                set(i, v[i].x, v[i].y, v[i].z, v[i].w);
            }
        }

        public void set(int index, float x, float y, float z, float w) {
            values[index * 4] = x;
            values[index * 4 + 1] = y;
            values[index * 4 + 2] = z;
            values[index * 4 + 3] = w;
        }

        @Override
        public void update(ByteBuffer buffer) {
            int count = values.length / 4;
            for (int i = 0; i < count; i++) {
                buffer.putFloat(offset + i * 16, values[i * 4]);
                buffer.putFloat(offset + i * 16 + 4, values[i * 4 + 1]);
                buffer.putFloat(offset + i * 16 + 8, values[i * 4 + 2]);
                buffer.putFloat(offset + i * 16 + 12, values[i * 4 + 3]);
            }
        }
    }

    static class Mat4ArrayEntry extends Entry {
        private final Matrix4f[] values;

        public Mat4ArrayEntry(int offset, int count) {
            super(offset);
            this.values = new Matrix4f[count];
            for (int i = 0; i < count; i++) {
                values[i] = new Matrix4f();
            }
        }

        public void set(Matrix4f[] v) {
            if (v.length > values.length) {
                throw new IllegalArgumentException("Length mismatch");
            }
            for (int i = 0; i < v.length; i++) {
                values[i].set(v[i]);
            }
        }

        public void set(int index, Matrix4f v) {
            values[index].set(v);
        }

        @Override
        public void update(ByteBuffer buffer) {
            for (int i = 0; i < values.length; i++) {
                values[i].get(offset + i * 64, buffer);
            }
        }
    }

    static class UintArrayEntry extends Entry {
        private final int[] values;

        public UintArrayEntry(int offset, int count) {
            super(offset);
            this.values = new int[count];
        }

        public void set(int[] v) {
            if (v.length > values.length) {
                throw new IllegalArgumentException("Length mismatch");
            }
            System.arraycopy(v, 0, values, 0, v.length);
        }

        public void set(int index, int v) {
            values[index] = v;
        }

        @Override
        public void update(ByteBuffer buffer) {
            for (int i = 0; i < values.length; i++) {
                buffer.putInt(offset + i * 16, values[i]);
            }
        }
    }

    static class Uvec2Entry extends Entry {
        private int x, y;

        public Uvec2Entry(int offset) {
            super(offset);
        }

        public void setValue(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putInt(offset, x);
            buffer.putInt(offset + 4, y);
        }
    }

    static class Uvec3Entry extends Entry {
        private int x, y, z;

        public Uvec3Entry(int offset) {
            super(offset);
        }

        public void setValue(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putInt(offset, x);
            buffer.putInt(offset + 4, y);
            buffer.putInt(offset + 8, z);
        }
    }

    static class Uvec4Entry extends Entry {
        private int x, y, z, w;

        public Uvec4Entry(int offset) {
            super(offset);
        }

        public void setValue(int x, int y, int z, int w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        @Override
        public void update(ByteBuffer buffer) {
            buffer.putInt(offset, x);
            buffer.putInt(offset + 4, y);
            buffer.putInt(offset + 8, z);
            buffer.putInt(offset + 12, w);
        }
    }

    static class Uvec2ArrayEntry extends Entry {
        private final int[] values; // stored as x, y, x, y...

        public Uvec2ArrayEntry(int offset, int count) {
            super(offset);
            this.values = new int[count * 2];
        }

        public void set(Vector2i[] v) {
            if (v.length > values.length / 2) {
                throw new IllegalArgumentException("Length mismatch");
            }
            for (int i = 0; i < v.length; i++) {
                set(i, v[i].x, v[i].y);
            }
        }

        public void set(int index, int x, int y) {
            values[index * 2] = x;
            values[index * 2 + 1] = y;
        }

        @Override
        public void update(ByteBuffer buffer) {
            int count = values.length / 2;
            for (int i = 0; i < count; i++) {
                buffer.putInt(offset + i * 16, values[i * 2]);
                buffer.putInt(offset + i * 16 + 4, values[i * 2 + 1]);
            }
        }
    }

    static class Uvec3ArrayEntry extends Entry {
        private final int[] values; // stored as x, y, z...

        public Uvec3ArrayEntry(int offset, int count) {
            super(offset);
            this.values = new int[count * 3];
        }

        public void set(Vector3i[] v) {
            if (v.length > values.length / 3) {
                throw new IllegalArgumentException("Length mismatch");
            }
            for (int i = 0; i < v.length; i++) {
                set(i, v[i].x, v[i].y, v[i].z);
            }
        }

        public void set(int index, int x, int y, int z) {
            values[index * 3] = x;
            values[index * 3 + 1] = y;
            values[index * 3 + 2] = z;
        }

        @Override
        public void update(ByteBuffer buffer) {
            int count = values.length / 3;
            for (int i = 0; i < count; i++) {
                buffer.putInt(offset + i * 16, values[i * 3]);
                buffer.putInt(offset + i * 16 + 4, values[i * 3 + 1]);
                buffer.putInt(offset + i * 16 + 8, values[i * 3 + 2]);
            }
        }
    }

    static class Uvec4ArrayEntry extends Entry {
        private final int[] values; // stored as x, y, z, w...

        public Uvec4ArrayEntry(int offset, int count) {
            super(offset);
            this.values = new int[count * 4];
        }

        public void set(Vector4i[] v) {
            if (v.length > values.length / 4) {
                throw new IllegalArgumentException("Length mismatch");
            }
            for (int i = 0; i < v.length; i++) {
                set(i, v[i].x, v[i].y, v[i].z, v[i].w);
            }
        }

        public void set(int index, int x, int y, int z, int w) {
            values[index * 4] = x;
            values[index * 4 + 1] = y;
            values[index * 4 + 2] = z;
            values[index * 4 + 3] = w;
        }

        @Override
        public void update(ByteBuffer buffer) {
            int count = values.length / 4;
            for (int i = 0; i < count; i++) {
                buffer.putInt(offset + i * 16, values[i * 4]);
                buffer.putInt(offset + i * 16 + 4, values[i * 4 + 1]);
                buffer.putInt(offset + i * 16 + 8, values[i * 4 + 2]);
                buffer.putInt(offset + i * 16 + 12, values[i * 4 + 3]);
            }
        }
    }
}