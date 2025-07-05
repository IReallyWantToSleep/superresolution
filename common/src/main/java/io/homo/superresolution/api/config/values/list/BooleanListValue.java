package io.homo.superresolution.api.config.values.list;

import io.homo.superresolution.api.config.values.ListValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BooleanListValue extends ListValue<Boolean> {
    public BooleanListValue(
            List<String> path,
            Supplier<List<Boolean>> defaultSupplier,
            String comment,
            Predicate<Boolean> elementValidator
    ) {
        super(
                path,
                defaultSupplier,
                comment,
                obj -> {
                    if (obj instanceof Number) return ((Number) obj).intValue() == 1;
                    if (obj instanceof String) return Boolean.parseBoolean((String) obj);
                    if (obj instanceof Boolean) return (Boolean) obj;
                    throw new IllegalArgumentException("Cannot convert to Boolean: " + obj);
                },
                elementValidator
        );
    }
}
