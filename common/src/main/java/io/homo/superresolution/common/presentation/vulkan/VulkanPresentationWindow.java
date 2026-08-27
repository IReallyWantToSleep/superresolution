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

import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.presentation.DLSSNRPostProcessor;
import io.homo.superresolution.common.presentation.capture.FrameCaptureManager;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.common.presentation.window.PresentationWindowState;

public final class VulkanPresentationWindow {
	private static VulkanPresentationContext context;
	private static VulkanSurface surface;
	private static boolean shown;
	private static boolean requestedVsync = false;

	private VulkanPresentationWindow() {
	}

	public static synchronized void initialize(
		VulkanPresentationContext presentationContext,
		VulkanSurface presentationSurface
	) {
		if (context != null) {
			return;
		}
		FrameCaptureManager.initialize(
				presentationContext.device(),
				presentationContext.framePacingTiming()
		);
		presentationContext.setVsync(requestedVsync);
		context = presentationContext;
		surface = presentationSurface;
	}

	public static void endMinecraftFrame() {
		VulkanPresentationContext presentationContext = context;
		VulkanSurface presentationSurface = surface;
		if (presentationContext == null || presentationSurface == null) {
			return;
		}
		PresentationWindowState.requireOwnerThread();

		FrameResources frameResources = FrameCaptureManager.finishFrame();
		LowLatency.endRenderSubmission();

		presentationContext.tickWindow();

		if (frameResources == null) {
			return;
		}
		presentationContext.setVsync(requestedVsync);
		if (!frameResources.hasFinalColor()) {
			consumeRenderedFrame(presentationContext, frameResources);
			throw new IllegalStateException("Vulkan presentation frame is missing final color");
		}
		// MINIMIZE GUARD: Consume captured resources without acquiring or presenting a swapchain image.
		if (!presentationSurface.shouldClose()
			&& !presentationSurface.isMinimized()) {
			boolean presented = presentationContext.present(frameResources);
			if (presented && !shown) {
				presentationSurface.show();
				shown = true;
			}
		} else {
			consumeRenderedFrame(presentationContext, frameResources);
		}
	}

	private static void consumeRenderedFrame(
		VulkanPresentationContext presentationContext,
		FrameResources frameResources
	) {
		presentationContext.consumeWithoutPresent(frameResources);
	}

	public static synchronized void setVsync(boolean enabled) {
		requestedVsync = enabled;
		if (context != null) {
			context.setVsync(requestedVsync);
		}
	}

	public static synchronized void flushCapturedFrame() {
		VulkanPresentationContext presentationContext = context;
		if (presentationContext == null) {
			return;
		}
		FrameResources pending = FrameCaptureManager.finishFrame();
		if (pending != null) {
			presentationContext.consumeWithoutPresent(pending);
		}
	}

	public static boolean shutdownApplicationManagedProvider(
			String providerId,
			Runnable teardown
	) {
		VulkanPresentationContext presentationContext = context;
		return presentationContext != null
				&& presentationContext.shutdownApplicationManagedProvider(providerId, teardown);
	}

	public static synchronized void shutdown() {
		VulkanPresentationContext presentationContext = context;
		FrameResources pending = FrameCaptureManager.finishFrame();
		if (presentationContext != null && pending != null) {
			presentationContext.consumeWithoutPresent(pending);
		}
		if (presentationContext != null) {
			presentationContext.device().getMainQueue().waitIdle();
		}
		if (presentationContext != null) {
			presentationContext.destroy();
		}
		FrameCaptureManager.shutdown();
		DLSSNRPostProcessor.shutdown();
		context = null;
		surface = null;
		shown = false;
	}

	public static boolean isInitialized() {
		return context != null && surface != null;
	}

}
