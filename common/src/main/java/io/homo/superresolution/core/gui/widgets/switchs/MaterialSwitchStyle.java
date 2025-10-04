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

package io.homo.superresolution.core.gui.widgets.switchs;

import io.homo.superresolution.core.gui.core.WidgetStyle;

public class MaterialSwitchStyle extends WidgetStyle<MaterialSwitchStyle> {
    private MaterialSwitchSize size = MaterialSwitchSize.Medium;
    private MaterialSwitchShape shape = MaterialSwitchShape.Round;
    private MaterialSwitchVariant variant = MaterialSwitchVariant.Elevated;

    public MaterialSwitchSize size() {
        return size;
    }

    public MaterialSwitchStyle size(MaterialSwitchSize size) {
        this.size = size;
        return this;
    }

    public MaterialSwitchShape shape() {
        return shape;
    }

    public MaterialSwitchStyle shape(MaterialSwitchShape shape) {
        this.shape = shape;
        return this;
    }

    public MaterialSwitchVariant variant() {
        return variant;
    }

    public MaterialSwitchStyle variant(MaterialSwitchVariant variant) {
        this.variant = variant;
        return this;
    }
}
