package io.homo.superresolution.api.config.values.single;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StringValue extends ConfigValue<String> {
    private final Predicate<String> validator;

    public StringValue(List<String> path, Supplier<String> defaultSupplier, String comment, Predicate<String> validator) {
        super(path, defaultSupplier, comment);
        this.validator = (obj) -> obj != null && validator.test(obj);
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) return false;
        return value instanceof String && validator.test((String) value);
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.define(
                path,
                defaultSupplier,
                (Object obj) -> validator.test(convertType(obj))
        );
    }

    @Override
    protected String convertType(Object value) {
        if (value == null) return null;
        return value.toString();
    }
}
