package io.homo.superresolution.api.config;

import com.electronwill.nightconfig.core.ConfigSpec;

import java.util.List;
import java.util.function.Supplier;

public abstract class ConfigValue<T> {
    protected final List<String> path;
    protected final Supplier<T> defaultSupplier;
    protected final String comment;
    protected ModConfigSpec configSpec;

    public ConfigValue(List<String> path, Supplier<T> defaultSupplier, String comment) {
        this.path = path;
        this.defaultSupplier = defaultSupplier;
        this.comment = comment;
    }

    public T get() {
        return convertType(configSpec.configData.getOrElse(path, defaultSupplier));
    }

    public void set(Object value) {
        if (isValid(value)) {
            configSpec.configData.set(path, value);
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

    public abstract boolean isValid(Object value);

    protected abstract void fillSpec(ConfigSpec spec);

    protected abstract T convertType(Object value);
}
