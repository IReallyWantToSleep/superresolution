package io.homo.superresolution.api.config.values.list;

import io.homo.superresolution.api.config.values.ListValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LongListValue extends ListValue<Long> {
    public LongListValue(
            List<String> path,
            Supplier<List<Long>> defaultSupplier,
            String comment,
            Predicate<Long> elementValidator
    ) {
        super(
                path,
                defaultSupplier,
                comment,
                obj -> {
                    if (obj instanceof Number) return ((Number) obj).longValue();
                    if (obj instanceof String) return Long.parseLong((String) obj);
                    throw new IllegalArgumentException("Cannot convert to Long: " + obj);
                },
                elementValidator
        );
    }
}
