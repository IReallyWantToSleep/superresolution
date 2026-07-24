/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.api.registry;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class LowLatencyRegistry {
    private static final Map<String, LowLatencyDescription> descriptions = new Object2ObjectLinkedOpenHashMap<>();
    private static final Map<String, Boolean> supportCache = new Object2BooleanArrayMap<>();

    private LowLatencyRegistry() {
    }

    public static void register(LowLatencyDescription description) {
        Objects.requireNonNull(description, "description cannot be null");
        String id = description.getId();
        if (descriptions.containsKey(id)) {
            throw new IllegalStateException("LowLatencyDescription with id " + id + " is already registered");
        }
        descriptions.put(id, description);
        supportCache.remove(id);
    }

    public static LowLatencyDescription getDescriptionById(String id) {
        return descriptions.get(id);
    }

    public static Map<String, LowLatencyDescription> getDescriptions() {
        return Collections.unmodifiableMap(descriptions);
    }

    public static boolean isRegistered(String id) {
        return descriptions.containsKey(id);
    }

    public static boolean isSupported(LowLatencyDescription description) {
        String id = description.getId();
        if (!supportCache.containsKey(id)) {
            boolean supported = description.getRequirement().check().support();
            supportCache.put(id, supported);
        }
        return supportCache.get(id);
    }

    public static void clearSupportCache() {
        supportCache.clear();
    }

    public static void invalidateSupportCache(String id) {
        supportCache.remove(id);
    }
}
