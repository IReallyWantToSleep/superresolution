package io.homo.superresolution.api.config.values.single;

import com.electronwill.nightconfig.core.ConfigSpec;
import io.homo.superresolution.api.config.ConfigValue;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class EnumValue<T extends Enum<T>> extends ConfigValue<T> {
    private final Class<T> enumClass;

    public EnumValue(List<String> path, Supplier<T> defaultSupplier, Class<T> enumClass, String comment) {
        super(path, defaultSupplier, comment);
        this.enumClass = enumClass;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) return false;
        if (value instanceof String) {
            try {
                Enum.valueOf(enumClass, (String) value);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return enumClass.isInstance(value);
    }

    @Override
    protected void fillSpec(ConfigSpec spec) {
        spec.define(
                path,
                defaultSupplier,
                (Object obj) -> isValid(convertType(obj))
        );
    }

    @Override
    protected T convertType(Object value) {
        if (enumClass.isInstance(value)) return enumClass.cast(value);
        if (value instanceof String)
            return Enum.valueOf(enumClass, (String) value);
        return null;
    }
}
