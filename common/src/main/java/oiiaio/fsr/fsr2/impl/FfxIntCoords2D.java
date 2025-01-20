package oiiaio.fsr.fsr2.impl;

public class FfxIntCoords2D {
    private int x;
    private int y;
    public FfxIntCoords2D(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public static FfxIntCoords2D create(int x, int y) {
        return new FfxIntCoords2D(x, y);
    }
}