package io.homo.superresolution.common.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EnumSerializer<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    private final Map<String, T> nameToValueMap;
    private final Map<T, String> valueToNameMap;
    private final T defaultValue;
    private final String defaultName;

    private EnumSerializer(Map<String, T> nameToValueMap, T defaultValue) {
        this.nameToValueMap = Map.copyOf(nameToValueMap);
        this.valueToNameMap = createReverseMap(nameToValueMap);
        this.defaultValue = Objects.requireNonNull(defaultValue, "Default value cannot be null");
        this.defaultName = validateDefaultValue(nameToValueMap, defaultValue);
    }

    private Map<T, String> createReverseMap(Map<String, T> forwardMap) {
        Map<T, String> reverseMap = new HashMap<>();
        forwardMap.forEach((name, value) -> {
            if (reverseMap.containsKey(value)) {
                throw new IllegalArgumentException(
                        String.format("Duplicate value detected: %s -> %s and %s -> %s",
                                value, reverseMap.get(value), value, name)
                );
            }
            reverseMap.put(value, name);
        });
        return Collections.unmodifiableMap(reverseMap);
    }

    private String validateDefaultValue(Map<String, T> map, T defaultValue) {
        String name = valueToNameMap.get(defaultValue);
        if (name == null) {
            throw new IllegalArgumentException("Default value not found in enum mapping");
        }
        return name;
    }

    @Override
    public JsonElement serialize(T value, Type type, JsonSerializationContext context) {
        String name = valueToNameMap.getOrDefault(value, defaultName);
        return new JsonPrimitive(name);
    }

    @Override
    public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        return nameToValueMap.getOrDefault(json.getAsString(), defaultValue);
    }

    public static class Builder<T> {
        private final Map<String, T> mapping = new HashMap<>();
        private T defaultValue;

        public Builder<T> addMapping(String name, T value) {
            if (mapping.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate mapping name: " + name);
            }
            mapping.put(name, Objects.requireNonNull(value, "Mapped value cannot be null"));
            return this;
        }

        public Builder<T> setDefault(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public EnumSerializer<T> build() {
            if (mapping.isEmpty()) {
                throw new IllegalStateException("At least one enum mapping is required");
            }
            if (defaultValue == null) {
                throw new IllegalStateException("Default value must be specified");
            }
            if (!mapping.containsValue(defaultValue)) {
                throw new IllegalStateException("Default value must exist in mappings");
            }
            return new EnumSerializer<>(mapping, defaultValue);
        }
    }
}