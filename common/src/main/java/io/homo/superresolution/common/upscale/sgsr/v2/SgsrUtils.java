package io.homo.superresolution.common.upscale.sgsr.v2;

public class SgsrUtils {
    public static int divideRoundUp(int dividend, int divisor) {
        return (dividend + divisor - 1) / divisor;
    }
}
