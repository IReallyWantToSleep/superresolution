/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.gui.widgets.sliders;

import io.homo.superresolution.core.gui.core.WidgetStyle;

public class MaterialSliderStyle extends WidgetStyle<MaterialSliderStyle> {
    private boolean steps;

    public MaterialSliderSize size() {
        return size;
    }

    public MaterialSliderStyle size(MaterialSliderSize size) {
        this.size = size;
        return this;
    }

    private MaterialSliderSize size = MaterialSliderSize.Medium;

    public boolean valueIndicator() {
        return valueIndicator;
    }

    public MaterialSliderStyle valueIndicator(boolean valueIndicator) {
        this.valueIndicator = valueIndicator;
        return this;
    }

    private boolean valueIndicator;


    public boolean steps() {
        return steps;
    }

    public MaterialSliderStyle steps(boolean steps) {
        this.steps = steps;
        return this;
    }
}
