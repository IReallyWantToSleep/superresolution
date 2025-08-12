package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.vulkan.VulkanInterop;
import io.homo.superresolution.core.graphics.vulkan.texture.VulkanTexture;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;

public class GlImportableTexture2D extends GlTexture2D {

    private int glMemoryObject = 0;
    private final VulkanTexture sourceTexture;

    public GlImportableTexture2D(VulkanTexture sourceTexture) {
        super(sourceTexture.getTextureDescription());
        this.sourceTexture = sourceTexture;
        initializeTexture();
    }

    @Override
    protected void initializeTexture() {
        try (GlState ignored = new GlState(
                GlState.STATE_TEXTURE | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES);
             MemoryStack stack = MemoryStack.stackPush()) {
            configureTextureParameters();

            long handle = sourceTexture.getExportedMemoryHandle();
            long size = sourceTexture.getMemorySize();

            SuperResolution.LOGGER.info(
                    "memSize {} Size {}x{} SizePerPixel {} Levels {} Format {}",
                    size,
                    sourceTexture.getWidth(),
                    sourceTexture.getHeight(),
                    sourceTexture.getTextureFormat().getBytesPerPixel(),
                    sourceTexture.getMipmapSettings().getLevels(),
                    sourceTexture.getTextureFormat()
            );

            int[] memoryObjects = new int[1];
            glCreateMemoryObjectsEXT(memoryObjects);
            glMemoryObject = memoryObjects[0];

            VulkanInterop.IMPL.glImportMemoryEXT(glMemoryObject, size, handle);

            glBindTexture(GL_TEXTURE_2D, (int) this.handle());
            glTextureStorageMem2DEXT((int) this.handle(),
                    sourceTexture.getMipmapSettings().getLevels(),
                    sourceTexture.getTextureFormat().gl(),
                    sourceTexture.getWidth(),
                    sourceTexture.getHeight(),
                    glMemoryObject,
                    0);
        }
    }

    @Override
    public void resize(int width, int height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() {
        super.destroy();
        glDeleteMemoryObjectsEXT(glMemoryObject);
    }
}
