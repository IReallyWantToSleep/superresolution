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

public class BezierInterpolator implements TimeInterpolator {
    public static final TimeInterpolator LINEAR = new BezierInterpolator(0, 0, 1, 1);
    public static final TimeInterpolator EASE = new BezierInterpolator(0.25, 0.1, 0.25, 1);
    public static final TimeInterpolator EASE_IN = new BezierInterpolator(0.42, 0, 1, 1);
    public static final TimeInterpolator EASE_OUT = new BezierInterpolator(0, 0, 0.58, 1);
    public static final TimeInterpolator EASE_IN_OUT = new BezierInterpolator(0.42, 0, 0.58, 1);
    private static final int PRECISION = 24;
    private static final float STEP_SIZE = 1.0f / (PRECISION - 1);

    private final float[] values;
    private final boolean isLinear;

    public BezierInterpolator(double x1, double y1, double x2, double y2) {
        if (x1 == y1 && x2 == y2) {
            isLinear = true;
            values = null;
            return;
        }

        isLinear = false;
        values = new float[PRECISION];
        double px3 = 3 * x1;
        double px2 = 3 * (x2 - x1) - px3;
        double px1 = 1 - px3 - px2;

        double py3 = 3 * y1;
        double py2 = 3 * (y2 - y1) - py3;
        double py1 = 1 - py3 - py2;

        for (int i = 0; i < PRECISION; i++) {
            double targetX = i * (double) STEP_SIZE;
            if (i == 0) {
                values[0] = 0f;
                continue;
            }
            if (i == PRECISION - 1) {
                values[PRECISION - 1] = 1f;
                continue;
            }
            double low = 0;
            double high = 1;
            double t = 0.5;
            for (int k = 0; k < 16; k++) {
                t = (low + high) * 0.5;
                double xEstimate = ((px1 * t + px2) * t + px3) * t;
                if (xEstimate < targetX) {
                    low = t;
                } else {
                    high = t;
                }
            }
            double y = ((py1 * t + py2) * t + py3) * t;
            values[i] = (float) y;
        }
    }

    public static TimeInterpolator of(double x1, double y1, double x2, double y2) {
        return new BezierInterpolator(x1, y1, x2, y2);
    }

    @Override
    public float interpolation(float input) {
        if (isLinear) return input;
        if (input <= 0) return 0;
        if (input >= 1) return 1;
        float position = input * (PRECISION - 1);
        int index = (int) position;
        if (index >= PRECISION - 1) {
            return values[PRECISION - 1];
        }
        float weight = position - index;
        return values[index] + weight * (values[index + 1] - values[index]);
    }
}