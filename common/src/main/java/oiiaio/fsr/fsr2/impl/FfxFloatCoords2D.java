package oiiaio.fsr.fsr2.impl;

public class FfxFloatCoords2D {
    private float x;
    private float y;

    public FfxFloatCoords2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public static FfxFloatCoords2D create(float x, float y) {
        return new FfxFloatCoords2D(x, y);
    }
}