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
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class SelectionListBuilder<T, SELF extends SelectionListBuilder<T, SELF>>
        extends AbstractOptionBuilder<T, SelectionListOptionEntry<T>, SELF> {
    protected ImmutableList<T> values;
    protected Function<T, String> nameProvider;
    protected @Nullable Function<T, OptionRequirement> itemEnableRequirement = null;
    protected @Nullable Supplier<List<T>> valuesSupplier = null;
    protected @Nullable Function<T, Optional<Tooltip>> menuItemTooltipSupplier = null;

    public SelectionListBuilder(Text name, T value, T[] valuesArray) {
        super(name, value);
        this.values = ImmutableList.copyOf(valuesArray);
    }

    public @Nullable Function<T, Optional<Tooltip>> getMenuItemTooltipSupplier() {
        return menuItemTooltipSupplier;
    }

    @SuppressWarnings("unchecked")
    public SELF setMenuItemTooltipSupplier(@Nullable Function<T, Optional<Tooltip>> menuItemTooltipSupplier) {
        this.menuItemTooltipSupplier = menuItemTooltipSupplier;
        return (SELF) this;
    }

    public @Nullable Function<T, OptionRequirement> getItemEnableRequirement() {
        return itemEnableRequirement;
    }

    @SuppressWarnings("unchecked")
    public SELF setItemEnableRequirement(@Nullable Function<T, OptionRequirement> itemEnableRequirement) {
        this.itemEnableRequirement = itemEnableRequirement;
        return (SELF) this;
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
        entry.setValuesSupplier(valuesSupplier);
        entry.setMenuItemTooltip(menuItemTooltipSupplier);
        return finishBuild(entry);
    }

    @SuppressWarnings("unchecked")
    public SELF setValues(T[] valuesArray) {
        this.values = ImmutableList.copyOf(valuesArray);
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public SELF setNameProvider(@Nullable Function<T, String> nameProvider) {
        this.nameProvider = nameProvider != null ? nameProvider : t -> t.toString();
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public SELF setValuesSupplier(@Nullable Supplier<List<T>> valuesSupplier) {
        this.valuesSupplier = valuesSupplier;
        return (SELF) this;
    }
}