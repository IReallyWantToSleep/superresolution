package io.homo.superresolution.thirdparty.nanovg;

public class NVGtextRow {
    public String start;
    public String end;
    public float width;
    public float minx;
    public float maxx;

    public NVGtextRow() {
    }

    public NVGtextRow(String start, String end, float width, float minx, float maxx) {
        this.start = start;
        this.end = end;
        this.width = width;
        this.minx = minx;
        this.maxx = maxx;
    }

    @Override
    public String toString() {
        return "NVGtextRow{" +
                "start='" + start + '\'' +
                ", end='" + end + '\'' +
                ", width=" + width +
                ", minx=" + minx +
                ", maxx=" + maxx +
                '}';
    }
}
