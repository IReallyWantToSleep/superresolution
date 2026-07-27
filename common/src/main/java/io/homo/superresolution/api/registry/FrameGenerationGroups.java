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

/**
 * Frame generation algorithm groups predefined by Super Resolution. Backends registered
 * by other mods are expected to declare membership through {@code FrameGenerationDescription.Builder#group}.
 */
public final class FrameGenerationGroups {
    /** DLSS Frame Generation. Both the Streamline and NVNGX backends belong here. */
    public static final BackendGroup DLSS_FG = BackendGroup.of(
            "superresolution:dlss_fg",
            Component.translatable("superresolution.algorithm.frame_generation.dlss")
    );

    private FrameGenerationGroups() {
    }
}
