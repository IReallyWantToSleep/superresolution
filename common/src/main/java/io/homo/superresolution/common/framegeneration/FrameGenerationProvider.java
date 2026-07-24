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

/**
 * Backend used to run DLSS Frame Generation.
 * AUTO selects Streamline when it is initialized (Windows presentation)
 * and falls back to the cross-platform NVNGX path otherwise.
 */
public enum FrameGenerationProvider {
    AUTO,
    STREAMLINE,
    NGX
}
