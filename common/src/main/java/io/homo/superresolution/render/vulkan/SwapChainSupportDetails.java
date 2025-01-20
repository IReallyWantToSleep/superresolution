package io.homo.superresolution.render.vulkan;

import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;

public class SwapChainSupportDetails {
    protected VkSurfaceCapabilitiesKHR capabilities;
    protected VkSurfaceFormatKHR.Buffer formats;
    protected IntBuffer presentModes;

}
