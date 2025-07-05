package io.homo.superresolution.api.config.values.single;

import io.homo.superresolution.api.config.values.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DoubleValue extends ConfigValue<Double> {
    private final Predicate<Double> validator;

    public DoubleValue(List<String> path, Supplier<Double> defaultSupplier, String comment, Predicate<Double> validator) {
        super(path, defaultSupplier, comment);
        this.validator = validator;
    }

    @Override
    public boolean isValid(Object value) {
        if (value instanceof Number) {
            return validator.test(((Number) value).doubleValue());
        }
        return false;
    }

    @Override
    protected Double convertType(Object value) {
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) return Double.parseDouble((String) value);
        throw new IllegalArgumentException("Cannot convert " + value + " to Double");
    }
}
