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

package io.homo.superresolution.api.config.values.single;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ConfigValue;

import java.util.List;
import java.util.function.Supplier;

public class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
    private final Class<T> enumClass;

    public EnumValue(List<String> path, Supplier<T> defaultSupplier, Class<T> enumClass, String comment) {
        super(path, defaultSupplier, comment);
        this.enumClass = enumClass;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) return false;
        if (value instanceof String) {
            try {
                Enum.valueOf(enumClass, (String) value);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return enumClass.isInstance(value);
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.define(
                path,
                defaultSupplier,
                (Object obj) -> isValid(convertType(obj))
        );
    }

    @Override
    protected T convertType(Object value) {
        if (enumClass.isInstance(value)) return enumClass.cast(value);
        if (value instanceof String)
            return Enum.valueOf(enumClass, (String) value);
        return null;
    }
}
