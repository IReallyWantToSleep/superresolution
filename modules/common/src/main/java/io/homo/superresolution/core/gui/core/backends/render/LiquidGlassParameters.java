/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.gui.core.backends.render;

/**
 * Optical controls, independent of {@link LiquidGlassColorControls}.
 *
 * @param lightAngle light direction in degrees, with 0 pointing left and 90 pointing up
 * @param lightIntensity specular opacity in [0, 1]
 * @param refraction nonnegative multiplier of the reference material's displacement factor
 * @param depth glass thickness in GUI pixels, scaled by the current transform
 * @param dispersion channel separation in [0, 1], mapped to the reference shader's [0, 0.5]
 * @param frost background Gaussian sigma in GUI pixels, scaled by the current transform
 */
public record LiquidGlassParameters(float lightAngle, float lightIntensity, float refraction,
                                    float depth, float dispersion, float frost) {
    public static final LiquidGlassParameters DEFAULT =
            new LiquidGlassParameters(45f, 0.8f, 0.8f, 75f, 0.46f, 10f);

    public LiquidGlassParameters {
        if (!Float.isFinite(lightAngle) || !Float.isFinite(lightIntensity)
                || !Float.isFinite(refraction) || !Float.isFinite(depth)
                || !Float.isFinite(dispersion) || !Float.isFinite(frost)) {
            throw new IllegalArgumentException("Liquid glass parameters must be finite");
        }
        if (lightIntensity < 0f || lightIntensity > 1f || dispersion < 0f || dispersion > 1f) {
            throw new IllegalArgumentException("Light intensity and dispersion must be in [0, 1]");
        }
        if (refraction < 0f || depth < 0f || frost < 0f) {
            throw new IllegalArgumentException("Refraction, depth and frost must be nonnegative");
        }
    }
}
