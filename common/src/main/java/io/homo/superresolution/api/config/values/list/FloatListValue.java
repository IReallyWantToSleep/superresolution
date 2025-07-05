package io.homo.superresolution.api.config.values.list;

import io.homo.superresolution.api.config.values.ListValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class FloatListValue extends ListValue<Float> {
    public FloatListValue(
            List<String> path,
            Supplier<List<Float>> defaultSupplier,
            String comment,
            Predicate<Float> elementValidator
    ) {
        super(
                path,
                defaultSupplier,
                comment,
                obj -> {
                    if (obj instanceof Number) return ((Number) obj).floatValue();
                    if (obj instanceof String) return Float.parseFloat((String) obj);
                    throw new IllegalArgumentException("Cannot convert to Float: " + obj);
                },
                elementValidator
        );
    }
}
