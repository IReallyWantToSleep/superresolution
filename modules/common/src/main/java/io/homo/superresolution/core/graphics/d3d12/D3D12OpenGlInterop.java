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

import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.opengl.EXTMemoryObjectWin32;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class D3D12OpenGlInterop {
    private D3D12OpenGlInterop() {
    }

    public static void requireExtensions() {
        if (!GL.getCapabilities().GL_EXT_memory_object_win32 ||
                !GL.getCapabilities().GL_EXT_semaphore_win32) {
            throw new UnsupportedOperationException(
                    "D3D12 interop requires GL_EXT_memory_object_win32 and GL_EXT_semaphore_win32.");
        }
    }

    public static long queryAdapterLuid() {
        requireExtensions();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer luid = stack.calloc(EXTMemoryObjectWin32.GL_LUID_SIZE_EXT)
                    .order(ByteOrder.nativeOrder());
            EXTMemoryObject.glGetUnsignedBytevEXT(
                    EXTMemoryObjectWin32.GL_DEVICE_LUID_EXT,
                    luid);
            long lowPart = Integer.toUnsignedLong(luid.getInt(0));
            long highPart = Integer.toUnsignedLong(luid.getInt(4));
            long value = lowPart | (highPart << 32);
            if (value == 0) {
                throw new IllegalStateException("OpenGL reported a null device LUID.");
            }
            return value;
        }
    }
}
