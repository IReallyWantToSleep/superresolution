package io.homo.superresolution.api.config.values.list;

import io.homo.superresolution.api.config.values.ListValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StringListValue extends ListValue<String> {
    public StringListValue(
            List<String> path,
            Supplier<List<String>> defaultSupplier,
            String comment,
            Predicate<String> elementValidator
    ) {
        super(
                path,
                defaultSupplier,
                comment,
                obj -> obj.toString(),
                elementValidator
        );
    }
}
