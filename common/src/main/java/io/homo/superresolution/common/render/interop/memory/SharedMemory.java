package io.homo.superresolution.common.render.interop.memory;

import static io.homo.superresolution.common.render.gl.GlConst.GL_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class SharedMemory {
    public long vkHandle = VK_NULL_HANDLE;
    public long allocationSize = 0;
    public long vkRef = VK_NULL_HANDLE;
    public int glRef = GL_NULL_HANDLE;
    public long glAttachedTexture = GL_NULL_HANDLE;
}
