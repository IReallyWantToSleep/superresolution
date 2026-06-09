/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.command.ResourceAccessType;

import static org.lwjgl.vulkan.VK10.*;

public record VulkanResourceState(
        int layout,
        int accessMask,
        int stageMask,
        ResourceAccessType accessType
) {
    public static final VulkanResourceState UNDEFINED = new VulkanResourceState(
            VK_IMAGE_LAYOUT_UNDEFINED,
            0,
            VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
            ResourceAccessType.UNDEFINED
    );
}
