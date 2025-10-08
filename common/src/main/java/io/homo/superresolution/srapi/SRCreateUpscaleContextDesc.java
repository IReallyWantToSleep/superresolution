/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.srapi;

import org.joml.Vector2i;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;

public class SRCreateUpscaleContextDesc {
    public long device;
    public long phyDevice;
    public Vector2i upscaledSize;
    public Vector2i renderSize;
    public int flags;

    public SRCreateUpscaleContextDesc(long device, long phyDevice, Vector2i upscaledSize, Vector2i renderSize, int flags) {
        this.device = device;
        this.phyDevice = phyDevice;
        this.upscaledSize = upscaledSize;
        this.renderSize = renderSize;
        this.flags = flags;
    }

    public SRCreateUpscaleContextDesc(VkDevice device, VkPhysicalDevice phyDevice, Vector2i upscaledSize, Vector2i renderSize, int flags) {
        if (device != null) this.device = device.address();
        if (phyDevice != null) this.phyDevice = phyDevice.address();
        this.upscaledSize = upscaledSize;
        this.renderSize = renderSize;
        this.flags = flags;
    }
}
