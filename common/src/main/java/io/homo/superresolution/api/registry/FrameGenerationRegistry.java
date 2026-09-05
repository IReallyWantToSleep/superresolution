/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.api.registry;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of frame generation backends.
 * <p>
 * Insertion order is meaningful: the automatic entry resolves to the first registered
 * description whose {@link io.homo.superresolution.api.utils.Requirement} passes, so
 * built-ins register most-preferred first. Registration is expected to happen once at
 * startup on a single thread (from {@code FrameGenerationDescriptions}, and from listeners
 * of {@code FrameGenerationRegisterEvent}); this class is not thread-safe.
 */
public final class FrameGenerationRegistry {
    private static final Map<String, FrameGenerationDescription> descriptions = new Object2ObjectLinkedOpenHashMap<>();
    private static final Map<String, Boolean> supportCache = new Object2BooleanArrayMap<>();

    private FrameGenerationRegistry() {
    }

    public static void register(FrameGenerationDescription description) {
        Objects.requireNonNull(description, "description cannot be null");
        String id = description.getId();
        if (descriptions.containsKey(id)) {
            throw new IllegalStateException("FrameGenerationDescription with id " + id + " is already registered");
        }
        descriptions.put(id, description);
        supportCache.remove(id);
    }

    public static FrameGenerationDescription getDescriptionById(String id) {
        return descriptions.get(id);
    }

    public static Map<String, FrameGenerationDescription> getDescriptions() {
        return Collections.unmodifiableMap(descriptions);
    }

    public static boolean isRegistered(String id) {
        return descriptions.containsKey(id);
    }

    public static boolean isSupported(FrameGenerationDescription description) {
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
