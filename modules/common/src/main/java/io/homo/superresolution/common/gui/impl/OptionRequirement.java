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

package io.homo.superresolution.common.gui.impl;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FunctionalInterface
public interface OptionRequirement {
    @SafeVarargs
    static <T> OptionRequirement isValue(ValueHolder<T> dependency, @Nullable T firstValue, @Nullable T... otherValues) {
        Set<T> values = Stream.concat(
                Stream.of(firstValue),
                Arrays.stream(otherValues)
        ).collect(Collectors.toCollection(HashSet::new));
        return () -> values.contains(dependency.value());
    }

    static <T> OptionRequirement matches(ValueHolder<T> firstDependency, ValueHolder<T> secondDependency) {
        return () -> Objects.equals(firstDependency.value(), secondDependency.value());
    }

    static OptionRequirement isTrue(ValueHolder<Boolean> dependency) {
        return () -> Boolean.TRUE.equals(dependency.value());
    }

    static OptionRequirement isFalse(ValueHolder<Boolean> dependency) {
        return () -> Boolean.FALSE.equals(dependency.value());
    }

    static OptionRequirement not(OptionRequirement requirement) {
        return () -> !requirement.check();
    }

    static OptionRequirement all(OptionRequirement... requirements) {
        return () -> Arrays.stream(requirements).allMatch(OptionRequirement::check);
    }

    static OptionRequirement any(OptionRequirement... requirements) {
        return () -> Arrays.stream(requirements).anyMatch(OptionRequirement::check);
    }

    static OptionRequirement none(OptionRequirement... requirements) {
        return () -> Arrays.stream(requirements).noneMatch(OptionRequirement::check);
    }

    static OptionRequirement one(OptionRequirement... requirements) {
        return () -> {
            boolean oneFound = false;

            for (OptionRequirement requirement : requirements) {
                if (requirement.check()) {
                    if (oneFound) {
                        return false;
                    }

                    oneFound = true;
                }
            }

            return oneFound;
        };
    }

    boolean check();
}
