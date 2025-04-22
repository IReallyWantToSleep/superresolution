package io.homo.superresolution.common.config.special;

import io.homo.superresolution.common.config.ConfigSpecType;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class SpecialConfigDescription<T> {
    protected String key;
    protected ConfigSpecType type;
    protected T value;
    protected T defaultValue;
    protected Function<T, Optional<Component>> name = (a) -> Optional.of(Component.empty());
    protected Component tooltip = null;
    protected Class<? extends Enum<?>> clazz = null;
    protected Pair<Float, Float> valueRange = null;
    protected Consumer<T> saveConsumer;
    protected boolean nameIsSupplier = false;

    public boolean isNameIsSupplier() {
        return nameIsSupplier;
    }

    public Consumer<Object> getSaveConsumer() {
        return (Consumer<Object>) saveConsumer;
    }

    public SpecialConfigDescription<T> setSaveConsumer(Consumer<T> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public Consumer<?> getSaveConsumer_() {
        return saveConsumer;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public SpecialConfigDescription<T> setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Component getTooltip() {
        return tooltip;
    }

    public SpecialConfigDescription<T> setTooltip(Component tooltip) {
        this.tooltip = tooltip;
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

    public SpecialConfigDescription<T> setName(Component name) {
        nameIsSupplier = false;
        this.name = (a) -> Optional.of(name);
        return this;
    }

    public Function<Object, Optional<Component>> getNameSupplier() {
        return (Function<Object, Optional<Component>>) name;
    }

    public SpecialConfigDescription<T> setNameSupplier(Function<T, Optional<Component>> name) {
        nameIsSupplier = true;
        this.name = name;
        return this;
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
