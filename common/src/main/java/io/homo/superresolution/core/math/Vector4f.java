package io.homo.superresolution.core.math;

public class Vector4f extends Vector<Vector4f> {
    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4f(float v) {
        this.x = v;
        this.y = v;
        this.z = v;
        this.w = v;
    }

    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    @Override
    public Vector4f copy() {
        return new Vector4f(x, y, z, w);
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

    public float z() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float w() {
        return w;
    }

    public void setW(float w) {
        this.w = w;
    }

    public Vector4f add(Vector4f other) {
        return new Vector4f(x + other.x, y + other.y, z + other.z, w + other.w);
    }

    public Vector4f add(float scalar) {
        return new Vector4f(x + scalar, y + scalar, z + scalar, w + scalar);
    }

    public Vector4f subtract(Vector4f other) {
        return new Vector4f(x - other.x, y - other.y, z - other.z, w - other.w);
    }

    public Vector4f subtract(float scalar) {
        return new Vector4f(x - scalar, y - scalar, z - scalar, w - scalar);
    }

    public Vector4f subtractFrom(float scalar) {
        return new Vector4f(scalar - x, scalar - y, scalar - z, scalar - w);
    }

    public Vector4f multiply(Vector4f other) {
        return new Vector4f(x * other.x, y * other.y, z * other.z, w * other.w);
    }

    public Vector4f multiply(float scalar) {
        return new Vector4f(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    public Vector4f divide(Vector4f other) {
        return new Vector4f(x / other.x, y / other.y, z / other.z, w / other.w);
    }

    public Vector4f divide(float scalar) {
        return new Vector4f(x / scalar, y / scalar, z / scalar, w / scalar);
    }

    public Vector4f divideInto(float scalar) {
        return new Vector4f(scalar / x, scalar / y, scalar / z, scalar / w);
    }

    @Override
    protected double[] data() {
        return new double[]{x, y, z, w};
    }
}