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

import java.util.Objects;

/**
 * Declaration that a frame generation backend is tied to (or incompatible with) a specific
 * low latency backend. The negotiator consults it when picking a {@code (fg, ll)} pair.
 * <p>
 * {@link Kind#REQUIRES} means the FG backend only runs when the negotiator has selected the
 * named LL backend (Streamline FG requires Streamline Reflex — DLSS-G's present pacing
 * depends on it).
 * <p>
 * {@link Kind#EXCLUDES} means the FG backend must not be paired with the named LL backend
 * (NVNGX FG under the Streamline interposer is untested and disallowed here).
 * <p>
 * {@link Kind#NONE} means no constraint.
 */
public final class LowLatencyBinding {
    public enum Kind {
        NONE,
        REQUIRES,
        EXCLUDES
    }

    private static final LowLatencyBinding NONE = new LowLatencyBinding(Kind.NONE, null);

    private final Kind kind;
    private final String backendId;

    private LowLatencyBinding(Kind kind, String backendId) {
        this.kind = kind;
        this.backendId = backendId;
    }

    public static LowLatencyBinding none() {
        return NONE;
    }

    public static LowLatencyBinding requires(String backendId) {
        return new LowLatencyBinding(Kind.REQUIRES, Objects.requireNonNull(backendId));
    }

    public static LowLatencyBinding excludes(String backendId) {
        return new LowLatencyBinding(Kind.EXCLUDES, Objects.requireNonNull(backendId));
    }

    public Kind getKind() {
        return kind;
    }

    public String getBackendId() {
        return backendId;
    }
}
