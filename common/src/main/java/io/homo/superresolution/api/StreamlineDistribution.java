/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.api;

import javax.annotation.Nullable;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Early handoff point through which the Wisteria mod (or any other distributor) hands Super
 * Resolution the directory containing the NVIDIA Streamline plugin DLLs
 * ({@code sl.common.dll}, {@code sl.interposer.dll}, {@code sl.dlss_g.dll},
 * {@code sl.reflex.dll}, {@code sl.pcl.dll}, {@code NvLowLatencyVk.dll}).
 * <p>
 * The distributor calls {@link #provide(Supplier)} from its mod entrypoint (which fires
 * before {@code Minecraft.<init>}, i.e. before SR reads its configuration or decides
 * whether to initialize Streamline). The supplier is only consulted lazily, when SR
 * actually needs the directory; a distributor is free to defer extraction until then.
 * <p>
 * When no distributor is registered, SR's Streamline paths are silently disabled and
 * frame generation falls back to any non-Streamline backend that happens to be
 * registered (typically NVNGX from Wisteria).
 */
public final class StreamlineDistribution {
    private static volatile Supplier<Path> pluginDirectorySupplier;
    private static volatile Path resolvedPluginDirectory;
    private static volatile boolean resolved;

    private StreamlineDistribution() {
    }

    /**
     * Register a distributor. The {@code pluginDirectorySupplier} is expected to extract the
     * Streamline DLLs to a directory (typically {@code <game>/config/wisteria/streamline/})
     * and return that directory. It is called at most once per game session, only when SR
     * decides to load Streamline. Later {@code provide} calls replace an earlier one.
     */
    public static synchronized void provide(Supplier<Path> pluginDirectorySupplier) {
        StreamlineDistribution.pluginDirectorySupplier = pluginDirectorySupplier;
        resolvedPluginDirectory = null;
        resolved = false;
    }

    /** Whether any distributor has registered a supplier. */
    public static boolean isProvided() {
        return pluginDirectorySupplier != null;
    }

    /**
     * Resolves the plugin directory, invoking the supplier if it hasn't been called yet.
     * Idempotent: subsequent calls return the cached result. Returns {@code null} when no
     * distributor was registered or when the supplier itself returned {@code null} / threw.
     */
    public static synchronized @Nullable Path pluginDirectory() {
        if (resolved) {
            return resolvedPluginDirectory;
        }
        Supplier<Path> supplier = pluginDirectorySupplier;
        resolved = true;
        if (supplier == null) {
            resolvedPluginDirectory = null;
            return null;
        }
        try {
            resolvedPluginDirectory = supplier.get();
        } catch (Throwable throwable) {
            resolvedPluginDirectory = null;
        }
        return resolvedPluginDirectory;
    }
}
