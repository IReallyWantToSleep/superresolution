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

import io.homo.superresolution.api.SuperResolutionAPI;
import io.homo.superresolution.api.event.FrameGenerationRegisterEvent;
import io.homo.superresolution.api.registry.FrameGenerationDescription;
import io.homo.superresolution.api.registry.FrameGenerationRegistry;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.core.streamline.Streamline;

public final class FrameGenerationDescriptions {
    public static final String AUTO_ID = "superresolution:auto";
    public static final String STREAMLINE_ID = "superresolution:streamline";
    public static final String NGX_ID = "superresolution:ngx";

    private static boolean registered;

    private FrameGenerationDescriptions() {
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
                        .displayName("Auto")
                        .automatic()
                        .build()
        );

        // Registration order is the automatic entry's preference order: Streamline first,
        // so Windows keeps using the interposer whenever it actually came up.
        FrameGenerationRegistry.register(
                FrameGenerationDescription.builder()
                        .id(STREAMLINE_ID)
                        .displayName("Streamline")
                        .requirement(
                                Requirement.nothing()
                                        .isTrue(() -> Streamline.isSupportedPlatform() && Streamline.isNativeAvailable())
                        )
                        .providerFactory(StreamlineFrameGenerationBackend::new)
                        .build()
        );

        FrameGenerationRegistry.register(
                FrameGenerationDescription.builder()
                        .id(NGX_ID)
                        .displayName("NVNGX")
                        .providerFactory(NgxFrameGenerationBackend::new)
                        .build()
        );

        SuperResolutionAPI.EVENT_BUS.post(new FrameGenerationRegisterEvent());
    }
}
