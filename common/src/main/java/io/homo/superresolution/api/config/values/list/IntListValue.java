package io.homo.superresolution.api.config.values.list;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ListValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class IntListValue extends ListValue<Integer> {
    public IntListValue(
            List<String> path,
            Supplier<List<Integer>> defaultSupplier,
            String comment,
            Predicate<Integer> elementValidator
    ) {
        super(
                path,
                defaultSupplier,
                comment,
                obj -> {
                    if (obj == null) return null;

                    if (obj instanceof Number) return ((Number) obj).intValue();
                    if (obj instanceof String) return Integer.parseInt((String) obj);
                    throw new IllegalArgumentException("Cannot convert to Integer: " + obj);
                },
                elementValidator
        );
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.defineList(
                path,
                defaultSupplier::get,
                (Object obj) -> elementValidator.test((Integer) obj)
        );
    }
}
