/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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
import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.vulkan.VulkanInterop;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.GL11.*;

public class GlImportableTexture2D extends GlTexture2D {

    private final VulkanTexture sourceTexture;
    private int glMemoryObject = 0;
    private long exportedMemoryHandle = -1;

    public GlImportableTexture2D(VulkanTexture sourceTexture) {
        super(sourceTexture.getTextureDescription());
        this.sourceTexture = sourceTexture;
        initializeTexture();
    }

    @Override
    protected void initializeTexture() {
        try (
                GlState ignored = new GlState(
                        GlState.STATE_TEXTURE | GlState.STATE_ACTIVE_TEXTURE | GlState.STATE_TEXTURES);
                MemoryStack stack = MemoryStack.stackPush()
        ) {
            configureTextureParameters();

            long handle = sourceTexture.takeExportedMemoryHandle();
            exportedMemoryHandle = handle;
            long size = sourceTexture.getMemorySize();
            boolean imported = false;

            try {
                SuperResolution.LOGGER.debug(
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

                // The backing Vulkan allocation is dedicated (VkMemoryDedicatedAllocateInfo). Mark the GL
                // memory object dedicated BEFORE importing so GL sizes/maps the texture to fit the dedicated
                // allocation exactly, instead of a generic huge-page-rounded block that overruns it
                // (NVRM "vaHi <= pMemBlock->end" / dmaAllocMapping_GM107).
                glMemoryObjectParameterivEXT(glMemoryObject, GL_DEDICATED_MEMORY_OBJECT_EXT, new int[]{GL_TRUE});

                VulkanInterop.IMPL.glImportMemoryEXT(glMemoryObject, size, handle);
                imported = true;

                glBindTexture(GL_TEXTURE_2D, (int) this.handle());
                glTextureStorageMem2DEXT((int) this.handle(),
                        sourceTexture.getMipmapSettings().getLevels(),
                        sourceTexture.getTextureFormat().gl(),
                        sourceTexture.getWidth(),
                        sourceTexture.getHeight(),
                        glMemoryObject,
                        0);
            } catch (Throwable throwable) {
                destroyMemoryObject();
                releaseExportedMemoryHandle(imported);
                super.destroy();
                throw throwable;
            }
        }
    }

    @Override
    public void destroy() {
        try {
            super.destroy();
        } finally {
            try {
                destroyMemoryObject();
            } finally {
                releaseExportedMemoryHandle(true);
            }
        }
    }

    private void destroyMemoryObject() {
        if (glMemoryObject != 0) {
            glDeleteMemoryObjectsEXT(glMemoryObject);
            glMemoryObject = 0;
        }
    }

    private void releaseExportedMemoryHandle(boolean imported) {
        long handle = exportedMemoryHandle;
        exportedMemoryHandle = -1;
        if (imported) {
            VulkanInterop.closeImportedExportedHandle(handle);
        } else {
            VulkanInterop.closeUnimportedExportedHandle(handle);
        }
    }

}
