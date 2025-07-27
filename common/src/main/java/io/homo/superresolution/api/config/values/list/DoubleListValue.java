package io.homo.superresolution.api.config.values.list;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ListValue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DoubleListValue extends ListValue<Double> {
    public DoubleListValue(
            List<String> path,
            Supplier<List<Double>> defaultSupplier,
            String comment,
            Predicate<Double> elementValidator
    ) {
        super(
                path,
                defaultSupplier,
                comment,
                obj -> {
                    if (obj == null) return null;

                    if (obj instanceof Number) return ((Number) obj).doubleValue();
                    if (obj instanceof String) return Double.parseDouble((String) obj);
                    throw new IllegalArgumentException("Cannot convert to Float: " + obj);
                },
                elementValidator
        );
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.defineList(
                path,
                defaultSupplier::get,
                (Object obj) -> elementValidator.test((Double) obj)
        );
    }
}
