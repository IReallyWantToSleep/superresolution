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

package io.homo.superresolution.common.upscale.nis.struct;

public class NISConfig {
    public static final int SIZE = 256;
    public float kDetectRatio;
    public float kDetectThres;
    public float kMinContrastRatio;
    public float kRatioNorm;
    public float kContrastBoost;
    public float kEps;
    public float kSharpStartY;
    public float kSharpScaleY;
    public float kSharpStrengthMin;
    public float kSharpStrengthScale;
    public float kSharpLimitMin;
    public float kSharpLimitScale;
    public float kScaleX;
    public float kScaleY;
    public float kDstNormX;
    public float kDstNormY;
    public float kSrcNormX;
    public float kSrcNormY;
    public int kInputViewportOriginX;
    public int kInputViewportOriginY;
    public int kInputViewportWidth;
    public int kInputViewportHeight;
    public int kOutputViewportOriginX;
    public int kOutputViewportOriginY;
    public int kOutputViewportWidth;
    public int kOutputViewportHeight;
    public float reserved0;
    public float reserved1;
}