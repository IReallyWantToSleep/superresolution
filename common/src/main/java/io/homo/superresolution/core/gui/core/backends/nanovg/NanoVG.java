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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.common.minecraft.MinecraftWindow;

public class NanoVG {
    public static final boolean USE_RHI = false;

    public static NanoVGRenderers RENDERER;
    public static NanoVGContextWrapper context;

    public static NanoVGContextWrapper getContext() {
        return context;
    }

    public static void init() {
        context = new NanoVGContextWrapper(1 | 2);
        RENDERER = new NanoVGRenderers();
        NanoVGFontLoader.initAndLoad();
    }

    public static float getScreenWidth() {
        return MinecraftWindow.getWindowWidth();
    }

    public static float getScreenHeight() {
        return MinecraftWindow.getWindowHeight();
    }

    public static class NanoVGRenderers {
        public NanoVGTextRenderer TEXT = new NanoVGTextRenderer(context);
    }


}
