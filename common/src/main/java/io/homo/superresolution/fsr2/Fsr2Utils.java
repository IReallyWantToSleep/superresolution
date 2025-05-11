package io.homo.superresolution.fsr2;

import static java.lang.Math.pow;

public class Fsr2Utils {
    public static int ffxFsr2GetJitterPhaseCount(float renderWidth, float displayWidth) {
        float basePhaseCount = 8.0f;
        float jitterPhaseCount = (float) (basePhaseCount * pow((displayWidth / renderWidth), 2.0f));
        return (int) jitterPhaseCount;
    }
}
