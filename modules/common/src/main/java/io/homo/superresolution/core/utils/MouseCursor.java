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

package io.homo.superresolution.core.utils;

#if MC_VER >= MC_26_3
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLMouse;
#else
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import org.lwjgl.glfw.GLFW;
#endif

public class MouseCursor {
#if MC_VER >= MC_26_3
    public static final MouseCursor HAND = new MouseCursor(SDLMouse.SDL_SYSTEM_CURSOR_POINTER);
    public static final MouseCursor CROSSHAIR = new MouseCursor(SDLMouse.SDL_SYSTEM_CURSOR_CROSSHAIR);
    public static final MouseCursor IBEAM = new MouseCursor(SDLMouse.SDL_SYSTEM_CURSOR_TEXT);
    public static final MouseCursor NOT_ALLOWED = new MouseCursor(SDLMouse.SDL_SYSTEM_CURSOR_NOT_ALLOWED);
    public static final MouseCursor ARROW = new MouseCursor(SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT);
#else
    public static final MouseCursor HAND = new MouseCursor(GLFW.GLFW_HAND_CURSOR);
    public static final MouseCursor CROSSHAIR = new MouseCursor(GLFW.GLFW_CROSSHAIR_CURSOR);
    public static final MouseCursor IBEAM = new MouseCursor(GLFW.GLFW_IBEAM_CURSOR);
    public static final MouseCursor NOT_ALLOWED = new MouseCursor(GLFW.GLFW_NOT_ALLOWED_CURSOR);
    public static final MouseCursor ARROW = new MouseCursor(GLFW.GLFW_ARROW_CURSOR);
#endif

    public final int id;
#if MC_VER < MC_26_3
    private long glfwCursor = -1;
#endif

    private MouseCursor(int id) {
        this.id = id;
    }

    public void use() {
#if MC_VER >= MC_26_3
        Minecraft.getInstance().getWindow().selectCursor(cursorType());
#else
        if (glfwCursor == -1 || glfwCursor == 0) {
            glfwCursor = GLFW.glfwCreateStandardCursor(id);
        }
        if (glfwCursor != 0) {
            GLFW.glfwSetCursor(
                    MinecraftWindow.getWindowHandle(),
                    glfwCursor
            );
        }
#endif
    }

#if MC_VER >= MC_26_3
    private CursorType cursorType() {
        return switch (id) {
            case SDLMouse.SDL_SYSTEM_CURSOR_POINTER -> CursorTypes.POINTING_HAND;
            case SDLMouse.SDL_SYSTEM_CURSOR_CROSSHAIR -> CursorTypes.CROSSHAIR;
            case SDLMouse.SDL_SYSTEM_CURSOR_TEXT -> CursorTypes.IBEAM;
            case SDLMouse.SDL_SYSTEM_CURSOR_NOT_ALLOWED -> CursorTypes.NOT_ALLOWED;
            default -> CursorTypes.ARROW;
        };
    }
#endif
}
