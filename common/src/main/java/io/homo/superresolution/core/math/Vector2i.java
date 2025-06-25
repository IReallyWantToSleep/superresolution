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
