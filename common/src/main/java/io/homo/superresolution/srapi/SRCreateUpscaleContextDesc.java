package io.homo.superresolution.srapi;

import io.homo.superresolution.core.math.Vector2i;
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
