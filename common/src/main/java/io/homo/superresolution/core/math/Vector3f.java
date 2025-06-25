package io.homo.superresolution.core.math;

public class Vector3f extends Vector<Vector3f> {
    public float x;
    public float y;
    public float z;

    public Vector3f(float v) {
        this.x = v;
        this.y = v;
        this.z = v;

    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Vector3f copy() {
        return new Vector3f(x, y, z);
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float z() {
        return z;
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


    public Vector3f add(Vector3f other) {
        return new Vector3f(x + other.x, y + other.y, z + other.z);
    }

    public Vector3f add(float scalar) {
        return new Vector3f(x + scalar, y + scalar, z + scalar);
    }

    public Vector3f subtract(Vector3f other) {
        return new Vector3f(x - other.x, y - other.y, z - other.z);
    }

    public Vector3f subtract(float scalar) {
        return new Vector3f(x - scalar, y - scalar, z - scalar);
    }

    public Vector3f subtractFrom(float scalar) {
        return new Vector3f(scalar - x, scalar - y, scalar - z);
    }

    public Vector3f multiply(Vector3f other) {
        return new Vector3f(x * other.x, y * other.y, z * other.z);
    }

    public Vector3f multiply(float scalar) {
        return new Vector3f(x * scalar, y * scalar, z * scalar);
    }

    public Vector3f divide(Vector3f other) {
        return new Vector3f(x / other.x, y / other.y, z / other.z);
    }

    public Vector3f divide(float scalar) {
        return new Vector3f(x / scalar, y / scalar, z / scalar);
    }

    public Vector3f divideInto(float scalar) {
        return new Vector3f(scalar / x, scalar / y, scalar / z);
    }

    @Override
    protected double[] data() {
        return new double[]{x, y, z};
    }
}
