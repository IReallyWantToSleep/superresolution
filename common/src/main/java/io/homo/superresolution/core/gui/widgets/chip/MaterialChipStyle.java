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

package io.homo.superresolution.core.gui.widgets.chip;

import io.homo.superresolution.core.gui.core.WidgetStyle;

public class MaterialChipStyle extends WidgetStyle<MaterialChipStyle> {
    private MaterialChipType type = MaterialChipType.Assist;
    private boolean elevated = false;

    public MaterialChipType type() {
        return type;
    }

    public MaterialChipStyle type(MaterialChipType type) {
        this.type = type == null ? MaterialChipType.Assist : type;
        return this;
    }

    public boolean elevated() {
        return elevated;
    }

    public MaterialChipStyle elevated(boolean elevated) {
        this.elevated = elevated;
        return this;
    }
}
