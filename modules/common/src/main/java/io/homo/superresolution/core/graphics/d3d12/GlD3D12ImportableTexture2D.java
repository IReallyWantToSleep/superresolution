/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.d3d12;

import io.homo.superresolution.core.graphics.opengl.GlState;
import io.homo.superresolution.core.graphics.opengl.texture.GlTexture2D;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;

import java.util.Objects;

import static org.lwjgl.opengl.EXTMemoryObject.*;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.GL_HANDLE_TYPE_D3D12_RESOURCE_EXT;
import static org.lwjgl.opengl.EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT;
import static org.lwjgl.opengl.GL11.*;

/**
 * OpenGL texture view over a D3D12 shared committed resource.
 */
public final class GlD3D12ImportableTexture2D extends GlTexture2D {
    private final D3D12Texture2D source;
    private final int importedTextureId;
    private D3D12Texture2D.ExternalBorrowLease sourceBorrow;
    private int memoryObject;
    private boolean textureDeleteAttempted;
    private boolean textureDestroyed;

    public GlD3D12ImportableTexture2D(D3D12Texture2D source) {
        super(requireSharedTexture(source));
        this.source = source;
        this.importedTextureId = Math.toIntExact(handle());
    }

    /**
     * Completes the fallible import after the caller has retained this owner.
     */
    public synchronized void initializeImport() {
        source.device().withLifecycleLock(this::initializeImportLocked);
    }

    private void initializeImportLocked() {
        source.device().assertLifecycleLockHeld();
        if (textureDestroyed) {
            throw new IllegalStateException(
                    "The OpenGL/D3D12 imported texture is destroyed");
        }
        if (sourceBorrow != null || memoryObject != 0) {
            throw new IllegalStateException(
                    "The OpenGL/D3D12 imported texture is already initialized");
        }
        sourceBorrow = source.borrowExternal();
        configureMipmap();
        initializeTextureLocked();
    }

    private static TextureDescription requireSharedTexture(D3D12Texture2D source) {
        Objects.requireNonNull(source, "source");
        if (!source.isShared()) {
            throw new IllegalArgumentException(
                    "OpenGL can only import a shared D3D12 Texture2D");
        }
        return source.getTextureDescription();
    }

    private static void clearGlErrors() {
        while (glGetError() != GL_NO_ERROR) {
            // Discard errors left by unrelated work so the import check below
            // reports only this operation.
        }
    }

    @Override
    protected synchronized void initializeTexture() {
        source.device().withLifecycleLock(this::initializeTextureLocked);
    }

    private void initializeTextureLocked() {
        source.device().assertLifecycleLockHeld();
        if (sourceBorrow == null) {
            throw new IllegalStateException(
                    "The D3D12 Texture2D import borrow is closed");
        }
        try (
                GlState ignored = new GlState(
                        GlState.STATE_TEXTURE |
                                GlState.STATE_ACTIVE_TEXTURE |
                                GlState.STATE_TEXTURES)
        ) {
            clearGlErrors();
            configureTextureParameters();
            memoryObject = glCreateMemoryObjectsEXT();
            glMemoryObjectParameteriEXT(
                    memoryObject,
                    GL_DEDICATED_MEMORY_OBJECT_EXT,
                    GL_TRUE);
            glImportMemoryWin32HandleEXT(
                    memoryObject,
                    0L,
                    GL_HANDLE_TYPE_D3D12_RESOURCE_EXT,
                    source.sharedHandleLocked());

            glBindTexture(GL_TEXTURE_2D, Math.toIntExact(handle()));
            glTextureStorageMem2DEXT(
                    Math.toIntExact(handle()),
                    1,
                    source.getTextureFormat().gl(),
                    source.getWidth(),
                    source.getHeight(),
                    memoryObject,
                    0);
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                throw new IllegalStateException(
                        "Could not import D3D12 resource " + source.string() +
                                " into OpenGL (error 0x" +
                                Integer.toHexString(error) + ").");
            }
            updateDebugLabel(source.getTextureDescription().getLabel());
        }
    }

    private static void checkGlError(String operation) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            throw new IllegalStateException(
                    operation + " failed with OpenGL error 0x" +
                            Integer.toHexString(error) + ".");
        }
    }

    @Override
    public synchronized void destroy() {
        source.device().withLifecycleLock(this::destroyLocked);
    }

    private void destroyLocked() {
        source.device().assertLifecycleLockHeld();
        if (!textureDestroyed) {
            clearGlErrors();
            if (!textureDeleteAttempted) {
                super.destroy();
                textureDeleteAttempted = true;
            } else {
                glDeleteTextures(importedTextureId);
            }
            checkGlError("Delete the OpenGL/D3D12 imported texture");
            textureDestroyed = true;
        }
        if (memoryObject != 0) {
            clearGlErrors();
            glDeleteMemoryObjectsEXT(memoryObject);
            checkGlError("Delete the OpenGL/D3D12 imported memory object");
            memoryObject = 0;
        }
        if (sourceBorrow != null) {
            sourceBorrow.close();
            sourceBorrow = null;
        }
    }
}
