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

public abstract class Vector<SELF> {
    public static Vector2f vector2f(Vector<?> vector) {
        double[] data = vector.data();
        if (data.length != 2) {
            throw new RuntimeException();
        }
        return new Vector2f((float) data[0], (float) data[1]);
    }

    public static Vector2i vector2i(Vector<?> vector) {
        double[] data = vector.data();
        if (data.length != 2) {
            throw new RuntimeException();
        }
        return new Vector2i((int) data[0], (int) data[1]);
    }

    public static Vector3f vector3f(Vector<?> vector) {
        double[] data = vector.data();
        if (data.length == 2) {
            return new Vector3f((float) data[0], (float) data[1], 1);
        }
        if (data.length != 3) {
            throw new RuntimeException();
        }
        return new Vector3f((float) data[0], (float) data[1], (float) data[2]);
    }

    public static Vector3i vector3i(Vector<?> vector) {
        double[] data = vector.data();
        if (data.length == 2) {
            return new Vector3i((int) data[0], (int) data[1], 1);
        }
        if (data.length != 3) {
            throw new RuntimeException();
        }
        return new Vector3i((int) data[0], (int) data[1], (int) data[2]);
    }

    public static Vector4f vector4f(Vector<?> vector) {
        double[] data = vector.data();
        if (data.length == 2) {
            return new Vector4f((float) data[0], (float) data[1], 1, 1);
        }
        if (data.length == 3) {
            return new Vector4f((float) data[0], (float) data[1], (float) data[2], 1);
        }
        if (data.length != 4) {
            throw new RuntimeException();
        }
        return new Vector4f((float) data[0], (float) data[1], (float) data[2], (float) data[3]);
    }

    public static Vector4i vector4i(Vector<?> vector) {
        double[] data = vector.data();
        if (data.length == 2) {
            return new Vector4i((int) data[0], (int) data[1], 1, 1);
        }
        if (data.length == 3) {
            return new Vector4i((int) data[0], (int) data[1], (int) data[2], 1);
        }
        if (data.length != 4) {
            throw new RuntimeException();
        }
        return new Vector4i((int) data[0], (int) data[1], (int) data[2], (int) data[3]);
    }

    protected abstract double[] data();

    public abstract SELF copy();

}
