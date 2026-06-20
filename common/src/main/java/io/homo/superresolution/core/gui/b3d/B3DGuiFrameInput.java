/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.core.gui.b3d;

import io.homo.superresolution.core.gui.NanoVGScreen;

public record B3DGuiFrameInput(
        NanoVGScreen<?> screen,
        float mouseX,
        float mouseY,
        float partialTick,
        float guiScale,
        float dpiScale,
        int framebufferWidth,
        int framebufferHeight
) {
}
