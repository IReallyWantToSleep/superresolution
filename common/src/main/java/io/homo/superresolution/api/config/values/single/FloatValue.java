package io.homo.superresolution.api.config.values.single;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FloatValue extends ConfigValue<Float> {
    private final Predicate<Float> validator;

    public FloatValue(List<String> path, Supplier<Float> defaultSupplier, String comment, Predicate<Float> validator) {
        super(path, defaultSupplier, comment);
        this.validator = (obj) -> obj != null && validator.test(obj);
    }

    @Override
    public boolean isValid(Object value) {
        if (value instanceof Number) {
            return validator.test(((Number) value).floatValue());
        }
        return false;
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.define(
                path,
                defaultSupplier,
                (Object obj) -> validator.test((Float) obj)
        );
    }

    @Override
    protected Float convertType(Object value) {
        if (value instanceof Float) return (Float) value;
        if (value instanceof Number) return ((Number) value).floatValue();
        if (value instanceof String) return Float.parseFloat((String) value);
        throw new IllegalArgumentException("Cannot convert " + value + " to Float");
    }
}
