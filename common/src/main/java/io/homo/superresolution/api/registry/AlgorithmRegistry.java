package io.homo.superresolution.api.registry;

import io.homo.superresolution.common.upscale.AlgorithmDescriptions;

import java.util.HashMap;
import java.util.Map;

public class AlgorithmRegistry {
    private static final Map<String, AlgorithmDescription<?>> algorithmMap = new HashMap<>();
    private static final Map<String, AlgorithmDescription<?>> codeNameAlgorithmMap = new HashMap<>();

    static {
        AlgorithmDescriptions.registryAlgorithms();
    }

    public static void registry(AlgorithmDescription<?> description) {
        algorithmMap.put(description.getUUID(), description);
        codeNameAlgorithmMap.put(description.getCodeName(), description);
    }

    public static Map<String, AlgorithmDescription<?>> getAlgorithmMap() {
        return algorithmMap;
    }

    public static AlgorithmDescription<?> getDescriptionByID(String id) {
        return codeNameAlgorithmMap.get(id);
    }
}
