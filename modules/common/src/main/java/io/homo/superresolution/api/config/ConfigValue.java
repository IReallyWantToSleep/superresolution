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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public abstract class ConfigValue<T> {
    protected final List<String> path;
    protected final Supplier<T> defaultSupplier;
    protected final String comment;
    protected final BiPredicate<T, T> equalityChecker;
    protected final List<BiConsumer<T, T>> changeListeners = new ArrayList<>();
    protected ModConfigSpec configSpec;

    public ConfigValue(List<String> path, Supplier<T> defaultSupplier, String comment) {
        this(path, defaultSupplier, comment, Objects::equals);
    }

    public ConfigValue(List<String> path, Supplier<T> defaultSupplier, String comment, BiPredicate<T, T> equalityChecker) {
        this.path = path;
        this.defaultSupplier = defaultSupplier;
        this.comment = comment;
        this.equalityChecker = equalityChecker;
    }

    public T get() {
        return convertType(configSpec.configData.getOrElse(path, defaultSupplier));
    }

    public void set(Object value) {
        if (isValid(value)) {
            T oldValue = get();
            configSpec.configData.set(path, value);
            T newValue = get();

            if (!equalityChecker.test(oldValue, newValue)) {
                notifyListeners(oldValue, newValue);
            }
        } else {
            throw new IllegalArgumentException("Invalid value for config path " + path + ": " + value);
        }
    }

    public T getDefault() {
        return defaultSupplier.get();
    }

    public List<String> getPath() {
        return path;
    }

    public String getComment() {
        return comment;
    }

    public ConfigValue<T> onChange(BiConsumer<T, T> listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
        return this;
    }

    public ConfigValue<T> removeChangeListener(BiConsumer<T, T> listener) {
        changeListeners.remove(listener);
        return this;
    }

    public void clearChangeListeners() {
        changeListeners.clear();
    }

    protected void notifyListeners(T oldValue, T newValue) {
        for (BiConsumer<T, T> listener : changeListeners) {
            try {
                listener.accept(oldValue, newValue);
            } catch (Exception e) {
                ModConfigSpec.LOGGER.error("Error notifying config change listener for path {}", path, e);
            }
        }
    }

    public abstract boolean isValid(Object value);

    protected abstract void fillSpec(ConfigSpec spec);

    protected abstract T convertType(Object value);
}
