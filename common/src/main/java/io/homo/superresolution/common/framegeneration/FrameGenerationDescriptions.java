/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.common.framegeneration;

import io.homo.superresolution.api.StreamlineDistribution;
import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.FrameGenerationRegisterEvent;
import io.homo.superresolution.api.registry.FrameGenerationDescription;
import io.homo.superresolution.api.registry.FrameGenerationGroups;
import io.homo.superresolution.api.registry.FrameGenerationRegistry;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexMode;

public final class FrameGenerationDescriptions {
    public static final String AUTO_ID = "superresolution:auto";

    private static boolean registered;

    private FrameGenerationDescriptions() {
    }

    /**
     * Whether the current configuration might resolve to a Streamline-backed FG backend,
     * which decides at startup whether Super Resolution loads the Streamline interposer.
     * <p>
     * Streamline is only preloaded when all of these hold: (a) the FG algorithm is auto or
     * the DLSS FG group, (b) a Streamline distributor (Wisteria) has been registered via
     * {@link StreamlineDistribution}, and (c) startup-time Reflex is configured on
     * ({@code low_latency/mode = superresolution:nv_reflex} and NVIDIA Reflex mode ≠ OFF).
     * Runtime backend selection is otherwise the negotiator's job.
     */
    public static boolean mayUseStreamline(String providerId) {
        if (!AUTO_ID.equals(providerId) && !FrameGenerationGroups.DLSS_FG.getId().equals(providerId)) {
            return false;
        }
        if (!StreamlineDistribution.isProvided()) {
            return false;
        }
        if (!"superresolution:nv_reflex".equals(SuperResolutionConfig.getLowLatencyMode())) {
            return false;
        }
        return SuperResolutionConfig.getNVIDIAReflexMode() != NVIDIAReflexMode.OFF;
    }

    /**
     * Idempotent: the registry rejects duplicate ids, and this runs from
     * {@code FrameGeneration}'s static initializer, which several entry points can reach
     * first.
     */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        FrameGenerationRegistry.register(
                FrameGenerationDescription.builder()
                        .id(AUTO_ID)
                        .displayName(net.minecraft.network.chat.Component.translatable("superresolution.algorithm.frame_generation.auto"))
                        .automatic()
                        .build()
        );

        // DLSS Frame Generation group representative. Concrete backends
        // (Streamline / NVNGX) are registered by the Wisteria mod through
        // FrameGenerationRegisterEvent and join this group via .group(DLSS_FG).
        FrameGenerationRegistry.register(
                FrameGenerationDescription.builder()
                        .id(FrameGenerationGroups.DLSS_FG.getId())
                        .displayName(FrameGenerationGroups.DLSS_FG.getDisplayName())
                        .automatic()
                        .group(FrameGenerationGroups.DLSS_FG)
                        .build()
        );

        SuperResolutionAPI.EVENT_BUS.post(new FrameGenerationRegisterEvent());
    }
}
