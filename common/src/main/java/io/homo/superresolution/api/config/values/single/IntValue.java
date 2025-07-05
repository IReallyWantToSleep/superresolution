package io.homo.superresolution.api.config.values.single;

import io.homo.superresolution.api.config.values.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class IntValue extends ConfigValue<Integer> {
    private final Predicate<Integer> validator;

    public IntValue(List<String> path, Supplier<Integer> defaultSupplier, String comment, Predicate<Integer> validator) {
        super(path, defaultSupplier, comment);
        this.validator = validator;
    }

    @Override
    public boolean isValid(Object value) {
        if (value instanceof Number) {
            return validator.test(((Number) value).intValue());
        }
        return false;
    }

    @Override
    protected Integer convertType(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) return Integer.parseInt((String) value);
        throw new IllegalArgumentException("Cannot convert " + value + " to Integer");
    }
}
