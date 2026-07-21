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

import org.lwjgl.glfw.GLFW;

public record KeyInput(
        KeyCode code,

        int scancode,

        int modifiers
) {
    public static KeyInput fromRaw(int keyCode, int scancode, int modifiers) {
        return new KeyInput(KeyCode.fromCode(keyCode), scancode, modifiers);
    }

    public boolean controlDown() {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    public boolean shiftDown() {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    public boolean altDown() {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

    public boolean superDown() {
        return (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
    }
}
