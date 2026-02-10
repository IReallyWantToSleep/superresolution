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

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class SelectionListBuilder<T> extends AbstractOptionBuilder<T, SelectionListOptionEntry<T>, SelectionListBuilder<T>> {
    protected ImmutableList<T> values;
    protected Function<T, String> nameProvider;
    protected @Nullable Function<T, OptionRequirement> itemEnableRequirement = null;

    public SelectionListBuilder(Text name, T value, T[] valuesArray) {
        super(name, value);
        this.values = ImmutableList.copyOf(valuesArray);
    }

    public @Nullable Function<T, OptionRequirement> getItemEnableRequirement() {
        return itemEnableRequirement;
    }

    public SelectionListBuilder<T> setItemEnableRequirement(@Nullable Function<T, OptionRequirement> itemEnableRequirement) {
        this.itemEnableRequirement = itemEnableRequirement;
        return this;
    }

    @Override
    public SelectionListOptionEntry<T> build() {
        SelectionListOptionEntry<T> entry = new SelectionListOptionEntry<>(
                this.name,
                this.value,
                this.values,
                nameProvider
        );
        entry.setItemEnableRequirement(itemEnableRequirement);
        return finishBuild(entry);
    }

    public SelectionListBuilder<T> setValues(T[] valuesArray) {
        this.values = ImmutableList.copyOf(valuesArray);
        return this;
    }

    public SelectionListBuilder<T> setNameProvider(@Nullable Function<T, String> nameProvider) {
        this.nameProvider = nameProvider != null ? nameProvider : t -> t.toString();
        return this;
    }
}