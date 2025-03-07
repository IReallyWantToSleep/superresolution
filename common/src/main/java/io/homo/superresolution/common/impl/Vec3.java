package io.homo.superresolution.common.impl;

public class Vec3 {
    public float x = 0;
    public float y = 0;
    public float z = 0;

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

}
