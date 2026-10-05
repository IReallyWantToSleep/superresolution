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
