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

import java.util.ArrayList;
import java.util.List;

public class OptionCategory {
    private Text name;

    public OptionCategory(Text name) {
        this.name = name;
    }

    public List<AbstractOptionEntry<?, ?, ?>> getEntries() {
        return entries;
    }

    protected List<AbstractOptionEntry<?, ?, ?>> entries = new ArrayList<>();

    public Text getCategoryName() {
        return name;
    }

    public OptionCategory addEntry(AbstractOptionEntry<?, ?, ?> entry) {
        entries.add(entry);
        return this;
    }
}
