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

package io.homo.superresolution.common.gui.impl;

import net.minecraft.locale.Language;

public class Text {
    protected String value;
    protected boolean translatable;

    protected Text(String value, boolean translatable) {
        this.value = value;
        this.translatable = translatable;
    }

    public static Text empty() {
        return new Text("", false);
    }

    public static Text literal(String value) {
        return new Text(value, false);
    }

    public static Text translatable(String key) {
        return new Text(key, true);
    }

    public String getString() {
        return translatable ? Language.getInstance().getOrDefault(value) : value;
    }

    @Override
    public String toString() {
        return getString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Text && ((Text) obj).translatable == translatable && value.equals(((Text) obj).value);
    }
}
