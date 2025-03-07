package io.homo.superresolution.common.impl;

public class Vec2 {
    public float x = 0;
    public float y = 0;

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

}
