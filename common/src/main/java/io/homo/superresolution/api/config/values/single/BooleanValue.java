package io.homo.superresolution.api.config.values.single;

import io.homo.superresolution.api.config.values.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BooleanValue extends ConfigValue<Boolean> {
    private final Predicate<Boolean> validator;

    public BooleanValue(List<String> path, Supplier<Boolean> defaultSupplier, String comment, Predicate<Boolean> validator) {
        super(path, defaultSupplier, comment);
        this.validator = validator;
    }

    @Override
    public boolean isValid(Object value) {
        if (value instanceof Boolean) {
            return validator.test((Boolean) value);
        }
        return false;
    }

    @Override
    protected Boolean convertType(Object value) {
        if (value instanceof Integer) return ((Integer) value) == 1;
        if (value instanceof Number) return ((Number) value).intValue() == 1;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        if (value instanceof Boolean) return (Boolean) value;
        throw new IllegalArgumentException("Cannot convert " + value + " to Boolean");
    }
}
