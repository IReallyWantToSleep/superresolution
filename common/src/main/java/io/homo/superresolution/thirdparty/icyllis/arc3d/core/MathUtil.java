/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.thirdparty.icyllis.arc3d.core;

import java.lang.invoke.*;

/**
 * Utility class that provides auxiliary operations.
 */
public class MathUtil {
    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static int clamp(int x, int min, int max) {
        return Math.min(max, Math.max(x, min));
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static long clamp(long x, long min, long max) {
        return Math.min(max, Math.max(x, min));
    }

    /**
     * Clamps x between min and max, exactly. If x is NaN, returns NaN.
     *
     * @return x clamped between min and max
     */
    public static float clamp(float x, float min, float max) {
        return Math.min(max, Math.max(x, min));
    }

    /**
     * Clamps x between min and max, exactly. If x is NaN, returns NaN.
     *
     * @return x clamped between min and max
     */
    public static double clamp(double x, double min, double max) {
        return Math.min(max, Math.max(x, min));
    }

    /**
     * Clamps x between min and max. If x is NaN, returns min.
     * Note the result is incorrect if min is negative zero.
     *
     * @return x clamped between min and max
     */
    @SuppressWarnings("ManualMinMaxCalculation")
    public static float pin(float x, float min, float max) {
        float y = max < x ? max : x;
        return min < y ? y : min;
    }

    /**
     * Clamps x between min and max. If x is NaN, returns min.
     * Note the result is incorrect if min is negative zero.
     *
     * @return x clamped between min and max
     */
    @SuppressWarnings("ManualMinMaxCalculation")
    public static double pin(double x, double min, double max) {
        double y = max < x ? max : x;
        return min < y ? y : min;
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min3(float[] v) {
        return Math.min(Math.min(v[0], v[1]), v[2]);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static double min(double a, double b, double c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static double min(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static double max(double a, double b, double c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static double max(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Linear interpolation between two values.
     */
    public static float lerp(float a, float b, float t) {
        return (b - a) * t + a;
    }

    /**
     * Linear interpolation between two values.
     */
    public static double lerp(double a, double b, double t) {
        return (b - a) * t + a;
    }

    /**
     * Linear interpolation between two values, matches GLSL {@code mix} intrinsic function.
     * Slower than {@link #lerp(float, float, float)} but without intermediate overflow or underflow.
     */
    public static float mix(float a, float b, float t) {
        return a * (1 - t) + b * t;
    }

    /**
     * Linear interpolation between two values, matches GLSL {@code mix} intrinsic function.
     * Slower than {@link #lerp(double, double, double)} but without intermediate overflow or underflow.
     */
    public static double mix(double a, double b, double t) {
        return a * (1 - t) + b * t;
    }


    /**
     * Returns the log2 of the provided value, were that value to be rounded up to the next power of 2.
     * Returns 0 if value <= 0:<br>
     * Never returns a negative number, even if value is NaN.
     * <pre>
     *     ceilLog2((-inf..1]) -> 0
     *     ceilLog2((1..2])    -> 1
     *     ceilLog2((2..4])    -> 2
     *     ceilLog2((4..8])    -> 3
     *     ceilLog2(+inf)      -> 128
     *     ceilLog2(NaN)       -> 0
     * </pre>
     * NextLog2.
     */
    public static int ceilLog2(float v) {
        int exp = ((Float.floatToRawIntBits(v) + (1 << 23) - 1) >> 23) - 127;
        return exp & ~(exp >> 31);
    }

    /**
     * Returns ceil(log2(sqrt(x))):
     * <pre>
     *     log2(sqrt(x)) == log2(x^(1/2)) == log2(x)/2 == log2(x)/log2(4) == log4(x)
     * </pre>
     */
    public static int ceilLog4(float v) {
        return (ceilLog2(v) + 1) >> 1;
    }

    /**
     * Returns ceil(log2(sqrt(sqrt(x)))):
     * <pre>
     *     log2(sqrt(sqrt(x))) == log2(x^(1/4)) == log2(x)/4 == log2(x)/log2(16) == log16(x)
     * </pre>
     */
    public static int ceilLog16(float v) {
        return (ceilLog2(v) + 3) >> 2;
    }

    protected MathUtil() {
        throw new UnsupportedOperationException();
    }
}
