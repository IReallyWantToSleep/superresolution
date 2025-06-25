package io.homo.superresolution.core.graphics.impl.vertex;

import io.homo.superresolution.core.graphics.impl.GpuObject;

public interface IVertexBuffer extends GpuObject {
    int getSize();

    boolean isDynamic();

    void updateData(float[] data, int offset, int length);

    void destroy();
}
