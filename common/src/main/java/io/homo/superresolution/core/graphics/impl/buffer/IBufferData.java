package io.homo.superresolution.core.graphics.impl.buffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;

public interface IBufferData {
    /**
     * 数据源
     */
    Buffer container();

    /**
     * 获取数据源大小
     */
    int size();

    /**
     * 获取数据源指针
     */
    default long containerPtr() {
        return MemoryUtil.memAddress(container());
    }
}
