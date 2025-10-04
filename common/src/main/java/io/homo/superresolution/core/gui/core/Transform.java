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

package io.homo.superresolution.core.gui.core;

import io.homo.superresolution.core.math.Vector2f;

import java.util.Arrays;

public class Transform {
    private float[] mat = identityMatrix();

    public Transform() {
    }

    public Transform(float[] mat) {
        this.mat = Arrays.copyOf(mat, 6);
    }

    public static float[] identityMatrix() {
        return new float[]{1, 0, 0, 1, 0, 0};
    }

    public static Transform identity() {
        return new Transform();
    }

    public static float[] multiply(float[] left, float[] right) {
        float a = left[0] * right[0] + left[2] * right[1];
        float b = left[1] * right[0] + left[3] * right[1];
        float c = left[0] * right[2] + left[2] * right[3];
        float d = left[1] * right[2] + left[3] * right[3];
        float e = left[0] * right[4] + left[2] * right[5] + left[4];
        float f = left[1] * right[4] + left[3] * right[5] + left[5];
        return new float[]{a, b, c, d, e, f};
    }

    public float[] getMatrix() {
        return Arrays.copyOf(mat, 6);
    }

    public Transform setMatrix(float[] newMat) {
        this.mat = Arrays.copyOf(newMat, 6);
        return this;
    }

    public Transform copy() {
        return new Transform(mat);
    }

    public Transform translate(float x, float y) {
        float[] t = new float[]{1, 0, 0, 1, x, y};
        mat = multiply(mat, t);
        return this;
    }

    public Transform translate(Vector2f v) {
        return translate(v.x(), v.y());
    }

    public Transform scale(float sx, float sy) {
        float[] s = new float[]{sx, 0, 0, sy, 0, 0};
        mat = multiply(mat, s);
        return this;
    }

    public Transform scale(float s) {
        return scale(s, s);
    }

    public Transform scaleAt(float sx, float sy, float cx, float cy) {
        // T(cx, cy) * S(sx, sy) * T(-cx, -cy)
        translate(cx, cy);
        scale(sx, sy);
        translate(-cx, -cy);
        return this;
    }


    public Transform rotate(float radians) {
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float[] r = new float[]{cos, sin, -sin, cos, 0, 0};
        mat = multiply(mat, r);
        return this;
    }

    public Transform rotateAt(float radians, float cx, float cy) {
        translate(cx, cy);
        rotate(radians);
        translate(-cx, -cy);
        return this;
    }

    public Transform rotateDegrees(float degrees) {
        float cos = (float) Math.cos(Math.toDegrees(degrees));
        float sin = (float) Math.sin(Math.toDegrees(degrees));
        float[] r = new float[]{cos, sin, -sin, cos, 0, 0};
        mat = multiply(mat, r);
        return this;
    }

    public Transform rotateDegreesAt(float degrees, float cx, float cy) {
        translate(cx, cy);
        rotate((float) Math.toDegrees(degrees));
        translate(-cx, -cy);
        return this;
    }

    public Transform setIdentity() {
        mat = identityMatrix();
        return this;
    }


    public float[] transformMatrix() {
        return getMatrix();
    }

    @Override
    public String toString() {
        float[] m = getMatrix();
        return String.format(
                "Transform[[% .3f % .3f % .3f][% .3f % .3f % .3f]]",
                m[0], m[2], m[4], m[1], m[3], m[5]
        );
    }
}