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

public class Vector2f extends Vector<Vector2f> {
    public float x;
    public float y;

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f(float v) {
        this.x = v;
        this.y = v;
    }

    public float x() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float y() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Vector2f add(Vector2f other) {
        return new Vector2f(x + other.x, y + other.y);
    }

    public Vector2f add(float scalar) {
        return new Vector2f(x + scalar, y + scalar);
    }

    public Vector2f subtract(Vector2f other) {
        return new Vector2f(x - other.x, y - other.y);
    }

    public Vector2f subtract(float scalar) {
        return new Vector2f(x - scalar, y - scalar);
    }

    public Vector2f subtractFrom(float scalar) {
        return new Vector2f(scalar - x, scalar - y);
    }

    public Vector2f multiply(Vector2f other) {
        return new Vector2f(x * other.x, y * other.y);
    }

    public Vector2f multiply(float scalar) {
        return new Vector2f(x * scalar, y * scalar);
    }

    public Vector2f divide(Vector2f other) {
        return new Vector2f(x / other.x, y / other.y);
    }

    public Vector2f divide(float scalar) {
        return new Vector2f(x / scalar, y / scalar);
    }

    public Vector2f divideInto(float scalar) {
        return new Vector2f(scalar / x, scalar / y);
    }

    @Override
    protected double[] data() {
        return new double[]{x, y};
    }

    @Override
    public Vector2f copy() {
        return new Vector2f(x, y);
    }
}
