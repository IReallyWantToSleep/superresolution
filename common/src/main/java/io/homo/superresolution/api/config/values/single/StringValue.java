package io.homo.superresolution.api.config.values.single;

import io.homo.superresolution.api.config.values.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StringValue extends ConfigValue<String> {
    private final Predicate<String> validator;

    public StringValue(List<String> path, Supplier<String> defaultSupplier, String comment, Predicate<String> validator) {
        super(path, defaultSupplier, comment);
        this.validator = validator;
    }

    @Override
    public boolean isValid(Object value) {
        return value instanceof String && validator.test((String) value);
    }

    @Override
    protected String convertType(Object value) {
        return value.toString();
    }
}
