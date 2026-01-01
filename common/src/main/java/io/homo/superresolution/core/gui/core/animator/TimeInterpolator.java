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

package io.homo.superresolution.core.gui.core.animator;

@FunctionalInterface
public interface TimeInterpolator {
    float interpolation(float progress);

    float PI = (float) Math.PI;
    float C1 = 1.70158f;
    float C2 = C1 * 1.525f;
    float C3 = C1 + 1;
    float C4 = (2 * PI) / 3;
    float C5 = (2 * PI) / 4.5f;

    static float bounceOutHelper(float x) {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (x < 1 / d1) {
            return n1 * x * x;
        } else if (x < 2 / d1) {
            return n1 * (x -= 1.5f / d1) * x + 0.75f;
        } else if (x < 2.5f / d1) {
            return n1 * (x -= 2.25f / d1) * x + 0.9375f;
        } else {
            return n1 * (x -= 2.625f / d1) * x + 0.984375f;
        }
    }

    static TimeInterpolator linear() {
        return x -> x;
    }

    static TimeInterpolator easeInQuad() {
        return x -> x * x;
    }

    static TimeInterpolator easeOutQuad() {
        return x -> 1 - (1 - x) * (1 - x);
    }

    static TimeInterpolator easeInOutQuad() {
        return x -> x < 0.5f ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
    }

    static TimeInterpolator easeInCubic() {
        return x -> x * x * x;
    }

    static TimeInterpolator easeOutCubic() {
        return x -> 1 - (float) Math.pow(1 - x, 3);
    }

    static TimeInterpolator easeInOutCubic() {
        return x -> x < 0.5f ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
    }

    static TimeInterpolator easeInQuart() {
        return x -> x * x * x * x;
    }

    static TimeInterpolator easeOutQuart() {
        return x -> 1 - (float) Math.pow(1 - x, 4);
    }

    static TimeInterpolator easeInOutQuart() {
        return x -> x < 0.5f ? 8 * x * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 4) / 2;
    }

    static TimeInterpolator easeInQuint() {
        return x -> x * x * x * x * x;
    }

    static TimeInterpolator easeOutQuint() {
        return x -> 1 - (float) Math.pow(1 - x, 5);
    }

    static TimeInterpolator easeInOutQuint() {
        return x -> x < 0.5f ? 16 * x * x * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 5) / 2;
    }

    static TimeInterpolator easeInSine() {
        return x -> 1 - (float) Math.cos((x * PI) / 2);
    }

    static TimeInterpolator easeOutSine() {
        return x -> (float) Math.sin((x * PI) / 2);
    }

    static TimeInterpolator easeInOutSine() {
        return x -> -((float) Math.cos(PI * x) - 1) / 2;
    }

    static TimeInterpolator easeInExpo() {
        return x -> x == 0 ? 0 : (float) Math.pow(2, 10 * x - 10);
    }

    static TimeInterpolator easeOutExpo() {
        return x -> x == 1 ? 1 : 1 - (float) Math.pow(2, -10 * x);
    }

    static TimeInterpolator easeInOutExpo() {
        return x -> x == 0 ? 0
                : x == 1 ? 1
                : x < 0.5f ? (float) Math.pow(2, 20 * x - 10) / 2
                : (2 - (float) Math.pow(2, -20 * x + 10)) / 2;
    }

    static TimeInterpolator easeInCirc() {
        return x -> 1 - (float) Math.sqrt(1 - Math.pow(x, 2));
    }

    static TimeInterpolator easeOutCirc() {
        return x -> (float) Math.sqrt(1 - Math.pow(x - 1, 2));
    }

    static TimeInterpolator easeInOutCirc() {
        return x -> x < 0.5f
                ? (1 - (float) Math.sqrt(1 - Math.pow(2 * x, 2))) / 2
                : ((float) Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2;
    }

    static TimeInterpolator easeInBack() {
        return x -> C3 * x * x * x - C1 * x * x;
    }

    static TimeInterpolator easeOutBack() {
        return x -> 1 + C3 * (float) Math.pow(x - 1, 3) + C1 * (float) Math.pow(x - 1, 2);
    }

    static TimeInterpolator easeInOutBack() {
        return x -> x < 0.5f
                ? ((float) Math.pow(2 * x, 2) * ((C2 + 1) * 2 * x - C2)) / 2
                : ((float) Math.pow(2 * x - 2, 2) * ((C2 + 1) * (x * 2 - 2) + C2) + 2) / 2;
    }

    static TimeInterpolator easeInElastic() {
        return x -> x == 0 ? 0
                : x == 1 ? 1
                : -(float) Math.pow(2, 10 * x - 10) * (float) Math.sin((x * 10 - 10.75f) * C4);
    }

    static TimeInterpolator easeOutElastic() {
        return x -> x == 0 ? 0
                : x == 1 ? 1
                : (float) Math.pow(2, -10 * x) * (float) Math.sin((x * 10 - 0.75f) * C4) + 1;
    }

    static TimeInterpolator easeInOutElastic() {
        return x -> x == 0 ? 0
                : x == 1 ? 1
                : x < 0.5f
                ? -((float) Math.pow(2, 20 * x - 10) * (float) Math.sin((20 * x - 11.125f) * C5)) / 2
                : ((float) Math.pow(2, -20 * x + 10) * (float) Math.sin((20 * x - 11.125f) * C5)) / 2 + 1;
    }

    static TimeInterpolator easeInBounce() {
        return x -> 1 - bounceOutHelper(1 - x);
    }

    static TimeInterpolator easeOutBounce() {
        return TimeInterpolator::bounceOutHelper;
    }

    static TimeInterpolator easeInOutBounce() {
        return x -> x < 0.5f
                ? (1 - bounceOutHelper(1 - 2 * x)) / 2
                : (1 + bounceOutHelper(2 * x - 1)) / 2;
    }
}
