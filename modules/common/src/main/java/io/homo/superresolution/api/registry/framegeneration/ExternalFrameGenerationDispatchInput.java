/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.api.registry.framegeneration;

import io.homo.superresolution.common.framegeneration.FrameGenerationMode;
import io.homo.superresolution.common.framegeneration.constants.FrameGenerationConstants;
import io.homo.superresolution.common.presentation.capture.FrameResources;

import java.util.Objects;

public record ExternalFrameGenerationDispatchInput(
        FrameResources frameResources,
        FrameGenerationConstants constants,
        FrameGenerationMode mode,
        int colorWidth,
        int colorHeight,
        int colorFormat,
        int backBufferCount,
        long vkCommandBuffer
) {
    public ExternalFrameGenerationDispatchInput {
        frameResources = Objects.requireNonNull(frameResources, "frameResources cannot be null");
        constants = Objects.requireNonNull(constants, "constants cannot be null");
        mode = Objects.requireNonNull(mode, "mode cannot be null");
        if (colorWidth <= 0 || colorHeight <= 0) {
            throw new IllegalArgumentException("Color dimensions must be positive");
        }
        if (backBufferCount <= 0 || vkCommandBuffer == 0L) {
            throw new IllegalArgumentException("External presentation input is invalid");
        }
    }
}
