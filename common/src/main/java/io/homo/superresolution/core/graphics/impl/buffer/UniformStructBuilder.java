package io.homo.superresolution.core.graphics.impl.buffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class UniformStructBuilder {
    private final Map<String, StructuredUniformBuffer.Entry> entries = new HashMap<>();
    private int currentOffset = 0;

    public static UniformStructBuilder start() {
        return new UniformStructBuilder();
    }

    public UniformStructBuilder floatEntry(String name) {
        addEntry(name, new StructuredUniformBuffer.FloatEntry(currentOffset));
        currentOffset += 4;
        return this;
    }

    public UniformStructBuilder intEntry(String name) {
        addEntry(name, new StructuredUniformBuffer.IntEntry(currentOffset));
        currentOffset += 4;
        return this;
    }

    public UniformStructBuilder boolEntry(String name) {
        addEntry(name, new StructuredUniformBuffer.BoolEntry(currentOffset));
        currentOffset += 4;
        return this;
    }

    public UniformStructBuilder vec2Entry(String name) {
        currentOffset = alignOffset(currentOffset, 8);
        addEntry(name, new StructuredUniformBuffer.Vec2Entry(currentOffset));
        currentOffset += 8;
        return this;
    }

    public UniformStructBuilder vec3Entry(String name) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredUniformBuffer.Vec3Entry(currentOffset));
        currentOffset += 12;
        return this;
    }

    public UniformStructBuilder vec4Entry(String name) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredUniformBuffer.Vec4Entry(currentOffset));
        currentOffset += 16;
        return this;
    }

    public UniformStructBuilder mat4Entry(String name) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredUniformBuffer.Mat4Entry(currentOffset));
        currentOffset += 64;
        return this;
    }

        public UniformStructBuilder uintEntry(String name) {
        addEntry(name, new StructuredUniformBuffer.UintEntry(currentOffset));
        currentOffset += 4;
        return this;
    }

    private void addEntry(String name, StructuredUniformBuffer.Entry entry) {
        if (entries.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate entry name: " + name);
        }
        entries.put(name, entry);
    }

    private int alignOffset(int offset, int alignment) {
        return (offset + alignment - 1) & -alignment;
    }

    public StructuredUniformBuffer build() {
        int totalSize = alignOffset(currentOffset, 16);
        ByteBuffer buffer = MemoryUtil.memAlloc(totalSize);
        for (int i = 0; i < totalSize; i++) {
            buffer.put(i, (byte) 0);
        }

        return new StructuredUniformBuffer(buffer, entries, totalSize);
    }
}
