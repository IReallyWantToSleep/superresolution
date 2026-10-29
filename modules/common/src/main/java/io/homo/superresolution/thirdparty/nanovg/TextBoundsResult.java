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
