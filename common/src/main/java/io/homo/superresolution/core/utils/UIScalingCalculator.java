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

package io.homo.superresolution.core.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class UIScalingCalculator {

    public static double calculateUIScaling(int displayWidth, int displayHeight, double userZoom) {
        int BASE_WIDTH = 1366;
        int BASE_HEIGHT = 768;
        double MIN_SCALE = 0.5;
        double MAX_SCALE = 2.0;
        double SCALING_EXPONENT = 0.25;
        double baseArea = BASE_WIDTH * BASE_HEIGHT;
        double currentArea = displayWidth * displayHeight;
        if (currentArea <= 0) {
            return clamp(userZoom, MIN_SCALE, MAX_SCALE);
        }
        double areaRatio = currentArea / baseArea;

        double exponent = SCALING_EXPONENT;
        if (areaRatio < 0.5) {
            exponent = 0.25 + (0.5 - areaRatio) * (0.38 - 0.25) / 0.5;
        }

        double areaScaling = Math.pow(areaRatio, exponent);
        double sizeCompensation = calculateSizeCompensation(
                BASE_WIDTH, BASE_HEIGHT,
                displayWidth, displayHeight
        );

        double widthRatio = (double) displayWidth / BASE_WIDTH;
        double heightRatio = (double) displayHeight / BASE_HEIGHT;

        double widthCompensation = 1.0;
        if (widthRatio < 0.5) {
            widthCompensation = Math.pow(widthRatio, 0.15);
        }

        double heightCompensation = 1.0;
        if (heightRatio > 1.5) {
            heightCompensation = Math.pow(1.0 / heightRatio, 0.12);
        }

        BigDecimal bd = BigDecimal.valueOf(clamp(areaScaling * sizeCompensation * widthCompensation * heightCompensation * userZoom, MIN_SCALE, MAX_SCALE));
        return bd.doubleValue();
    }

    private static double calculateSizeCompensation(double baseW, double baseH,
                                                    double currentW, double currentH) {
        double baseLong = Math.max(baseW, baseH);
        double currentLong = Math.max(currentW, currentH);
        double longRatio = baseLong / currentLong;
        return longRatio < 0.7 ? 1.15 : 1.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }
}
