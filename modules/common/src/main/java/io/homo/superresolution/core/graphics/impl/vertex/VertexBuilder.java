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

package io.homo.superresolution.core.graphics.impl.vertex;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class VertexBuilder {
    private final IVertexBuffer vertexBuffer;
    private final VertexFormat vertexFormat;
    private final int stride;
    private final Map<String, AttributeInfo> nameToAttribute;
    private final Map<Integer, AttributeInfo> locationToAttribute;
    private ByteBuffer buffer;
    private int currentVertexOffset;
    private int currentVertexPosition;
    private int vertexCount;
    private boolean buildingVertex;
    private int dirtyStart;
    private int dirtyEnd;
    private boolean mapped;

    private VertexBuilder(IVertexBuffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
        this.vertexFormat = vertexBuffer.getVertexFormat();
        this.stride = vertexFormat.stride();

        this.nameToAttribute = new HashMap<>();
        this.locationToAttribute = new HashMap<>();

        for (VertexFormat.VertexAttribute attr : vertexFormat.attributes()) {
            AttributeInfo info = new AttributeInfo(
                    attr.location(),
                    attr.name(),
                    attr.format(),
                    attr.offset()
            );
            nameToAttribute.put(attr.name(), info);
            locationToAttribute.put(attr.location(), info);
        }

        this.buffer = null;
        this.currentVertexOffset = 0;
        this.currentVertexPosition = 0;
        this.vertexCount = 0;
        this.buildingVertex = false;
        this.dirtyStart = Integer.MAX_VALUE;
        this.dirtyEnd = 0;
        this.mapped = false;
    }

    public static VertexBuilder of(IVertexBuffer vertexBuffer) {
        return new VertexBuilder(vertexBuffer);
    }

    public VertexBuilder addVertex() {
        if (buildingVertex) {
            throw new IllegalStateException("Previous vertex not ended. Call endVertex() first.");
        }

        if (!mapped) {
            buffer = vertexBuffer.map(true);
            mapped = true;
        }

        if (currentVertexOffset + stride > buffer.capacity()) {
            throw new IllegalStateException("Buffer overflow: cannot add more vertices.");
        }

        buildingVertex = true;
        currentVertexPosition = currentVertexOffset;
        return this;
    }

    public VertexBuilder endVertex() {
        if (!buildingVertex) {
            throw new IllegalStateException("No vertex being built. Call addVertex() first.");
        }

        buildingVertex = false;

        dirtyStart = Math.min(dirtyStart, currentVertexOffset);
        dirtyEnd = Math.max(dirtyEnd, currentVertexOffset + stride);

        currentVertexOffset += stride;
        vertexCount++;
        return this;
    }

    public VertexBuilder attribute(String name, float... values) {
        if (!buildingVertex) {
            throw new IllegalStateException("Not building a vertex. Call addVertex() first.");
        }

        AttributeInfo attr = nameToAttribute.get(name);
        if (attr == null) {
            throw new IllegalArgumentException("Unknown attribute: " + name);
        }

        writeAttribute(attr, values);
        return this;
    }

    public VertexBuilder attribute(int location, float... values) {
        if (!buildingVertex) {
            throw new IllegalStateException("Not building a vertex. Call addVertex() first.");
        }

        AttributeInfo attr = locationToAttribute.get(location);
        if (attr == null) {
            throw new IllegalArgumentException("Unknown attribute location: " + location);
        }

        writeAttribute(attr, values);
        return this;
    }

    public VertexBuilder attributeInt(String name, int... values) {
        if (!buildingVertex) {
            throw new IllegalStateException("Not building a vertex. Call addVertex() first.");
        }

        AttributeInfo attr = nameToAttribute.get(name);
        if (attr == null) {
            throw new IllegalArgumentException("Unknown attribute: " + name);
        }

        writeAttributeInt(attr, values);
        return this;
    }

    public VertexBuilder attributeInt(int location, int... values) {
        if (!buildingVertex) {
            throw new IllegalStateException("Not building a vertex. Call addVertex() first.");
        }

        AttributeInfo attr = locationToAttribute.get(location);
        if (attr == null) {
            throw new IllegalArgumentException("Unknown attribute location: " + location);
        }

        writeAttributeInt(attr, values);
        return this;
    }

    public VertexBuilder attributeByte(String name, byte... values) {
        if (!buildingVertex) {
            throw new IllegalStateException("Not building a vertex. Call addVertex() first.");
        }

        AttributeInfo attr = nameToAttribute.get(name);
        if (attr == null) {
            throw new IllegalArgumentException("Unknown attribute: " + name);
        }

        writeAttributeByte(attr, values);
        return this;
    }

    public VertexBuilder attributeByte(int location, byte... values) {
        if (!buildingVertex) {
            throw new IllegalStateException("Not building a vertex. Call addVertex() first.");
        }

        AttributeInfo attr = locationToAttribute.get(location);
        if (attr == null) {
            throw new IllegalArgumentException("Unknown attribute location: " + location);
        }

        writeAttributeByte(attr, values);
        return this;
    }

    public void upload() {
        if (buildingVertex) {
            throw new IllegalStateException("Vertex not ended. Call endVertex() first.");
        }

        if (mapped) {
            vertexBuffer.unmap();
            buffer = null;
            mapped = false;
        }

        currentVertexOffset = 0;
        vertexCount = 0;
        dirtyStart = Integer.MAX_VALUE;
        dirtyEnd = 0;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public VertexBuilder reset() {
        currentVertexOffset = 0;
        currentVertexPosition = 0;
        vertexCount = 0;
        buildingVertex = false;
        dirtyStart = Integer.MAX_VALUE;
        dirtyEnd = 0;
        return this;
    }

    public boolean isMapped() {
        return mapped;
    }

    private void writeAttribute(AttributeInfo attr, float[] values) {
        int componentCount = attr.format.getComponentCount();
        if (values.length != componentCount) {
            throw new IllegalArgumentException(
                    String.format("Expected %d components for attribute '%s', got %d",
                            componentCount, attr.name, values.length)
            );
        }

        int position = currentVertexPosition + attr.offset;

        switch (attr.format) {
            case FLOAT:
            case FLOAT2:
            case FLOAT3:
            case FLOAT4:
                for (float value : values) {
                    buffer.putFloat(position, value);
                    position += 4;
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        "Format " + attr.format + " not supported for float values"
                );
        }
    }

    private void writeAttributeInt(AttributeInfo attr, int[] values) {
        int componentCount = attr.format.getComponentCount();
        if (values.length != componentCount) {
            throw new IllegalArgumentException(
                    String.format("Expected %d components for attribute '%s', got %d",
                            componentCount, attr.name, values.length)
            );
        }

        int position = currentVertexPosition + attr.offset;

        switch (attr.format) {
            case INT:
            case INT2:
            case INT3:
            case INT4:
            case UINT:
            case UINT2:
            case UINT3:
            case UINT4:
                for (int value : values) {
                    buffer.putInt(position, value);
                    position += 4;
                }
                break;
            case SHORT2:
            case SHORT4:
            case USHORT2:
            case USHORT4:
                for (int value : values) {
                    buffer.putShort(position, (short) value);
                    position += 2;
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        "Format " + attr.format + " not supported for int values"
                );
        }
    }

    private void writeAttributeByte(AttributeInfo attr, byte[] values) {
        int componentCount = attr.format.getComponentCount();
        if (values.length != componentCount) {
            throw new IllegalArgumentException(
                    String.format("Expected %d components for attribute '%s', got %d",
                            componentCount, attr.name, values.length)
            );
        }

        int position = currentVertexPosition + attr.offset;

        switch (attr.format) {
            case BYTE4_NORMALIZED:
            case UBYTE4_NORMALIZED:
                for (byte value : values) {
                    buffer.put(position, value);
                    position += 1;
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        "Format " + attr.format + " not supported for byte values"
                );
        }
    }

    private static class AttributeInfo {
        final int location;
        final String name;
        final VertexAttributeFormat format;
        final int offset;

        AttributeInfo(int location, String name, VertexAttributeFormat format, int offset) {
            this.location = location;
            this.name = name;
            this.format = format;
            this.offset = offset;
        }
    }
}
