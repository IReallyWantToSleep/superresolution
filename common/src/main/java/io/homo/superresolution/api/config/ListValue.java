package io.homo.superresolution.api.config;

import com.electronwill.nightconfig.core.ConfigSpec;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ListValue<E> extends ConfigValue<List<E>> {
    private final Function<Object, E> elementConverter;
    protected final Predicate<E> elementValidator;

    public ListValue(
            List<String> path,
            Supplier<List<E>> defaultSupplier,
            String comment,
            Function<Object, E> elementConverter,
            Predicate<E> elementValidator
    ) {
        super(path, defaultSupplier, comment);
        this.elementConverter = elementConverter;
        this.elementValidator = elementValidator;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) return false;
        if (!(value instanceof List<?> list)) return false;

        for (Object element : list) {
            if (element == null) return false;
            try {
                E converted = elementConverter.apply(element);
                if (converted == null || !elementValidator.test(converted)) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    protected void fillSpec(ConfigSpec spec) {
        spec.defineList(
                path,
                defaultSupplier::get,
                (Object obj) -> elementValidator.test((E) obj)
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<E> convertType(Object value) {
        if (value == null) return getDefault();
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .filter(e -> e != null)
                    .map(elementConverter)
                    .filter(elementValidator)
                    .collect(Collectors.toList());
        }
        return getDefault();
    }
}
