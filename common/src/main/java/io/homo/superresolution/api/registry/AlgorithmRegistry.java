package io.homo.superresolution.api.registry;

import io.homo.superresolution.common.upscale.AlgorithmDescriptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AlgorithmRegistry {
    private static final Map<String, AlgorithmDescription<?>> algorithmMap = new HashMap<>();

    static {
        AlgorithmDescriptions.registryAlgorithms();
    }

    public static void registry(AlgorithmDescription<?> description) {
        algorithmMap.put(description.getUUID(), description);
    }

    public static Map<String, AlgorithmDescription<?>> getAlgorithmMap() {
        return algorithmMap;
    }

    public static AlgorithmDescription<?> getDescriptionByID(String id) {
        for (AlgorithmDescription<?> description : algorithmMap.values()) {
            if (Objects.equals(description.codeName, id)) {
                return description;
            }
        }
        return null;
    }
}
