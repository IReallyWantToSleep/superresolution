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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.utils.Color;

public class MaterialElevation {

    public static final Color DEFAULT_SHADOW_COLOR = Color.rgba(0, 0, 0, 255);

    private static final Color TRANSPARENT_COLOR = Color.rgba(0, 0, 0, 0);

    private static final int ALPHA_UMBRA = 34;
    private static final int ALPHA_PENUMBRA = 22;
    private static final int ALPHA_AMBIENT = 20;
    private static final ShadowLayer[][] LEVEL_CONFIGS = {
            // Level 0
            {},
            // Level 1
            {
                    new ShadowLayer(0, 2, 1, -1, ALPHA_UMBRA),
                    new ShadowLayer(0, 1, 1, 0, ALPHA_PENUMBRA),
                    new ShadowLayer(0, 1, 3, 0, ALPHA_AMBIENT)
            },
            // Level 2
            {
                    new ShadowLayer(0, 3, 3, -2, ALPHA_UMBRA),
                    new ShadowLayer(0, 3, 4, 0, ALPHA_PENUMBRA),
                    new ShadowLayer(0, 1, 8, 0, ALPHA_AMBIENT)
            },
            // Level 3
            {
                    new ShadowLayer(0, 3, 5, -1, ALPHA_UMBRA),
                    new ShadowLayer(0, 6, 10, 0, ALPHA_PENUMBRA),
                    new ShadowLayer(0, 1, 18, 0, ALPHA_AMBIENT)
            },
            // Level 4
            {
                    new ShadowLayer(0, 5, 5, -3, ALPHA_UMBRA),
                    new ShadowLayer(0, 8, 10, 1, ALPHA_PENUMBRA),
                    new ShadowLayer(0, 3, 14, 2, ALPHA_AMBIENT)
            },
            // Level 5
            {
                    new ShadowLayer(0, 7, 8, -4, ALPHA_UMBRA),
                    new ShadowLayer(0, 12, 17, 2, ALPHA_PENUMBRA),
                    new ShadowLayer(0, 5, 22, 4, ALPHA_AMBIENT)
            }
    };

    public static void draw(RenderContext ctx, int level, float x, float y, float width, float height, float cornerRadius) {
        draw(ctx, level, x, y, width, height, cornerRadius, DEFAULT_SHADOW_COLOR);
    }

    public static void draw(RenderContext ctx, int level, float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft) {
        draw(
                ctx,
                level,
                x,
                y,
                width,
                height,
                topLeft,
                topRight,
                bottomRight,
                bottomLeft,
                DEFAULT_SHADOW_COLOR
        );
    }

    public static void draw(RenderContext ctx, int level, float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, Color shadowBaseColor) {
        if (level <= 0 || level >= LEVEL_CONFIGS.length) {
            return;
        }

        ShadowLayer[] layers = LEVEL_CONFIGS[level];
        float avgRadius = (topLeft + topRight + bottomRight + bottomLeft) / 4.0f;

        for (ShadowLayer layer : layers) {
            drawSingleLayer(ctx, x, y, width, height, avgRadius, shadowBaseColor, layer);
        }
    }

    public static void draw(RenderContext ctx, int level, float x, float y, float width, float height, float cornerRadius, Color shadowBaseColor) {
        if (level <= 0 || level >= LEVEL_CONFIGS.length) {
            return;
        }

        ShadowLayer[] layers = LEVEL_CONFIGS[level];

        for (ShadowLayer layer : layers) {
            drawSingleLayer(ctx, x, y, width, height, cornerRadius, shadowBaseColor, layer);
        }
    }

    private static void drawSingleLayer(RenderContext ctx, float x, float y, float width, float height, float radius, Color baseColor, ShadowLayer layer) {
        Color innerColor = applyAlpha(
                baseColor,
                (int) ((float) layer.alphaValue / 255 * ctx.globalAlpha() * 255)
        );

        float spread = layer.spread;
        float gx = x + layer.offsetX - spread;
        float gy = y + layer.offsetY - spread;
        float gw = width + spread * 2;
        float gh = height + spread * 2;
        float gr = radius + spread;
        float feather = Math.max(1.0f, layer.blur);

        IPaint shadowPaint = ctx.boxGradient(
                gx, gy, gw, gh, gr, feather,
                innerColor, TRANSPARENT_COLOR
        );

        float pathPadding = feather * 2.0f;

        ctx.beginPath();

        ctx.rect(
                gx - pathPadding,
                gy - pathPadding,
                gw + pathPadding * 2,
                gh + pathPadding * 2
        );

        ctx.paint(shadowPaint);
        ctx.endPath(true);
    }

    private static Color applyAlpha(Color base, int alpha) {
        int finalAlpha = (int) (((float) base.alpha() / 255.0f) * alpha);

        return Color.rgba(base.red(), base.green(), base.blue(), finalAlpha);
    }

    private record ShadowLayer(float offsetX,

                               float offsetY,

                               float blur,

                               float spread,

                               int alphaValue) {
    }
}