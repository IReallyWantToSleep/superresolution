/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.thirdparty.fsr2.common;


import org.joml.Vector2f;

import static java.lang.Math.floor;
import static java.lang.Math.pow;

public class Fsr2Utils {


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

    private static int packHalf2x16(float x, float y) {
        int hx = floatToHalfIntBits(x);
        int hy = floatToHalfIntBits(y);
        return (hy << 16) | (hx & 0xffff);
    }

    private static int floatToHalfIntBits(float value) {
        int bits = Float.floatToRawIntBits(value);
        int sign = (bits >>> 16) & 0x8000;
        int exp = ((bits >>> 23) & 0xff) - 127 + 15;

        if (exp > 0x1f) exp = 0x1f;
        if (exp < 0) exp = 0;

        int mantissa = (bits >>> 13) & 0x3ff;
        return sign | (exp << 10) | mantissa;
    }

    public static void rcasCon(int[] con, float sharpness) {
        sharpness = (float) Math.pow(2.0, -sharpness);
        con[0] = Float.floatToRawIntBits(sharpness);
        con[1] = packHalf2x16(sharpness, sharpness);
        con[2] = 0;
        con[3] = 0;

    }

    public static float halton(int index, int base) {
        float f = 1.0f, result = 0.0f;

        for (int currentIndex = index; currentIndex > 0; ) {
            f /= (float) base;
            result = result + f * (float) (currentIndex % base);
            currentIndex = (int) (floor((float) (currentIndex) / (float) (base)));
        }

        return result;
    }

    public static int ffxFsr2GetJitterPhaseCount(float renderWidth, float displayWidth) {
        float basePhaseCount = 8.0f;
        float jitterPhaseCount = (float) (basePhaseCount * pow((displayWidth / renderWidth), 2.0f));
        return (int) jitterPhaseCount;
    }

    public static Vector2f ffxFsr2GetJitterOffset(int index, int phaseCount) {if (!(phaseCount > 0)) {
            throw new RuntimeException();
        }

        float x = halton((index % phaseCount) + 1, 2) - 0.5f;
        float y = halton((index % phaseCount) + 1, 3) - 0.5f;

        //if (index % 4 == 0){
        //    x = 1;
        //    y = 1;
        //}
        //if (index % 4 == 1){
        //    x = 0;
        //    y = 0;
        //}
        //if (index % 4 == 2){
        //    x = 1;
        //    y = 1;
        //}
        //if (index % 4 == 3){
        //    x = 1;
        //    y = 1;
        //}

        return new Vector2f(x , y);
    }
}
