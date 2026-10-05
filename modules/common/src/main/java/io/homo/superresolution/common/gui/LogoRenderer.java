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

package io.homo.superresolution.common.gui;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

public class LogoRenderer {
    public static MaterialSymbol Logo = new MaterialSymbol(null, null, null) {
        private LogoRenderer renderer = new LogoRenderer();

        @Override
        public void render(RenderContext ctx, Color color, float iconSize, Vector2f position) {
            ctx.save();
            ctx.fillColor(color);
            float scale = iconSize / renderer.getLogoHeight();
            renderer.render(
                    ctx,
                    position.x - (renderer.getLogoWidth() * scale * 0.5f),
                    position.y - (renderer.getLogoHeight() * scale * 0.5f),
                    scale,
                    scale
            );
            ctx.restore();
        }
    };

    public float getLogoWidth() {
        return 55.11f;
    }

    public float getLogoHeight() {
        return 61.47f;
    }

    public void render(RenderContext ctx, float x, float y, float scaleX, float scaleY) {
        ctx.pushTransform();
        ctx.translate(x, y);
        ctx.scale(scaleX, scaleY);
        ctx.translate(-22.41f, -19.27f);

        ctx.beginPath();
        ctx.move(30.18f, 20.26f);
        ctx.lineTo(26.22f, 38.900000000000006f);
        ctx.bezier(26.06f, 39.68000000000001f, 26.65f, 40.410000000000004f, 27.439999999999998f, 40.410000000000004f);
        ctx.lineTo(58.339999999999996f, 40.410000000000004f);
        ctx.bezier(58.93f, 40.410000000000004f, 59.44f, 40.00000000000001f, 59.559999999999995f, 39.42f);
        ctx.lineTo(60.49999999999999f, 34.97f);
        ctx.bezier(60.65999999999999f, 34.19f, 60.06999999999999f, 33.47f, 59.279999999999994f, 33.47f);
        ctx.lineTo(36.35999999999999f, 33.47f);
        ctx.bezier(35.56999999999999f, 33.47f, 34.97999999999999f, 32.74f, 35.13999999999999f, 31.959999999999997f);
        ctx.lineTo(36.169999999999995f, 27.119999999999997f);
        ctx.bezier(36.28999999999999f, 26.54f, 36.8f, 26.13f, 37.38999999999999f, 26.13f);
        ctx.lineTo(61.379999999999995f, 26.13f);
        ctx.bezier(61.97f, 26.13f, 62.48f, 25.72f, 62.599999999999994f, 25.14f);
        ctx.lineTo(63.529999999999994f, 20.78f);
        ctx.bezier(63.68999999999999f, 20.0f, 63.099999999999994f, 19.27f, 62.309999999999995f, 19.27f);
        ctx.lineTo(31.409999999999997f, 19.27f);
        ctx.bezier(30.819999999999997f, 19.27f, 30.309999999999995f, 19.68f, 30.189999999999998f, 20.259999999999998f);

        ctx.move(55.53f, 51.1f);
        ctx.lineTo(24.61f, 51.1f);
        ctx.bezier(24.02f, 51.1f, 23.509999999999998f, 51.51f, 23.39f, 52.09f);
        ctx.lineTo(22.47f, 56.45f);
        ctx.bezier(22.31f, 57.230000000000004f, 22.9f, 57.96f, 23.689999999999998f, 57.96f);
        ctx.lineTo(54.599999999999994f, 57.96f);
        ctx.bezier(55.19f, 57.96f, 55.699999999999996f, 57.550000000000004f, 55.81999999999999f, 56.97f);
        ctx.lineTo(56.74999999999999f, 52.61f);
        ctx.bezier(56.90999999999999f, 51.83f, 56.31999999999999f, 51.1f, 55.529999999999994f, 51.1f);

        ctx.move(70.8f, 78.96f);
        ctx.lineTo(66.05f, 68.47999999999999f);
        ctx.bezier(65.67999999999999f, 67.64999999999999f, 66.28f, 66.71999999999998f, 67.19f, 66.71999999999998f);
        ctx.lineTo(71.07f, 66.71999999999998f);
        ctx.bezier(71.64999999999999f, 66.71999999999998f, 72.14999999999999f, 66.31999999999998f, 72.27999999999999f,
                65.75999999999999f);
        ctx.lineTo(77.51999999999998f, 43.85999999999999f);
        ctx.bezier(77.70999999999998f, 43.07999999999999f, 77.10999999999999f, 42.31999999999999f, 76.30999999999999f,
                42.31999999999999f);
        ctx.lineTo(52.56999999999999f, 42.31999999999999f);
        ctx.bezier(51.97999999999999f, 42.31999999999999f, 51.46999999999999f, 42.72999999999999f, 51.349999999999994f,
                43.309999999999995f);
        ctx.lineTo(50.419999999999995f, 47.669999999999995f);
        ctx.bezier(50.26f, 48.449999999999996f, 50.849999999999994f, 49.17999999999999f, 51.63999999999999f,
                49.17999999999999f);
        ctx.lineTo(67.39999999999999f, 49.17999999999999f);
        ctx.bezier(68.19f, 49.17999999999999f, 68.77999999999999f, 49.90999999999999f, 68.61999999999999f,
                50.68999999999999f);
        ctx.lineTo(66.88f, 58.889999999999986f);
        ctx.bezier(66.75999999999999f, 59.469999999999985f, 66.25f, 59.87999999999999f, 65.66f, 59.87999999999999f);
        ctx.lineTo(48.839999999999996f, 59.87999999999999f);
        ctx.bezier(48.24999999999999f, 59.87999999999999f, 47.739999999999995f, 60.289999999999985f, 47.62f,
                60.86999999999999f);
        ctx.lineTo(46.69f, 65.22999999999999f);
        ctx.bezier(46.519999999999996f, 66.00999999999999f, 47.12f, 66.74f, 47.91f, 66.74f);
        ctx.lineTo(56.029999999999994f, 66.74f);
        ctx.bezier(56.50999999999999f, 66.74f, 56.949999999999996f, 67.02f, 57.16f, 67.44999999999999f);
        ctx.lineTo(63.12f, 80.02999999999999f);
        ctx.bezier(63.33f, 80.46999999999998f, 63.76f, 80.73999999999998f, 64.25f, 80.73999999999998f);
        ctx.lineTo(69.67f, 80.73999999999998f);
        ctx.bezier(70.58f, 80.73999999999998f, 71.18f, 79.79999999999998f, 70.81f, 78.97999999999998f);

        ctx.move(41.63f, 79.74f);
        ctx.lineTo(45.540000000000006f, 61.349999999999994f);
        ctx.bezier(45.7f, 60.56999999999999f, 45.11000000000001f, 59.839999999999996f, 44.32000000000001f,
                59.839999999999996f);
        ctx.lineTo(39.74000000000001f, 59.839999999999996f);
        ctx.bezier(39.150000000000006f, 59.839999999999996f, 38.64000000000001f, 60.24999999999999f, 38.52000000000001f,
                60.83f);
        ctx.lineTo(34.60000000000001f, 79.22f);
        ctx.bezier(34.43000000000001f, 80.0f, 35.03000000000001f, 80.73f, 35.82000000000001f, 80.73f);
        ctx.lineTo(40.41000000000001f, 80.73f);
        ctx.bezier(41.000000000000014f, 80.73f, 41.51000000000001f, 80.32000000000001f, 41.63000000000001f,
                79.74000000000001f);
        ctx.endPath(true);

        ctx.popTransform();
    }
}
