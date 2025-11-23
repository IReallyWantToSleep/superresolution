package io.homo.superresolution.thirdparty.nanovg;


public class TextMetricsResult {
    public float ascender;
    public float descender;
    public float lineHeight;

    public TextMetricsResult() {
    }

    public TextMetricsResult(float ascender, float descender, float lineHeight) {
        this.ascender = ascender;
        this.descender = descender;
        this.lineHeight = lineHeight;
    }

    @Override
    public String toString() {
        return "TextMetricsResult{" +
                "ascender=" + ascender +
                ", descender=" + descender +
                ", lineHeight=" + lineHeight +
                '}';
    }
}
