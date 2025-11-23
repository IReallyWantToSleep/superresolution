package io.homo.superresolution.thirdparty.nanovg;

public class NVGglyphPosition {
    public String str;
    public float x;
    public float minx;
    public float maxx;

    public NVGglyphPosition() {
    }

    public NVGglyphPosition(String str, float x, float minx, float maxx) {
        this.str = str;
        this.x = x;
        this.minx = minx;
        this.maxx = maxx;
    }

    @Override
    public String toString() {
        return "NVGglyphPosition{" +
                "str='" + str + '\'' +
                ", x=" + x +
                ", minx=" + minx +
                ", maxx=" + maxx +
                '}';
    }
}
