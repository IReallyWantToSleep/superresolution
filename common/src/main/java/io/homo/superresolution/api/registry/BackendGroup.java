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

import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * User-visible algorithm group that aggregates one or more {@link FrameGenerationDescription}
 * or {@link LowLatencyDescription} backends. The UI exposes groups (e.g. "DLSS Frame
 * Generation", "NVIDIA Reflex") rather than backends; the negotiator picks a concrete
 * backend inside the selected group at runtime.
 */
public final class BackendGroup {
    private final String id;
    private final Component displayName;

    private BackendGroup(String id, Component displayName) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
    }

    public static BackendGroup of(String id, Component displayName) {
        return new BackendGroup(id, displayName);
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BackendGroup that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BackendGroup{" + id + "}";
    }
}
