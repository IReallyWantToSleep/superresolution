package io.homo.superresolution.core.impl;

public class Vec2 {
    public float x;
    public float y;

    public Vec2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vec2(float v) {
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

    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 add(float scalar) {
        return new Vec2(x + scalar, y + scalar);
    }

    public Vec2 subtract(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 subtract(float scalar) {
        return new Vec2(x - scalar, y - scalar);
    }

    public Vec2 subtractFrom(float scalar) {
        return new Vec2(scalar - x, scalar - y);
    }

    public Vec2 multiply(Vec2 other) {
        return new Vec2(x * other.x, y * other.y);
    }

    public Vec2 multiply(float scalar) {
        return new Vec2(x * scalar, y * scalar);
    }

    public Vec2 divide(Vec2 other) {
        return new Vec2(x / other.x, y / other.y);
    }

    public Vec2 divide(float scalar) {
        return new Vec2(x / scalar, y / scalar);
    }

    public Vec2 divideInto(float scalar) {
        return new Vec2(scalar / x, scalar / y);
    }

}
