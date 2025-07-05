package io.homo.superresolution.api.config.values;

import java.util.List;
import java.util.function.Supplier;

public abstract class ConfigValue<T> {
    protected final List<String> path;
    protected final Supplier<T> defaultSupplier;
    protected final String comment;
    protected T value;
    protected boolean valueSet = false;

    public ConfigValue(List<String> path, Supplier<T> defaultSupplier, String comment) {
        this.path = path;
        this.defaultSupplier = defaultSupplier;
        this.comment = comment;
        this.value = defaultSupplier.get();
    }

    public T get() {
        return value;
    }

    public void set(Object value) {
        if (isValid(value)) {
            this.value = convertType(value);
            this.valueSet = true;
        } else {
            throw new IllegalArgumentException("Invalid value for config path " + path + ": " + value);
        }
    }

    public void resetToDefault() {
        this.value = defaultSupplier.get();
        this.valueSet = false;
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

    public abstract boolean isValid(Object value);

    protected abstract T convertType(Object value);
}
