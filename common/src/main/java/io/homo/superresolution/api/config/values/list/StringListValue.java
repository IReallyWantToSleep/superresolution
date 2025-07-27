package io.homo.superresolution.api.config.values.list;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ListValue;

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
                obj -> {
                    if (obj == null) return null;

                    return obj.toString();
                },
                elementValidator
        );
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.defineList(
                path,
                defaultSupplier::get,
                (Object obj) -> elementValidator.test((String) obj)
        );
    }
}
