package io.homo.superresolution.core.impl;

public class Vec4 {
    public float x;
    public float y;
    public float z;
    public float w;

    public Vec4(float v) {
        this.x = v;
        this.y = v;
        this.z = v;
        this.w = v;
    }

    public Vec4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
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

    public Vec4 add(Vec4 other) {
        return new Vec4(x + other.x, y + other.y, z + other.z, w + other.w);
    }

    public Vec4 add(float scalar) {
        return new Vec4(x + scalar, y + scalar, z + scalar, w + scalar);
    }

    public Vec4 subtract(Vec4 other) {
        return new Vec4(x - other.x, y - other.y, z - other.z, w - other.w);
    }

    public Vec4 subtract(float scalar) {
        return new Vec4(x - scalar, y - scalar, z - scalar, w - scalar);
    }

    public Vec4 subtractFrom(float scalar) {
        return new Vec4(scalar - x, scalar - y, scalar - z, scalar - w);
    }

    public Vec4 multiply(Vec4 other) {
        return new Vec4(x * other.x, y * other.y, z * other.z, w * other.w);
    }

    public Vec4 multiply(float scalar) {
        return new Vec4(x * scalar, y * scalar, z * scalar, w * scalar);
    }

    public Vec4 divide(Vec4 other) {
        return new Vec4(x / other.x, y / other.y, z / other.z, w / other.w);
    }

    public Vec4 divide(float scalar) {
        return new Vec4(x / scalar, y / scalar, z / scalar, w / scalar);
    }

    public Vec4 divideInto(float scalar) {
        return new Vec4(scalar / x, scalar / y, scalar / z, scalar / w);
    }
}