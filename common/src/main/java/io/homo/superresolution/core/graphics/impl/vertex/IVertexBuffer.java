package io.homo.superresolution.core.graphics.impl.vertex;

public interface IVertexBuffer {
    int getId();

    int getSize();

    boolean isDynamic();

    void updateData(float[] data, int offset, int length);

    void destroy();
}
