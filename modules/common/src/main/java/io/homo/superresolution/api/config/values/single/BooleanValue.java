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

package io.homo.superresolution.api.config.values.single;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BooleanValue extends ConfigValue<Boolean> {
    private final Predicate<Boolean> validator;

    public BooleanValue(List<String> path, Supplier<Boolean> defaultSupplier, String comment, Predicate<Boolean> validator) {
        super(path, defaultSupplier, comment);
        this.validator = (obj) -> obj != null && validator.test(obj);
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return validator.test((Boolean) value);
        }
        return false;
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.define(
                path,
                defaultSupplier,
                (Object obj) -> validator.test(convertType(obj))
        );
    }

    @Override
    protected Boolean convertType(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value) == 1;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
