package io.homo.superresolution.core.math;

public class Vector3i extends Vector<Vector3i> {
    public int x;
    public int y;
    public int z;

    public Vector3i(int v) {
        this.x = v;
        this.y = v;
        this.z = v;

    }

    public Vector3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Vector3i copy() {
        return new Vector3i(x, y, z);
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int z() {
        return z;
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


    public Vector3i add(Vector3i other) {
        return new Vector3i(x + other.x, y + other.y, z + other.z);
    }

    public Vector3i add(int scalar) {
        return new Vector3i(x + scalar, y + scalar, z + scalar);
    }

    public Vector3i subtract(Vector3i other) {
        return new Vector3i(x - other.x, y - other.y, z - other.z);
    }

    public Vector3i subtract(int scalar) {
        return new Vector3i(x - scalar, y - scalar, z - scalar);
    }

    public Vector3i subtractFrom(int scalar) {
        return new Vector3i(scalar - x, scalar - y, scalar - z);
    }

    public Vector3i multiply(Vector3i other) {
        return new Vector3i(x * other.x, y * other.y, z * other.z);
    }

    public Vector3i multiply(int scalar) {
        return new Vector3i(x * scalar, y * scalar, z * scalar);
    }

    public Vector3i divide(Vector3i other) {
        return new Vector3i(x / other.x, y / other.y, z / other.z);
    }

    public Vector3i divide(int scalar) {
        return new Vector3i(x / scalar, y / scalar, z / scalar);
    }

    public Vector3i divideInto(int scalar) {
        return new Vector3i(scalar / x, scalar / y, scalar / z);
    }

    @Override
    protected double[] data() {
        return new double[]{x, y, z};
    }
}
