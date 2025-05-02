package io.homo.superresolution.core.impl;

public class Vec3 {
    public float x;
    public float y;
    public float z;

    public Vec3(float v) {
        this.x = v;
        this.y = v;
        this.z = v;

    }

    public Vec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
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


    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 add(float scalar) {
        return new Vec3(x + scalar, y + scalar, z + scalar);
    }

    public Vec3 subtract(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 subtract(float scalar) {
        return new Vec3(x - scalar, y - scalar, z - scalar);
    }

    public Vec3 subtractFrom(float scalar) {
        return new Vec3(scalar - x, scalar - y, scalar - z);
    }

    public Vec3 multiply(Vec3 other) {
        return new Vec3(x * other.x, y * other.y, z * other.z);
    }

    public Vec3 multiply(float scalar) {
        return new Vec3(x * scalar, y * scalar, z * scalar);
    }

    public Vec3 divide(Vec3 other) {
        return new Vec3(x / other.x, y / other.y, z / other.z);
    }

    public Vec3 divide(float scalar) {
        return new Vec3(x / scalar, y / scalar, z / scalar);
    }

    public Vec3 divideInto(float scalar) {
        return new Vec3(scalar / x, scalar / y, scalar / z);
    }

}
