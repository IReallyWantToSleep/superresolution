package io.homo.superresolution.core.graphics.impl.buffer;

import io.homo.superresolution.core.graphics.impl.GpuObject;
import io.homo.superresolution.core.impl.Destroyable;


public interface IBuffer extends GpuObject, Destroyable {
    /**
     * 获取数据
     */
    IBufferData data();

    /**
     * 上传数据
     */
    void upload();

    long getSize();

    BufferUsage getUsage();

    void setBufferData(IBufferData bufferData);

}
