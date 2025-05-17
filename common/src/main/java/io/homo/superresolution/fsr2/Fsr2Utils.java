package io.homo.superresolution.fsr2;

import static java.lang.Math.pow;

public class Fsr2Utils {
    public static int ffxFsr2GetJitterPhaseCount(float renderWidth, float displayWidth) {
        float basePhaseCount = 8.0f;
        float jitterPhaseCount = (float) (basePhaseCount * pow((displayWidth / renderWidth), 2.0f));
        return (int) jitterPhaseCount;
    }

    public static void spdSetup(int[] dispatchThreadGroupCountXY,
                                int[] workGroupOffset,
                                int[] numWorkGroupsAndMips,
                                int[] rectInfo,
                                int mips) {

        workGroupOffset[0] = rectInfo[0] / 64; // rectInfo[0] = left
        workGroupOffset[1] = rectInfo[1] / 64; // rectInfo[1] = top

        int endIndexX = (rectInfo[0] + rectInfo[2] - 1) / 64; // left + width -1
        int endIndexY = (rectInfo[1] + rectInfo[3] - 1) / 64; // top + height -1

        dispatchThreadGroupCountXY[0] = endIndexX + 1 - workGroupOffset[0];
        dispatchThreadGroupCountXY[1] = endIndexY + 1 - workGroupOffset[1];

        numWorkGroupsAndMips[0] = dispatchThreadGroupCountXY[0] * dispatchThreadGroupCountXY[1];

        if (mips >= 0) {
            numWorkGroupsAndMips[1] = mips;
        } else {
            int resolution = Math.max(rectInfo[2], rectInfo[3]);
            double log2Resolution = Math.log(resolution) / Math.log(2);
            double mipsFloat = Math.min(Math.floor(log2Resolution), 12.0);
            numWorkGroupsAndMips[1] = (int) mipsFloat;
        }
    }

    public static void spdSetup(int[] dispatchThreadGroupCountXY,
                                int[] workGroupOffset,
                                int[] numWorkGroupsAndMips,
                                int[] rectInfo) {

        spdSetup(dispatchThreadGroupCountXY, workGroupOffset, numWorkGroupsAndMips, rectInfo, -1);
    }
}
