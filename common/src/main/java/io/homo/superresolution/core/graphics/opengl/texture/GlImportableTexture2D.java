/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.graphics.opengl.texture;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.graphics.opengl.GlDebug;
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.vulkan.VulkanInterop;
import io.homo.superresolution.core.graphics.vulkan.texture.VulkanTexture;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL11.*;

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
                    "OpenGL-Vulkan interop texture: MemoryHandle {} MemorySize {}bits Size {}x{}px PixelSize {} Levels {} Format {}",
                    handle,
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
