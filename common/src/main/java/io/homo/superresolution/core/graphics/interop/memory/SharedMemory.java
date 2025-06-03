package io.homo.superresolution.core.graphics.interop.memory;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class SharedMemory {
    public long vkMemory = VK_NULL_HANDLE;
    public int glMemoryObject = 0;
    public int fd = -1; // POSIX FD或Windows句柄
    public long allocationSize;
}