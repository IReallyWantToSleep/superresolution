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

import java.util.Arrays;

public class ColorUtil {
    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 255;
    }

    public static int green(int color) {
        return color >> 8 & 255;
    }

    public static int blue(int color) {
        return color & 255;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int color(int red, int green, int blue) {
        return color(255, red, green, blue);
    }

    public static String RGB_to_RGBA(String code) {
        String codeData = code.replace("#", "");
        if (codeData.length() == 6) {
            return "#FF" + codeData.toUpperCase();
        }
        if (codeData.length() == 8) {
            return code.toUpperCase();
        }
        throw new IllegalArgumentException("Invalid RGB color code: " + code);
    }

    public static int[] toArray(String code, String toFormat) {
        code = RGB_to_RGBA(code).replace("#", "");
        int a = Integer.parseInt(code.substring(0, 2), 16);
        int r = Integer.parseInt(code.substring(2, 4), 16);
        int g = Integer.parseInt(code.substring(4, 6), 16);
        int b = Integer.parseInt(code.substring(6, 8), 16);

        toFormat = toFormat.toLowerCase();
        if (!Arrays.asList("rgba", "argb", "rgb").contains(toFormat)) {
            throw new IllegalArgumentException(toFormat + " is not a valid format (rgba, argb, rgb)");
        }
        if (toFormat.equals("rgba")) {
            return new int[]{r, g, b, a};
        }
        if (toFormat.equals("rgb")) {
            return new int[]{r, g, b};
        }
        throw new IllegalArgumentException("Invalid format: " + toFormat);
    }


    public static int[] toArray(Object code) {
        if (code instanceof int[]) {
            return (int[]) code;
        }
        if (code instanceof String) {
            if (((String) code).startsWith("#")) {
                if (((String) code).length() == 7) {
                    return toArray((String) code, "rgb");
                } else if (((String) code).length() == 9) {
                    return toArray((String) code, "rgba");
                } else {
                    throw new IllegalArgumentException("Unsupported color code format: " + code);
                }
            } else {
                throw new IllegalArgumentException("Unsupported color code format: " + code);
            }
        }
        throw new IllegalArgumentException("Unsupported code type: " + code.getClass().getName());
    }

    public static String toCode(int[] value, boolean forceRgba) {
        int r, g, b, a;
        if (value.length == 3) {
            r = value[0];
            g = value[1];
            b = value[2];
            a = 255;
        } else if (value.length == 4) {
            r = value[0];
            g = value[1];
            b = value[2];
            a = value[3];
        } else {
            throw new IllegalArgumentException("Unexpected shape of input: " + Arrays.toString(value));
        }

        if (forceRgba || a != 255) {
            return String.format("#%02X%02X%02X%02X", a, r, g, b);  // 输出标准 #AARRGGBB
        } else {
            return String.format("#%02X%02X%02X", r, g, b);
        }
    }

    public static String toCode(int[] value) {
        return toCode(value, false);
    }

    public static Color mix(Color fore, Color post, double weight) {
        return Color.rgb(
                (int) (fore.red() * weight + post.red() * (1 - weight)),
                (int) (fore.green() * weight + post.green() * (1 - weight)),
                (int) (fore.blue() * weight + post.blue() * (1 - weight))
        );
    }

}
