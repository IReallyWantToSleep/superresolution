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

package io.homo.superresolution.api.config.values.list;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ListValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StringListValue extends ListValue<String> {
    public StringListValue(
            List<String> path,
            Supplier<List<String>> defaultSupplier,
            String comment,
            Predicate<String> elementValidator
    ) {
        super(
                path,
                defaultSupplier,
                comment,
                obj -> {
                    if (obj == null) return null;

                    return obj.toString();
                },
                elementValidator
        );
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.defineList(
                path,
                defaultSupplier::get,
                (Object obj) -> elementValidator.test((String) obj)
        );
    }
}
