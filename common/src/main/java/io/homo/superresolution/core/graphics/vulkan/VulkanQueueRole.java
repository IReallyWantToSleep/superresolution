/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.graphics.vulkan;

public enum VulkanQueueRole {
    MAIN("Main"),
    FRAME_GENERATION("FrameGeneration"),
    PRESENT("Present");

    private final String debugLabel;

    VulkanQueueRole(String debugLabel) {
        this.debugLabel = debugLabel;
    }

    public String debugLabel() {
        return debugLabel;
    }
}
