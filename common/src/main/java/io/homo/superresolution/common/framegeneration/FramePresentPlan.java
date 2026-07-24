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

import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import java.util.List;

/**
 * Result of preparing frame generation for one presented frame.
 * <p>
 * With the Streamline backend the interpolated frames are produced and presented
 * by the Streamline swapchain interposer, so {@link #generatedFrames()} is empty.
 * With the NVNGX backend the interpolated frames are returned here and the caller
 * must present each of them (in order) before the real frame, with pacing.
 */
public final class FramePresentPlan {
    private static final FramePresentPlan NONE = new FramePresentPlan(false, List.of(), null);
    private static final FramePresentPlan STREAMLINE = new FramePresentPlan(true, List.of(), null);

    private final boolean frameGenerationActive;
    private final List<VulkanTexture> generatedFrames;
    private final VulkanTexture realFrame;

    private FramePresentPlan(
            boolean frameGenerationActive,
            List<VulkanTexture> generatedFrames,
            VulkanTexture realFrame
    ) {
        this.frameGenerationActive = frameGenerationActive;
        this.generatedFrames = generatedFrames;
        this.realFrame = realFrame;
    }

    public static FramePresentPlan none() {
        return NONE;
    }

    public static FramePresentPlan streamline() {
        return STREAMLINE;
    }

    public static FramePresentPlan generated(List<VulkanTexture> generatedFrames, VulkanTexture realFrame) {
        if (generatedFrames == null || generatedFrames.isEmpty()) {
            return NONE;
        }
        return new FramePresentPlan(true, List.copyOf(generatedFrames), realFrame);
    }

    public boolean frameGenerationActive() {
        return frameGenerationActive;
    }

    public List<VulkanTexture> generatedFrames() {
        return generatedFrames;
    }

    /**
     * The NVNGX "output real" frame — the rendered frame passed through DLSS-G, which
     * carries the DLSS-FG indicator so it stays steady across real and generated
     * frames. Present this instead of the raw backbuffer for the real frame. Null when
     * the raw backbuffer should be presented directly.
     */
    public VulkanTexture realFrame() {
        return realFrame;
    }
}
