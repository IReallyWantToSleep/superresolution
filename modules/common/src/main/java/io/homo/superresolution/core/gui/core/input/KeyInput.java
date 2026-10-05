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

package io.homo.superresolution.core.gui.core.input;

#if MC_VER >= MC_26_3
import org.lwjgl.sdl.SDLKeycode;
#else
import org.lwjgl.glfw.GLFW;
#endif

public record KeyInput(
        KeyCode code,

        int scancode,

        int modifiers
) {
    public static KeyInput fromRaw(int keyCode, int scancode, int modifiers) {
        return new KeyInput(KeyCode.fromCode(keyCode), scancode, modifiers);
    }

    public boolean controlDown() {
        #if MC_VER >= MC_26_3
        return (modifiers & SDLKeycode.SDL_KMOD_CTRL) != 0;
        #else
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        #endif
    }

    public boolean shiftDown() {
        #if MC_VER >= MC_26_3
        return (modifiers & SDLKeycode.SDL_KMOD_SHIFT) != 0;
        #else
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        #endif
    }

    public boolean altDown() {
        #if MC_VER >= MC_26_3
        return (modifiers & SDLKeycode.SDL_KMOD_ALT) != 0;
        #else
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        #endif
    }

    public boolean superDown() {
        #if MC_VER >= MC_26_3
        return (modifiers & SDLKeycode.SDL_KMOD_GUI) != 0;
        #else
        return (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
        #endif
    }
}
