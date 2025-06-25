package io.homo.superresolution.core.graphics.impl.vertex;

import io.homo.superresolution.core.graphics.impl.GpuObject;

public interface IVertexArray extends GpuObject {
    void destroy();

    void setAttributes(VertexAttribute[] attributes, IVertexBuffer vertexBuffer);
}
