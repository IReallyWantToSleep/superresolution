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

package io.homo.superresolution.core.gui.core.backends.interfaces;

import org.joml.Vector2f;

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

    public Vector2f transformPoint(Vector2f point) {
        float x = mat[0] * point.x + mat[2] * point.y + mat[4];
        float y = mat[1] * point.x + mat[3] * point.y + mat[5];
        return new Vector2f(x, y);
    }

    public Vector2f inverseTransformPoint(Vector2f point) {
        float a = mat[0], b = mat[1], c = mat[2], d = mat[3], e = mat[4], f = mat[5];
        float det = a * d - b * c;
        if (Math.abs(det) < 1e-10f) {
            return new Vector2f(point);
        }
        float invDet = 1.0f / det;
        float ia = d * invDet;
        float ib = -b * invDet;
        float ic = -c * invDet;
        float id = a * invDet;
        float ie = (c * f - d * e) * invDet;
        float ig = (b * e - a * f) * invDet;

        float x = ia * point.x + ic * point.y + ie;
        float y = ib * point.x + id * point.y + ig;
        return new Vector2f(x, y);
    }

    public Transform inverse() {
        float a = mat[0], b = mat[1], c = mat[2], d = mat[3], e = mat[4], f = mat[5];
        float det = a * d - b * c;
        if (Math.abs(det) < 1e-10f) {
            return Transform.identity();
        }
        float invDet = 1.0f / det;
        float[] invMat = new float[]{
                d * invDet,
                -b * invDet,
                -c * invDet,
                a * invDet,
                (c * f - d * e) * invDet,
                (b * e - a * f) * invDet
        };
        return new Transform(invMat);
    }

    public boolean isIdentity() {
        return Math.abs(mat[0] - 1) < 1e-6f &&
                Math.abs(mat[1]) < 1e-6f &&
                Math.abs(mat[2]) < 1e-6f &&
                Math.abs(mat[3] - 1) < 1e-6f &&
                Math.abs(mat[4]) < 1e-6f &&
                Math.abs(mat[5]) < 1e-6f;
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