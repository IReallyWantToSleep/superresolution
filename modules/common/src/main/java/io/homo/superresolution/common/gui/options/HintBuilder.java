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
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;

import java.util.function.Supplier;

public class HintBuilder extends AbstractOptionBuilder<Void, HintEntry, HintBuilder> {
    private Supplier<MaterialSymbol> iconSupplier = MaterialSymbols::iconInfo;
    private Supplier<String> titleSupplier = () -> "";
    private Supplier<String> textSupplier = () -> "";

    public HintBuilder(Text name) {
        super(name, null);
    }

    public HintBuilder setIcon(MaterialSymbol icon) {
        this.iconSupplier = () -> icon;
        return this;
    }

    public HintBuilder setIconProvider(Supplier<MaterialSymbol> iconSupplier) {
        this.iconSupplier = iconSupplier;
        return this;
    }

    public HintBuilder setTitle(String title) {
        this.titleSupplier = () -> title;
        return this;
    }

    public HintBuilder setTitleProvider(Supplier<String> titleSupplier) {
        this.titleSupplier = titleSupplier;
        return this;
    }

    public HintBuilder setText(String text) {
        this.textSupplier = () -> text;
        return this;
    }

    public HintBuilder setTextProvider(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier;
        return this;
    }

    @Override
    public HintEntry build() {
        HintEntry entry = new HintEntry(name)
                .setIconProvider(iconSupplier)
                .setTitleProvider(titleSupplier)
                .setTextProvider(textSupplier);
        finishBuild(entry);
        return entry;
    }
}