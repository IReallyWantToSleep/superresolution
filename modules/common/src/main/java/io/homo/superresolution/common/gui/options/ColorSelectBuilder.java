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

package io.homo.superresolution.common.gui.options;

import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.utils.Color;

import java.util.function.Consumer;

public class ColorSelectBuilder extends AbstractOptionBuilder<Color, ColorSelectOptionEntry, ColorSelectBuilder> {
    protected Color value;
    protected Consumer<Color> valueChangeListener;

    public ColorSelectBuilder(Text name, Color value) {
        super(name, value);
        this.value = value;
    }

    public Consumer<Color> getValueChangeListener() {
        return valueChangeListener;
    }

    public ColorSelectBuilder setValueChangeListener(Consumer<Color> valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
        return this;
    }

    public Color getValue() {
        return value;
    }

    @Override
    public ColorSelectBuilder setValue(Color value) {
        this.value = value;
        return this;
    }


    @Override
    public ColorSelectOptionEntry build() {
        ColorSelectOptionEntry entry = new ColorSelectOptionEntry(
                this.name,
                this.value
        );
        entry.valueChangeListener = this.valueChangeListener;
        return finishBuild(entry);
    }
}