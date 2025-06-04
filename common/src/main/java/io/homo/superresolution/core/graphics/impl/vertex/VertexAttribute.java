package io.homo.superresolution.core.graphics.impl.vertex;

public class VertexAttribute {
    private final int location;
    private final int componentCount;
    private final DataType dataType;
    private final int stride;
    private final int offset;

    public VertexAttribute(int location, int componentCount, DataType dataType, int stride, int offset) {
        this.location = location;
        this.componentCount = componentCount;
        this.dataType = dataType;
        this.stride = stride;
        this.offset = offset;
    }

    public int getLocation() {
        return location;
    }

    public int getComponentCount() {
        return componentCount;
    }

    public DataType getDataType() {
        return dataType;
    }

    public int getStride() {
        return stride;
    }

    public int getOffset() {
        return offset;
    }

    public enum DataType {
        FLOAT,    // 浮点型
        INTEGER   // 整型
    }
}