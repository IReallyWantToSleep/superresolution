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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.core.utils.MinecraftUtil;
import org.lwjgl.nanovg.NVGColor;

import static org.lwjgl.nanovg.NanoVGGL3.NVG_ANTIALIAS;
import static org.lwjgl.nanovg.NanoVGGL3.NVG_STENCIL_STROKES;

public class NanoVG {
    public static NanoVGRenderers RENDERER;
    public static NanoVGContext context;


    public static NanoVGContext getContext() {
        return context;
    }

    public static void init() {
        context = new NanoVGContext(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        RENDERER = new NanoVGRenderers();
        NanoVGFontLoader.initAndLoad();
    }

    public static float getScreenWidth() {
        return MinecraftUtil.getScreenSize().x;
    }

    public static float getScreenHeight() {
        return MinecraftUtil.getScreenSize().y;
    }

    public static NVGColor colorRGB(int r, int g, int b) {
        return colorRGB(r / 255f, g / 255f, b / 255f);
    }

    public static NVGColor colorRGB(float r, float g, float b) {
        NVGColor color = NVGColor.calloc();
        return org.lwjgl.nanovg.NanoVG.nvgRGBf(r, g, b, color);
    }

    public static NVGColor colorRGBA(int r, int g, int b, int a) {
        return colorRGBA(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    public static NVGColor colorRGBA(float r, float g, float b, float a) {
        NVGColor color = NVGColor.calloc();
        return org.lwjgl.nanovg.NanoVG.nvgRGBAf(r, g, b, a, color);
    }

    public static class NanoVGRenderers {
        public NanoVGTextRenderer TEXT = new NanoVGTextRenderer(context);
    }


}
