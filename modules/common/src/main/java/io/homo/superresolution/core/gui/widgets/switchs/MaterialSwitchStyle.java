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

package io.homo.superresolution.core.gui.widgets.switchs;

import io.homo.superresolution.core.gui.core.WidgetStyle;

public class MaterialSwitchStyle extends WidgetStyle<MaterialSwitchStyle> {
    private boolean showCheckedIconWhenEnable;
    private boolean showCheckedIconAlways;

    private boolean showUncheckedIconWhenEnable;
    private boolean showUncheckedIconAlways;

    public boolean showCheckedIconWhenEnable() {
        return showCheckedIconWhenEnable;
    }

    public MaterialSwitchStyle showCheckedIconWhenEnable(boolean showCheckedIconWhenEnable) {
        this.showCheckedIconWhenEnable = showCheckedIconWhenEnable;
        return this;
    }

    public boolean showCheckedIconAlways() {
        return showCheckedIconAlways;
    }

    public MaterialSwitchStyle showCheckedIconAlways(boolean showCheckedIconAlways) {
        this.showCheckedIconAlways = showCheckedIconAlways;
        return this;
    }

    public boolean showUncheckedIconWhenEnable() {
        return showUncheckedIconWhenEnable;
    }

    public MaterialSwitchStyle showUncheckedIconWhenEnable(boolean showUncheckedIconWhenEnable) {
        this.showUncheckedIconWhenEnable = showUncheckedIconWhenEnable;
        return this;
    }

    public boolean showUncheckedIconAlways() {
        return showUncheckedIconAlways;
    }

    public MaterialSwitchStyle showUncheckedIconAlways(boolean showUncheckedIconAlways) {
        this.showUncheckedIconAlways = showUncheckedIconAlways;
        return this;
    }
}
