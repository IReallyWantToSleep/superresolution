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

package io.homo.superresolution.core.math;

public class Vector4i extends Vector<Vector4i> {
    public int x;
    public int y;
    public int z;
    public int w;

    public Vector4i(int v) {
        this.x = v;
        this.y = v;
        this.z = v;
        this.w = v;
    }

    public Vector4i(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    @Override
    public Vector4i copy() {
        return new Vector4i(x, y, z, w);
    }

    public int x() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int y() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int z() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int w() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public Vector4i add(Vector4i other) {
        return new Vector4i(x + other.x, y + other.y, z + other.z, w + other.w);
    }

    public Vector4i add(int scalar) {
        return new Vector4i(x + scalar, y + scalar, z + scalar, w + scalar);
    }

    public Vector4i subtract(Vector4i other) {
        return new Vector4i(x - other.x, y - other.y, z - other.z, w - other.w);
    }

    public Vector4i subtract(int scalar) {
        return new Vector4i(x - scalar, y - scalar, z - scalar, w - scalar);
    }

    public Vector4i subtractFrom(int scalar) {
        return new Vector4i(scalar - x, scalar - y, scalar - z, scalar - w);
    }

    public Vector4i multiply(Vector4i other) {
        return new Vector4i(x * other.x, y * other.y, z * other.z, w * other.w);
    }

    public Vector4i multiply(int scalar) {
        return new Vector4i(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    public Vector4i divide(Vector4i other) {
        return new Vector4i(x / other.x, y / other.y, z / other.z, w / other.w);
    }

    public Vector4i divide(int scalar) {
        return new Vector4i(x / scalar, y / scalar, z / scalar, w / scalar);
    }

    public Vector4i divideInto(int scalar) {
        return new Vector4i(scalar / x, scalar / y, scalar / z, scalar / w);
    }

    @Override
    protected double[] data() {
        return new double[]{x, y, z, w};
    }
}