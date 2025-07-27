package io.homo.superresolution.api.config.values.single;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ConfigValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DoubleValue extends ConfigValue<Double> {
    private final Predicate<Double> validator;

    public DoubleValue(List<String> path, Supplier<Double> defaultSupplier, String comment, Predicate<Double> validator) {
        super(path, defaultSupplier, comment);
        this.validator = (obj) -> obj != null && validator.test(obj);
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) return false;
        if (value instanceof Number) {
            return validator.test(((Number) value).doubleValue());
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
    protected Double convertType(Object value) {
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) return Double.parseDouble((String) value);
        return null;
    }
}
