package io.homo.superresolution.api.config.values.single;

import io.homo.superresolution.api.config.values.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LongValue extends ConfigValue<Long> {
    private final Predicate<Long> validator;

    public LongValue(List<String> path, Supplier<Long> defaultSupplier, String comment, Predicate<Long> validator) {
        super(path, defaultSupplier, comment);
        this.validator = validator;
    }

    @Override
    public boolean isValid(Object value) {
        if (value instanceof Number) {
            return validator.test(((Number) value).longValue());
        }
        return false;
    }

    @Override
    protected Long convertType(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        throw new IllegalArgumentException("Cannot convert " + value + " to Long");
    }
}
