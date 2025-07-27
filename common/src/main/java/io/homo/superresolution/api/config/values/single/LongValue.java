package io.homo.superresolution.api.config.values.single;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LongValue extends ConfigValue<Long> {
    private final Predicate<Long> validator;

    public LongValue(List<String> path, Supplier<Long> defaultSupplier, String comment, Predicate<Long> validator) {
        super(path, defaultSupplier, comment);
        this.validator = (obj) -> obj != null && validator.test(obj);
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) return false;
        if (value instanceof Number) {
            return validator.test(((Number) value).longValue());
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
    protected Long convertType(Object value) {
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }
}
