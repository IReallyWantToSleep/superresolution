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
import io.homo.superresolution.common.gui.impl.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class EnumSelectorBuilder<T extends Enum<T>> extends AbstractOptionBuilder<T, SelectionListOptionEntry<T>, EnumSelectorBuilder<T>> {
    private final Class<T> clazz;
    private Function<Enum<T>, String> enumNameProvider;

    public EnumSelectorBuilder(Text fieldName, Class<T> clazz, T value) {
        super(fieldName, value);
        Objects.requireNonNull(clazz, "Enum class must not be null");
        Objects.requireNonNull(value, "Enum value must not be null");
        this.clazz = clazz;
        this.enumNameProvider = t -> t.name();
    }

    public EnumSelectorBuilder<T> setEnumNameProvider(@NotNull Function<Enum<T>, String> enumNameProvider) {
        Objects.requireNonNull(enumNameProvider, "Enum name provider must not be null");
        this.enumNameProvider = enumNameProvider;
        return this;
    }

    @Override
    public EnumSelectorBuilder<T> setDefaultValue(@Nullable Supplier<T> defaultValue) {
        this.defaultValue = defaultValue;
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
                ImmutableList.copyOf(this.clazz.getEnumConstants()),
                (Function<T, String>) enumNameProvider
        );
        entry.setTooltipSupplier(v -> tooltipSupplier.apply(v));
        if (errorSupplier != null) {
            entry.setErrorSupplier(v -> errorSupplier.apply(v));
        }
        return (EnumListEntry<T>) finishBuild(entry);
    }
}