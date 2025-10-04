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

package io.homo.superresolution.core.gui.core.animator;

import java.util.function.Function;

public class Easing {
    public static EasingMethod LINEAR = (Float T) -> T;
    public static EasingMethod EASE_IN_QUAD = (Float T) -> T * T;
    public static EasingMethod EASE_OUT_QUAD = (Float T) -> T * (2 - T);
    public static EasingMethod EASE_IN_OUT_QUAD = (Float T) -> T < 0.5 ? 2 * T * T : -1 + (4 - 2 * T) * T;
    public static EasingMethod EASE_IN_CUBIC = (Float T) -> T * T * T;
    public static EasingMethod EASE_OUT_CUBIC = (Float T) -> (--T) * T * T + 1;
    public static EasingMethod EASE_IN_OUT_CUBIC = (Float T) -> T < 0.5 ? 4 * T * T * T : (T - 1) * (2 * T - 2) * (2 * T - 2) + 1;
    public static EasingMethod EASE_IN_QUART = (Float T) -> T * T * T * T;
    public static EasingMethod EASE_OUT_QUART = (Float T) -> 1 - (--T) * T * T * T;
    public static EasingMethod EASE_IN_OUT_QUART = (Float T) -> T < 0.5 ? 8 * T * T * T * T : 1 - 8 * (--T) * T * T * T;
    public static EasingMethod EASE_IN_QUINT = (Float T) -> T * T * T * T * T;
    public static EasingMethod EASE_OUT_QUINT = (Float T) -> 1 + (--T) * T * T * T * T;
    public static EasingMethod EASE_IN_OUT_QUINT = (Float T) -> T < 0.5 ? 16 * T * T * T * T * T : 1 + 16 * (--T) * T * T * T * T;
    public static EasingMethod EASE_IN_BACK = (Float x) -> 2.70158f * x * x * x - 1.70158f * x * x;
    public static EasingMethod EASE_OUT_BACK = (Float x) -> (float) (1f + 2.70158f * Math.pow(x - 1f, 3f) + 1.70158f * Math.pow(x - 1f, 2f));

    public static EasingMethod cubicBezier(float p1x, float p1y, float p2x, float p2y) {
        return new EasingMethod() {
            @Override
            public Float apply(Float t) {
                float u = t;
                for (int i = 0; i < 8; i++) {
                    float x = bezierX(u, p1x, p2x);
                    float dx = bezierXDerivative(u, p1x, p2x);
                    if (Math.abs(dx) < 1e-6f) break;
                    u -= (x - t) / dx;
                    u = Math.max(0, Math.min(1, u));
                }
                float a = (p1x * (1 - t) * (1 - t) * (1 - t)) +
                        (3 * p1y * t * ((1 - t) * (1 - t))) +
                        (3 * p2x * t * t * ((1 - t))) +
                        (p2y * t * t * t);
                return bezierY(u, p1y, p2y);
            }

            private float bezierX(float t, float p1x, float p2x) {
                float u = 1 - t;
                return 3 * u * u * t * p1x + 3 * u * t * t * p2x + t * t * t;
            }

            private float bezierY(float t, float p1y, float p2y) {
                float u = 1 - t;
                return 3 * u * u * t * p1y + 3 * u * t * t * p2y + t * t * t;
            }

            private float bezierXDerivative(float t, float p1x, float p2x) {
                float u = 1 - t;
                return 3 * u * u * p1x + 6 * u * t * (p2x - p1x) + 3 * t * t * (1 - p2x);
            }
        };
    }

    public interface EasingMethod extends Function<Float, Float> {

    }
}