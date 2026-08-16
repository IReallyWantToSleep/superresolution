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

import io.homo.superresolution.common.framegeneration.FrameGenerationMode;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;

import java.util.Objects;

/**
 * Immutable render-thread snapshot consumed later by an application-managed
 * frame-generation dispatch.
 * <p>
 * Implementations may extend this contract with provider-specific immutable
 * values, but must not retain mutable global configuration, matrices, or frame
 * state that can change after the real frame is queued.
 */
public interface ProviderInputSnapshot {
    String providerId();

    int logicalFrameIndex();

    FrameGenerationMode mode();

    FGConstants constants();

    boolean historyResetRequested();

    static ProviderInputSnapshot of(
            String providerId,
            int logicalFrameIndex,
            FrameGenerationMode mode,
            FGConstants constants,
            boolean historyResetRequested
    ) {
        return new StandardProviderInputSnapshot(
                providerId,
                logicalFrameIndex,
                mode,
                constants,
                historyResetRequested
        );
    }

    record StandardProviderInputSnapshot(
            String providerId,
            int logicalFrameIndex,
            FrameGenerationMode mode,
            FGConstants constants,
            boolean historyResetRequested
    ) implements ProviderInputSnapshot {
        public StandardProviderInputSnapshot {
            providerId = Objects.requireNonNull(providerId, "providerId cannot be null");
            if (providerId.isBlank()) {
                throw new IllegalArgumentException("providerId cannot be blank");
            }
            mode = Objects.requireNonNull(mode, "mode cannot be null");
            constants = Objects.requireNonNull(constants, "constants cannot be null");
        }
    }
}
