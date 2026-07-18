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

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.vulkan.VkRenderSystem;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.Set;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_FORMAT_FEATURE_TRANSFER_DST_BIT;

public final class VulkanPresentationContext {
	private final VkRenderSystem renderSystem;
	private final VulkanDevice device;
	private final VulkanSurface surface;
	private final VulkanSwapchain swapchain;
	private boolean minimized;

	private VulkanPresentationContext(
		VkRenderSystem renderSystem,
		VulkanDevice device,
		VulkanSurface surface
	) {
		this.renderSystem = renderSystem;
		this.device = device;
		this.surface = surface;
		validate();
		this.swapchain = new VulkanSwapchain(this, surface);
		this.minimized = surface.isMinimized();
	}

	public static VulkanPresentationContext initialize(VulkanSurface surface) {
		VkRenderSystem renderSystem = RenderSystems.vulkan();
		if (renderSystem == null || renderSystem.device() == null) {
			throw new IllegalStateException("Super Resolution did not initialize a Vulkan device");
		}
		return new VulkanPresentationContext(renderSystem, renderSystem.device(), surface);
	}

	public boolean present(FrameResources frameResources) {
		return swapchain.present(frameResources);
	}

	public void consumeWithoutPresent(FrameResources frameResources) {
		swapchain.consumeWithoutPresent(frameResources);
	}

	public void tickWindow() {
		surface.refreshFramebufferSize();
		if (surface.consumeFramebufferResized()) {
			swapchain.requestRecreate();
		}
		boolean minimizedNow = surface.isMinimized();
		if (minimizedNow && !minimized) {
			swapchain.suspendPresentation();
		} else if (!minimizedNow && minimized) {
			swapchain.requestRecreate();
		}
		minimized = minimizedNow;
	}

	public void setVsync(boolean enabled) {
		swapchain.setVsync(enabled);
	}

	public void destroy() {
		swapchain.destroy();
		surface.destroySurface(renderSystem.getVulkanInstance());
	}

	public VkRenderSystem renderSystem() {
		return renderSystem;
	}

	public VulkanDevice device() {
		return device;
	}

	public VkPhysicalDevice physicalDevice() {
		return device.getPhysicalDevice();
	}

	public long surface() {
		return surface.surface();
	}

	private void validate() {
		if (surface.surface() == 0L) {
			throw new IllegalStateException("Vulkan presentation surface was not created");
		}

		try (MemoryStack stack = MemoryStack.stackPush()) {
			IntBuffer presentSupported = stack.mallocInt(1);
			int presentResult = vkGetPhysicalDeviceSurfaceSupportKHR(
				device.getPhysicalDevice(),
				device.getMainQueue().getQueueFamilyIndex(),
				surface.surface(),
				presentSupported
			);
			if (presentResult != VK_SUCCESS || presentSupported.get(0) == 0) {
				throw new IllegalStateException("Super Resolution graphics queue cannot present to the Vulkan surface");
			}

			VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
			int capabilitiesResult = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
				device.getPhysicalDevice(),
				surface.surface(),
				capabilities
			);
			if (capabilitiesResult != VK_SUCCESS) {
				throw new IllegalStateException("Failed to query Vulkan surface capabilities, VkResult=" + capabilitiesResult);
			}
			int requiredUsage = VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
			if ((capabilities.supportedUsageFlags() & requiredUsage) != requiredUsage) {
				throw new IllegalStateException(
					"Vulkan surface images do not support transfer destination and color attachment usage"
				);
			}

			IntBuffer formatCount = stack.ints(0);
			int formatResult = vkGetPhysicalDeviceSurfaceFormatsKHR(
				device.getPhysicalDevice(),
				surface.surface(),
				formatCount,
				null
			);
			if (formatResult != VK_SUCCESS || formatCount.get(0) == 0) {
				throw new IllegalStateException("Vulkan surface has no supported formats");
			}
			VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(formatCount.get(0), stack);
			vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), surface.surface(), formatCount, formats);
			boolean hasBlitDestination = false;
			for (int i = 0; i < formats.capacity(); i++) {
				int format = formats.get(i).format();
				if (format == VK_FORMAT_UNDEFINED) {
					if (supportsBlitDestination(stack, VK_FORMAT_B8G8R8A8_UNORM)
						|| supportsBlitDestination(stack, VK_FORMAT_R8G8B8A8_UNORM)) {
						hasBlitDestination = true;
						break;
					}
				} else if (supportsBlitDestination(stack, format)) {
					hasBlitDestination = true;
					break;
				}
			}
			if (!hasBlitDestination) {
				throw new IllegalStateException("Vulkan surface has no format that supports transfer blits");
			}
		}
	}

	private boolean supportsBlitDestination(MemoryStack stack, int format) {
		VkFormatProperties properties = VkFormatProperties.calloc(stack);
		vkGetPhysicalDeviceFormatProperties(device.getPhysicalDevice(), format, properties);
		int required = VK_FORMAT_FEATURE_TRANSFER_DST_BIT | VK_FORMAT_FEATURE_BLIT_DST_BIT;
		return (properties.optimalTilingFeatures() & required) == required;
	}
}
