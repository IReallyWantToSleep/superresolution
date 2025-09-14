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

public class Vector2i extends Vector<Vector2i> {
    public int x;
    public int y;

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2i(int v) {
        this.x = v;
        this.y = v;
    }

    @Override
    public Vector2i copy() {
        return new Vector2i(x, y);
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

    public Vector2i add(Vector2i other) {
        return new Vector2i(x + other.x, y + other.y);
    }

    public Vector2i add(int scalar) {
        return new Vector2i(x + scalar, y + scalar);
    }

    public Vector2i subtract(Vector2i other) {
        return new Vector2i(x - other.x, y - other.y);
    }

    public Vector2i subtract(int scalar) {
        return new Vector2i(x - scalar, y - scalar);
    }

    public Vector2i subtractFrom(int scalar) {
        return new Vector2i(scalar - x, scalar - y);
    }

    public Vector2i multiply(Vector2i other) {
        return new Vector2i(x * other.x, y * other.y);
    }

    public Vector2i multiply(int scalar) {
        return new Vector2i(x * scalar, y * scalar);
    }

    public Vector2i divide(Vector2i other) {
        return new Vector2i(x / other.x, y / other.y);
    }

    public Vector2i divide(int scalar) {
        return new Vector2i(x / scalar, y / scalar);
    }

    public Vector2i divideInto(int scalar) {
        return new Vector2i(scalar / x, scalar / y);
    }

    @Override
    protected double[] data() {
        return new double[]{x, y};
    }
}
