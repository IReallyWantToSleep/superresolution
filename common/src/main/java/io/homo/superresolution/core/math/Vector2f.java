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
