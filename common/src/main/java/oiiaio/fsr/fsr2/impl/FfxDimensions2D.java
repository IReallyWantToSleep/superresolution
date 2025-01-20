package oiiaio.fsr.fsr2.impl;

public class FfxDimensions2D {
    private int width;
    private int height;
    public FfxDimensions2D(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    public static FfxDimensions2D create(int width, int height) {
        return new FfxDimensions2D(width, height);
    }
}