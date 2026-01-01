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

package io.homo.superresolution.api.config;

import com.electronwill.nightconfig.core.ConfigSpec;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ListValue<E> extends ConfigValue<List<E>> {
    private final Function<Object, E> elementConverter;
    protected final Predicate<E> elementValidator;

    public ListValue(
            List<String> path,
            Supplier<List<E>> defaultSupplier,
            String comment,
            Function<Object, E> elementConverter,
            Predicate<E> elementValidator
    ) {
        this(path, defaultSupplier, comment, elementConverter, elementValidator, ListValue::defaultListEquals);
    }

    public ListValue(
            List<String> path,
            Supplier<List<E>> defaultSupplier,
            String comment,
            Function<Object, E> elementConverter,
            Predicate<E> elementValidator,
            BiPredicate<List<E>, List<E>> equalityChecker
    ) {
        super(path, defaultSupplier, comment, equalityChecker);
        this.elementConverter = elementConverter;
        this.elementValidator = elementValidator;
    }

    private static <E> boolean defaultListEquals(List<E> list1, List<E> list2) {
        if (list1 == list2) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            if (!Objects.equals(list1.get(i), list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) return false;
        if (!(value instanceof List<?> list)) return false;

        for (Object element : list) {
            if (element == null) return false;
            try {
                E converted = elementConverter.apply(element);
                if (converted == null || !elementValidator.test(converted)) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    protected void fillSpec(ConfigSpec spec) {
        spec.defineList(
                path,
                defaultSupplier::get,
                (Object obj) -> elementValidator.test((E) obj)
        );
    }

    @Override
    protected List<E> convertType(Object value) {
        if (value == null) return getDefault();
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .filter(e -> e != null)
                    .map(elementConverter)
                    .filter(elementValidator)
                    .collect(Collectors.toList());
        }
        return getDefault();
    }
}
