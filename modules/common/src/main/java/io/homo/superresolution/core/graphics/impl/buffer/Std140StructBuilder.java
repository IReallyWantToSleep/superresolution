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

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Std140StructBuilder {
    private final Map<String, StructuredData.Entry> entries = new HashMap<>();
    private int currentOffset = 0;

    public static Std140StructBuilder start() {
        return new Std140StructBuilder();
    }


    public Std140StructBuilder floatEntry(String name) {
        addEntry(name, new StructuredData.FloatEntry(currentOffset));
        currentOffset += 4;
        return this;
    }

    public Std140StructBuilder intEntry(String name) {
        addEntry(name, new StructuredData.IntEntry(currentOffset));
        currentOffset += 4;
        return this;
    }

    public Std140StructBuilder boolEntry(String name) {
        addEntry(name, new StructuredData.BoolEntry(currentOffset));
        currentOffset += 4;
        return this;
    }

    public Std140StructBuilder vec2Entry(String name) {
        currentOffset = alignOffset(currentOffset, 8);
        addEntry(name, new StructuredData.Vec2Entry(currentOffset));
        currentOffset += 8;
        return this;
    }

    public Std140StructBuilder vec3Entry(String name) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Vec3Entry(currentOffset));
        currentOffset += 12;
        return this;
    }

    public Std140StructBuilder vec4Entry(String name) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Vec4Entry(currentOffset));
        currentOffset += 16;
        return this;
    }

    public Std140StructBuilder mat4Entry(String name) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Mat4Entry(currentOffset));
        currentOffset += 64;
        return this;
    }

    public Std140StructBuilder floatArray(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.FloatArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder intArray(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.IntArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder boolArray(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.BoolArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder vec2Array(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Vec2ArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder vec3Array(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Vec3ArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder vec4Array(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Vec4ArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder mat4Array(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Mat4ArrayEntry(currentOffset, count));
        currentOffset += count * 64;
        return this;
    }

    public Std140StructBuilder uintArray(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.UintArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder uintEntry(String name) {
        addEntry(name, new StructuredData.UintEntry(currentOffset));
        currentOffset += 4;
        return this;
    }

    public Std140StructBuilder uvec2Entry(String name) {
        currentOffset = alignOffset(currentOffset, 8);
        addEntry(name, new StructuredData.Uvec2Entry(currentOffset));
        currentOffset += 8;
        return this;
    }

    public Std140StructBuilder uvec3Entry(String name) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Uvec3Entry(currentOffset));
        currentOffset += 12;
        return this;
    }

    public Std140StructBuilder uvec4Entry(String name) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Uvec4Entry(currentOffset));
        currentOffset += 16;
        return this;
    }

    public Std140StructBuilder uvec2Array(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Uvec2ArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder uvec3Array(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Uvec3ArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    public Std140StructBuilder uvec4Array(String name, int count) {
        currentOffset = alignOffset(currentOffset, 16);
        addEntry(name, new StructuredData.Uvec4ArrayEntry(currentOffset, count));
        currentOffset += count * 16;
        return this;
    }

    private void addEntry(String name, StructuredData.Entry entry) {
        if (entries.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate entry name: " + name);
        }
        entries.put(name, entry);
    }

    private int alignOffset(int offset, int alignment) {
        return (offset + alignment - 1) & -alignment;
    }

    public StructuredData build() {
        int totalSize = alignOffset(currentOffset, 16);
        ByteBuffer buffer = MemoryUtil.memAlloc(totalSize);
        for (int i = 0; i < totalSize; i++) {
            buffer.put(i, (byte) 0);
        }

        return new StructuredData(buffer, entries, totalSize);
    }
}
