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

import io.homo.superresolution.api.registry.FrameGenerationProvider;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.presentation.capture.FrameResources;

/**
 * Cross-platform NVNGX (raw DLSS-G) backend. Wraps {@link NgxFrameGenerationAdapter},
 * which owns the NGX feature and the vendored runtime; this class is only the
 * {@link FrameGenerationProvider} facade.
 */
public final class NgxFrameGenerationBackend implements FrameGenerationProvider {

    @Override
    public void initialize() {
        NgxFrameGenerationAdapter.initialize();
    }

    @Override
    public void shutdown() {
        NgxFrameGenerationAdapter.shutdown();
    }

    @Override
    public boolean isAvailable() {
        return NgxFrameGenerationAdapter.isAvailable();
    }

    @Override
    public int supportedGeneratedFrameCount() {
        return NgxFrameGenerationAdapter.supportedGeneratedFrameCount();
    }

    @Override
    public int presentationManagedGeneratedFrameCount(FrameGenerationMode mode) {
        // NGX hands the interpolated frames back for the swapchain to present.
        return Math.min(
                Math.max(1, mode.generatedFrameCount()),
                supportedGeneratedFrameCount()
        );
    }

    @Override
    public boolean dependenciesSatisfied() {
        // No Reflex requirement: Linux has none, and the mod drives the pacing itself.
        return true;
    }

    @Override
    public FramePresentPlan prepareFrame(
            FrameResources frameResources,
            FGConstants constants,
            FrameGenerationMode mode,
            int colorWidth,
            int colorHeight,
            int colorFormat,
            int backBufferCount,
            long commandBuffer
    ) {
        NgxFrameGenerationAdapter.PrepareResult result = NgxFrameGenerationAdapter.prepareFrame(
                frameResources,
                constants,
                mode,
                colorWidth,
                colorHeight,
                commandBuffer
        );
        return result == null
                ? FramePresentPlan.none()
                : FramePresentPlan.generated(result.generatedFrames(), result.realFrame());
    }

    @Override
    public void finishPresent(FrameResources frameResources, boolean frameGenerationActive) {
        // The mod paces and presents NGX frames itself; nothing to report back.
    }

    @Override
    public void disable() {
        NgxFrameGenerationAdapter.disable();
    }
}
