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

package io.homo.superresolution.common.gui.options;

import io.homo.superresolution.common.gui.impl.Text;

public class OptionBuilder {
    protected OptionCategory category;

    public OptionBuilder(OptionCategory category) {
        this.category = category;
    }

    public <T extends Enum<T>> EnumSelectorBuilder<T> enumSelectorOption(
            Text name,
            Class<T> clazz,
            T value
    ) {
        return new EnumSelectorBuilder<>(name, clazz, value).setCategory(category);
    }

    public <T> SelectionListBuilder<T> selectorOption(
            Text name,
            T value,
            T[] values
    ) {
        return (SelectionListBuilder<T>) new SelectionListBuilder(name, value, values).setCategory(category);
    }

    public BooleanSwitchBuilder booleanOption(
            Text name,
            Boolean value
    ) {
        return new BooleanSwitchBuilder(name, value).setCategory(category);
    }

    public NumberSliderBuilder numberOption(
            Text name,
            Number value,
            Number max,
            Number min
    ) {
        return new NumberSliderBuilder(name, value, max, min).setCategory(category);
    }
}
