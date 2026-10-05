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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class EnumSelectorBuilder<T extends Enum<T>> extends SelectionListBuilder<T, EnumSelectorBuilder<T>> {
    private final Class<T> clazz;

    public EnumSelectorBuilder(Text fieldName, Class<T> clazz, T value) {
        super(fieldName, value, clazz.getEnumConstants());
        Objects.requireNonNull(clazz, "Enum class must not be null");
        Objects.requireNonNull(value, "Enum value must not be null");
        this.clazz = clazz;
        setNameProvider(t -> t.name());
    }

    public EnumSelectorBuilder<T> setEnumNameProvider(@NotNull Function<Enum<T>, String> enumNameProvider) {
        Objects.requireNonNull(enumNameProvider, "Enum name provider must not be null");
        setNameProvider(t -> enumNameProvider.apply(t));
        return this;
    }

    public EnumSelectorBuilder<T> setDefaultValue(@NotNull T defaultValue) {
        Objects.requireNonNull(defaultValue, "Default value must not be null");
        this.defaultValue = () -> defaultValue;
        return this;
    }

    @Override
    public EnumListEntry<T> build() {
        EnumListEntry<T> entry = new EnumListEntry<>(
                this.name,
                this.value,
                this.values,
                this.nameProvider
        );
        entry.setItemEnableRequirement(itemEnableRequirement);
        entry.setValuesSupplier(valuesSupplier);
        entry.setMenuItemTooltip(menuItemTooltipSupplier);
        return (EnumListEntry<T>) finishBuild(entry);
    }

    @Override
    public EnumSelectorBuilder<T> setDefaultValue(@Nullable Supplier<T> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }
}