/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.d3d12;

import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;

import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_D3D12_RESOURCE_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGetError;

/**
 * OpenGL texture view over a D3D12 shared committed resource.
 */
public final class GlD3D12ImportableTexture2D extends GlTexture2D {
    private final D3D12InteropContext.Resource source;
    private int memoryObject;

    public GlD3D12ImportableTexture2D(D3D12InteropContext.Resource source) {
        super(source.textureDescription());
        this.source = source;
        configureMipmap();
        try {
            initializeTexture();
        } catch (Throwable throwable) {
            destroy();
            throw throwable;
        }
    }

    @Override
    protected void initializeTexture() {
        try (GlState ignored = new GlState(
                GlState.STATE_TEXTURE |
                        GlState.STATE_ACTIVE_TEXTURE |
                        GlState.STATE_TEXTURES)) {
            clearGlErrors();
            configureTextureParameters();
            memoryObject = glCreateMemoryObjectsEXT();
            glMemoryObjectParameteriEXT(
                    memoryObject,
                    GL_DEDICATED_MEMORY_OBJECT_EXT,
                    GL_TRUE);
            glImportMemoryWin32HandleEXT(
                    memoryObject,
                    source.allocationSize(),
                    GL_HANDLE_TYPE_D3D12_RESOURCE_EXT,
                    source.sharedHandle());

            glBindTexture(GL_TEXTURE_2D, Math.toIntExact(handle()));
            glTextureStorageMem2DEXT(
                    Math.toIntExact(handle()),
                    1,
                    source.textureDescription().getFormat().gl(),
                    source.textureDescription().getWidth(),
                    source.textureDescription().getHeight(),
                    memoryObject,
                    0);
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                throw new IllegalStateException(
                        "Could not import D3D12 resource " + source.index() +
                                " into OpenGL (error 0x" +
                                Integer.toHexString(error) + ").");
            }
            updateDebugLabel(source.textureDescription().getLabel());
        }
    }

    private static void clearGlErrors() {
        while (glGetError() != GL_NO_ERROR) {
            // Discard errors left by unrelated work so the import check below
            // reports only this operation.
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (memoryObject != 0) {
            glDeleteMemoryObjectsEXT(memoryObject);
            memoryObject = 0;
        }
    }
}
