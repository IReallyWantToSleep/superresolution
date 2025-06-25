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
