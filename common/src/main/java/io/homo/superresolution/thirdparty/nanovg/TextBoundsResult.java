package io.homo.superresolution.thirdparty.nanovg;

public class TextBoundsResult {
    public float advance;
    public float[] bounds;

    public TextBoundsResult() {
        this.bounds = new float[4];
    }

    public TextBoundsResult(float advance, float[] bounds) {
        this.advance = advance;
        this.bounds = bounds != null ? bounds : new float[4];
    }

    @Override
    public String toString() {
        return "TextBoundsResult{" +
                "advance=" + advance +
                ", bounds=[" + bounds[0] + ", " + bounds[1] + ", " + bounds[2] + ", " + bounds[3] + "]" +
                '}';
    }
}
