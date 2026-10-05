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

package io.homo.superresolution.core.math;

import org.joml.Vector2f;

public class MathUtil {
    public static Vector2f lerp(Vector2f a, Vector2f b, Vector2f c) {
        return new Vector2f(
                a.x + (b.x - a.x) * c.x,
                a.y + (b.y - a.y) * c.y
        );
    }

    public static Vector2f clamp(Vector2f value, Vector2f min, Vector2f max) {
        return new Vector2f(max).min(value.max(new Vector2f(min)));
    }

    public static float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }
}
