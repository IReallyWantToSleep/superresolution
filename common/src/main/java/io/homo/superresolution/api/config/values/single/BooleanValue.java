package io.homo.superresolution.api.config.values.single;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BooleanValue extends ConfigValue<Boolean> {
    private final Predicate<Boolean> validator;

    public BooleanValue(List<String> path, Supplier<Boolean> defaultSupplier, String comment, Predicate<Boolean> validator) {
        super(path, defaultSupplier, comment);
        this.validator = (obj) -> obj != null && validator.test(obj);
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) {
            return validator.test((Boolean) value);
        }
        return false;
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
    protected Boolean convertType(Object value) {
        if (value instanceof Integer) return ((Integer) value) == 1;
        if (value instanceof Number) return ((Number) value).intValue() == 1;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        if (value instanceof Boolean) return (Boolean) value;
        return null;
    }
}
