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

package io.homo.superresolution.common.config.special;

import io.homo.superresolution.common.config.ConfigSpecType;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class SpecialConfigDescription<T> {
    protected String key;
    protected ConfigSpecType type;
    protected T value;
    protected T defaultValue;

    protected Function<T, Optional<Component>> valueName = (a) -> Optional.of(Component.empty());
    protected Function<T, Optional<Component>> name = (a) -> Optional.of(Component.empty());
    protected Function<T, Optional<Component>> tooltipSupplier = (a) -> Optional.empty();

    protected Class<? extends Enum<?>> clazz = null;
    protected Pair<Float, Float> valueRange = null;
    protected Consumer<T> saveConsumer;
    protected boolean valueNameIsSupplier = false;

    public static <T> SpecialConfigDescription<T> of(String key, ConfigSpecType type, T defaultValue) {
        return new SpecialConfigDescription<T>()
                .setKey(key)
                .setType(type)
                .setDefaultValue(defaultValue)
                .setValue(defaultValue);
    }

    public boolean isValueNameIsSupplier() {
        return valueNameIsSupplier;
    }

    public Consumer<T> getSaveConsumer() {
        return saveConsumer;
    }

    public SpecialConfigDescription<T> setSaveConsumer(Consumer<T> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public SpecialConfigDescription<T> setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Optional<Component> getTooltip() {
        return tooltipSupplier.apply(getValue());
    }

    public SpecialConfigDescription<T> setTooltip(Function<T, Optional<Component>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public SpecialConfigDescription<T> setTooltip(Component tooltip) {
        this.tooltipSupplier = (a) -> Optional.ofNullable(tooltip);
        return this;
    }

    public SpecialConfigDescription<T> setName(Function<T, Optional<Component>> name) {
        this.name = name;
        return this;
    }

    public SpecialConfigDescription<T> setName(Component name) {
        this.name = (a) -> Optional.of(name);
        return this;
    }

    public Pair<Float, Float> getValueRange() {
        return valueRange;
    }

    public SpecialConfigDescription<T> setValueRange(Pair<Float, Float> valueRange) {
        this.valueRange = valueRange;
        return this;
    }

    public Class<? extends Enum<?>> getClazz() {
        return clazz;
    }

    public SpecialConfigDescription<T> setClazz(Class<? extends Enum<?>> clazz) {
        this.clazz = clazz;
        return this;
    }

    public Component getName() {
        return name.apply(getValue()).orElse(Component.empty());
    }

    public Component getValueName() {
        return valueName.apply(getValue()).orElse(Component.empty());
    }

    public SpecialConfigDescription<T> setValueName(Component valueName) {
        valueNameIsSupplier = false;
        this.valueName = (a) -> Optional.of(valueName);
        return this;
    }

    public SpecialConfigDescription<T> setValueNameSupplier(Function<T, Optional<Component>> valueNameSupplier) {
        valueNameIsSupplier = true;
        this.valueName = valueNameSupplier;
        return this;
    }

    public Function<T, Optional<Component>> getValueNameSupplier() {
        return valueName;
    }

    @SuppressWarnings("unchecked")
    public Function<Object, Optional<Component>> getValueNameSupplierAsObject() {
        return (Function<Object, Optional<Component>>) valueName;
    }

    @SuppressWarnings("unchecked")
    public Consumer<Object> getSaveConsumerAsObject() {
        return (Consumer<Object>) saveConsumer;
    }


    public String getKey() {
        return key;
    }

    public SpecialConfigDescription<T> setKey(String key) {
        this.key = key;
        return this;
    }

    public ConfigSpecType getType() {
        return type;
    }

    public SpecialConfigDescription<T> setType(ConfigSpecType type) {
        this.type = type;
        return this;
    }

    public T getValue() {
        return value;
    }

    public SpecialConfigDescription<T> setValue(T value) {
        this.value = value;
        return this;
    }
}
