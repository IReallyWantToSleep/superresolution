package io.homo.superresolution.core.graphics.impl.vertex;

public class VertexBufferDescription {
    private final int sizeInBytes;
    private final boolean dynamic;

    public VertexBufferDescription(int sizeInBytes, boolean dynamic) {
        this.sizeInBytes = sizeInBytes;
        this.dynamic = dynamic;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public boolean isDynamic() {
        return dynamic;
    }
}
