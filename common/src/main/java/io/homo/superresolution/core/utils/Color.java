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

import io.homo.superresolution.thirdparty.nanovg.NanoVGColor;

public class Color {
    private final int[] color;

    public Color(int[] color) {
        if (color.length == 3) {
            this.color = new int[]{
                    color[0],
                    color[1],
                    color[2],
                    255
            };
        } else {
            this.color = color;
        }
    }

    public static Color hex(String hex) {
        return new Color(ColorUtil.toArray(hex));
    }

    public static Color from(Object object) {
        if (object instanceof Color) {
            return Color.rgba(
                    ((Color) object).red(),
                    ((Color) object).green(),
                    ((Color) object).blue(),
                    ((Color) object).alpha()
            );
        }
        if (object instanceof NanoVGColor) {
            return Color.rgba(
                    ((NanoVGColor) object).r(),
                    ((NanoVGColor) object).g(),
                    ((NanoVGColor) object).b(),
                    ((NanoVGColor) object).a()
            );
        }
        return new Color(ColorUtil.toArray(object));
    }

    public static Color rgb(int[] rgb) {
        return new Color(new int[]{rgb[0], rgb[1], rgb[2], 255});
    }

    public static Color rgba(int[] rgba) {
        return new Color(new int[]{rgba[0], rgba[1], rgba[2], rgba[3]});
    }

    public static Color rgb(int r, int g, int b) {
        return new Color(new int[]{r, g, b, 255});
    }

    public static Color rgba(int r, int g, int b, int a) {
        return new Color(new int[]{r, g, b, a});
    }

    public static Color rgb(float r, float g, float b) {
        return rgb(
                (int) (r * 255),
                (int) (g * 255),
                (int) (b * 255)
        );
    }

    public static Color rgba(float r, float g, float b, float a) {
        return rgba(
                (int) (r * 255),
                (int) (g * 255),
                (int) (b * 255),
                (int) (a * 255)
        );
    }

    public static Color rgb(int rgb) {
        return rgb(
                ColorUtil.red(rgb),
                ColorUtil.green(rgb),
                ColorUtil.blue(rgb)
        );
    }

    public static Color black() {
        return rgb(0, 0, 0);
    }

    public static Color rgba(int rgba) {
        return rgba(
                ColorUtil.red(rgba),
                ColorUtil.green(rgba),
                ColorUtil.blue(rgba),
                ColorUtil.alpha(rgba)
        );
    }

    public static String toHEX(Color color) {
        return ColorUtil.toCode(color.color);
    }

    public static int toInt(Color color) {
        return ColorUtil.color(
                color.alpha(),
                color.red(),
                color.green(),
                color.blue()
        );
    }

    public static Color lerp(Color from, Color to, float t) {
        return from.copy().lerp(to, t);
    }

    public int red() {
        return color[0];
    }

    public int green() {
        return color[1];
    }

    public int blue() {
        return color[2];
    }

    public int alpha() {
        return color[3];
    }

    public Color red(int v) {
        color[0] = v;
        return this;
    }

    public Color green(int v) {
        color[1] = v;
        return this;

    }

    public Color blue(int v) {
        color[2] = v;
        return this;

    }

    public Color alpha(int v) {
        color[3] = v;
        return this;

    }

    public String hex() {
        return Color.toHEX(this);
    }

    public int integer() {
        return Color.toInt(this);
    }

    public Color copy() {
        return rgba(
                color[0],
                color[1],
                color[2],
                color[3]
        );
    }

    public Color lerp(Color other, float t) {
        t = Math.max(0, Math.min(1, t));
        color[0] = (int) (color[0] + (other.red() - color[0]) * t);
        color[1] = (int) (color[1] + (other.green() - color[1]) * t);
        color[2] = (int) (color[2] + (other.blue() - color[2]) * t);
        color[3] = (int) (color[3] + (other.alpha() - color[3]) * t);
        return this;
    }

}
