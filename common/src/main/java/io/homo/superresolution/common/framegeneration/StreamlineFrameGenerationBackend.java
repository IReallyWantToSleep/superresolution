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
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.lowlatency.nv.NVIDIAReflexMode;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.streamline.Streamline;
import io.homo.superresolution.core.streamline.StreamlineTypes;

/**
 * Streamline (sl.dlss_g) backend, Windows-only. Wraps
 * {@link StreamlineFrameGenerationAdapter}; the Streamline interposer produces and
 * presents the interpolated frames itself, so the plan carries no frames for the
 * swapchain.
 */
public final class StreamlineFrameGenerationBackend implements FrameGenerationProvider {

    @Override
    public void initialize() {
        StreamlineFrameGenerationAdapter.initialize();
    }

    @Override
    public void shutdown() {
        StreamlineFrameGenerationAdapter.shutdown();
    }

    @Override
    public boolean isAvailable() {
        return StreamlineFrameGenerationAdapter.isAvailable();
    }

    @Override
    public int supportedGeneratedFrameCount() {
        return StreamlineFrameGenerationAdapter.supportedGeneratedFrameCount();
    }

    @Override
    public int presentationManagedGeneratedFrameCount(FrameGenerationMode mode) {
        // The Streamline swapchain interposer presents the generated frames.
        return 0;
    }

    @Override
    public boolean dependenciesSatisfied() {
        // Streamline DLSS-G requires Reflex to drive its present pacing.
        return "superresolution:nv_reflex".equals(SuperResolutionConfig.getLowLatencyMode())
                && SuperResolutionConfig.getNVIDIAReflexMode() != NVIDIAReflexMode.OFF;
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
        StreamlineTypes.FrameToken token = Streamline.currentFrame();
        if (token == null
                || token.nativeHandle == 0L
                || token.frameIndex != frameResources.logicalFrameIndex()) {
            StreamlineFrameGenerationAdapter.disable();
            return FramePresentPlan.none();
        }
        boolean prepared = StreamlineFrameGenerationAdapter.prepareFrame(
                frameResources,
                constants,
                token,
                mode,
                colorWidth,
                colorHeight,
                colorFormat,
                backBufferCount,
                commandBuffer
        );
        return prepared ? FramePresentPlan.streamline() : FramePresentPlan.none();
    }

    @Override
    public void finishPresent(FrameResources frameResources, boolean frameGenerationActive) {
        StreamlineFrameGenerationAdapter.finishPresent(frameResources, frameGenerationActive);
    }

    @Override
    public void disable() {
        StreamlineFrameGenerationAdapter.disable();
    }
}
