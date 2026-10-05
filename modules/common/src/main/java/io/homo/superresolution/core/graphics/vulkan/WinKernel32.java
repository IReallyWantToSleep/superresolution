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

package io.homo.superresolution.core.graphics.vulkan;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Minimal JNA binding for closing Win32 NT handles.
 *
 * <p>{@code vkGetMemoryWin32HandleKHR} hands the application an NT handle that it owns. Unlike the
 * Linux opaque-FD path -- where {@code glImportMemoryFdEXT} takes ownership of the fd and closes it --
 * {@code glImportMemoryWin32HandleEXT} does <b>not</b> take ownership, so we must close the handle
 * after import. Otherwise one handle leaks per exported texture, and those are recreated on every
 * resize.
 *
 * <p>Only referenced from the Windows interop path, so {@code kernel32} is loaded lazily on Windows
 * only (the {@link #INSTANCE} initializer never runs on other platforms).
 */
interface WinKernel32 extends StdCallLibrary {
    WinKernel32 INSTANCE = Native.load("kernel32", WinKernel32.class);

    boolean CloseHandle(Pointer hObject);
}
