package io.homo.superresolution.core.graphics.impl.buffer;

import io.homo.superresolution.core.graphics.impl.GpuObject;


public interface IBuffer extends GpuObject {
    /**
     * 数据
     */
    IBufferData data();

    void upload();
}
