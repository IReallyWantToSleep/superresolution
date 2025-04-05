package io.homo.superresolution.common.render.interop.texture;

import io.homo.superresolution.common.render.interop.memory.SharedMemory;
import io.homo.superresolution.common.render.vulkan.*;
import io.homo.superresolution.common.render.impl.texture.TextureFormat;
import io.homo.superresolution.common.render.vulkan.texture.TextureUsage;
import io.homo.superresolution.common.render.vulkan.texture.VkAllocatedImage;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;
import static io.homo.superresolution.common.render.vulkan.Utils.VK_CHECK;
import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.vkGetMemoryWin32HandleKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;

public class SharedTexture {
    public SharedTexture(int width, int height, VkDeviceManager deviceManager) {
    }
}