package io.homo.superresolution.common.config.special;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class SpecialConfigDescription<T> {
    protected String key;
    protected ConfigType type;
    protected T value;
    protected T defaultValue;
    protected Component name = Component.empty();
    protected Component tooltip = null;
    protected Class<? extends Enum<?>> clazz = null;
    protected Pair<Float, Float> valueRange = null;
    protected Consumer<T> saveConsumer;


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
        return name;
    }

    public SpecialConfigDescription<T> setName(Component name) {
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

    public ConfigType getType() {
        return type;
    }

    public SpecialConfigDescription<T> setType(ConfigType type) {
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

    public enum ConfigType {
        ENUM, FLOAT, STRING, BOOLEAN
    }
}
