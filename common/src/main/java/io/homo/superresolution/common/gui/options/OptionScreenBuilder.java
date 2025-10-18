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
import io.homo.superresolution.common.gui.screens.MaterialStyleConfigScreen;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class OptionScreenBuilder {
    protected HashMap<Text, OptionCategory> categories = new HashMap<>();

    public Text getTitle() {
        return title;
    }

    public OptionScreenBuilder setTitle(Text title) {
        this.title = title;
        return this;
    }

    protected Text title = Text.empty();

    public OptionCategory getOrCreateCategory(Text name) {
        if (categories.containsKey(name)) {
            OptionCategory category = categories.get(name);
            if (category != null) {
                return category;
            }
        }
        OptionCategory category = new OptionCategory(name);
        categories.put(name, category);
        return category;
    }

    public OptionBuilder getOptionBuilder(@NotNull OptionCategory category) {
        return new OptionBuilder(category);
    }

    public OptionBuilder getOptionBuilder(@NotNull Text name) {
        return new OptionBuilder(getOrCreateCategory(name));
    }

    public MaterialStyleConfigScreen build() {
        return new MaterialStyleConfigScreen(title, categories.values().stream().toList());
    }
}
