package io.homo.superresolution.common.config;

import com.google.gson.*;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.api.registry.AlgorithmRegistry;

import java.lang.reflect.Type;

public class AlgorithmDescriptionSerializer implements JsonSerializer<AlgorithmDescription<?>>, JsonDeserializer<AlgorithmDescription<?>> {
    @Override
    public JsonElement serialize(AlgorithmDescription<?> value, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(value.codeName);
    }

    @Override
    public AlgorithmDescription<?> deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        AlgorithmDescription<?> description = AlgorithmRegistry.getDescriptionByID(json.getAsString());
        return description == null ? AlgorithmRegistry.getDescriptionByID("fsr1") : description;
    }
}
