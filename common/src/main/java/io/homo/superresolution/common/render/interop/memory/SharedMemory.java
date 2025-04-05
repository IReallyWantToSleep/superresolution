package io.homo.superresolution.common.render.interop.memory;

import static io.homo.superresolution.common.render.gl.GlConst.GL_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class SharedMemory {
    public long vkMemory = VK_NULL_HANDLE;
    public int glMemoryObject = 0;
    public int fd = -1; // POSIX FD或Windows句柄
    public long allocationSize;
}