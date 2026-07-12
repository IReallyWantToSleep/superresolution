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

package io.homo.superresolution.core.streamline;

import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.SuperResolutionConstants;
import org.lwjgl.system.Configuration;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.nio.file.Path;

public final class Streamline {
    private static boolean initAttempted;
    private static StreamlineSession defaultSession;

    private Streamline() {
    }

    public static boolean isSupportedPlatform() {
        return Platform.currentPlatform.getOS().type == OperatingSystemType.WINDOWS;
    }

    public static boolean isNativeAvailable() {
        return NativeLibManager.LIB_SUPER_RESOLUTION_STREAMLINE != null
                && NativeLibManager.LIB_SUPER_RESOLUTION_STREAMLINE.available;
    }

    public static boolean isInitialized() {
        return defaultSession != null && !defaultSession.isClosed();
    }

    public static boolean isSupportedOnCurrentVersion() {
        return true;
    }

    public static synchronized boolean prepareEarly() {
        Path nativeDir = SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath().toAbsolutePath();
        return prepareEarly(StreamlineInitConfig.defaultConfig(
                nativeDir,
                SuperResolutionConstants.ERROR_DIR.getPath().toAbsolutePath()
        ));
    }

    public static synchronized boolean prepareEarly(StreamlineInitConfig config) {
        if (!isSupportedOnCurrentVersion() || !isSupportedPlatform()) {
            return false;
        }
        Configuration.VULKAN_LIBRARY_NAME.set(
                NativeLibManager.LIB_STREAMLINE_INTERPOSER.getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath()).toAbsolutePath().toString()
        );
        NativeLibManager.extract(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        NativeLibManager.load(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath());
        return initEarly(config);
    }

    public static synchronized boolean initEarly() {
        Path nativeDir = SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath().toAbsolutePath();
        return initEarly(StreamlineInitConfig.defaultConfig(
                nativeDir,
                SuperResolutionConstants.DATA_DIR.getPath().toAbsolutePath()
        ));
    }

    public static synchronized boolean initEarly(StreamlineInitConfig config) {
        if (!isSupportedOnCurrentVersion() || !isSupportedPlatform() || !isNativeAvailable()) {
            return false;
        }
        if (defaultSession != null && defaultSession.isClosed()) {
            defaultSession.close();
            defaultSession = null;
            initAttempted = false;
        }
        if (isInitialized()) {
            return true;
        }
        if (initAttempted) {
            return false;
        }
        initAttempted = true;
        try {
            defaultSession = open(config);
        } catch (StreamlineException exception) {
            SuperResolution.LOGGER.error("Streamline init failed. result={} ({})",
                    StreamlineResult.nameOf(exception.result()), exception.result());
            return false;
        } catch (RuntimeException exception) {
            SuperResolution.LOGGER.error("Streamline init failed.", exception);
            return false;
        }
        SuperResolution.LOGGER.info("Streamline initialized.");
        return true;
    }

    public static synchronized void shutdown() {
        if (defaultSession == null) {
            return;
        }
        defaultSession.close();
        defaultSession = null;
        initAttempted = false;
    }

    public static StreamlineSession open(StreamlineInitConfig config) {
        if (!isSupportedPlatform()) {
            throw new UnsupportedOperationException("Streamline is only available on Windows Vulkan");
        }
        if (!isNativeAvailable()) {
            throw new IllegalStateException("Streamline native library is not loaded");
        }
        return StreamlineSession.open(config);
    }

    public static long createVkInstance(long createInfoAddress) {
        return StreamlineNative.nCreateVkInstance(createInfoAddress);
    }

    public static long createVkDevice(long instanceAddress, long physicalDeviceAddress, long createInfoAddress) {
        return StreamlineNative.nCreateVkDevice(instanceAddress, physicalDeviceAddress, createInfoAddress);
    }

    public static boolean setVulkanInfo(long instanceAddress, long physicalDeviceAddress, long deviceAddress, int graphicsQueueFamilyIndex) {
        if (!isInitialized()) {
            return false;
        }
        StreamlineTypes.VulkanInfo info = new StreamlineTypes.VulkanInfo();
        info.instance = instanceAddress;
        info.physicalDevice = physicalDeviceAddress;
        info.device = deviceAddress;
        info.computeQueueFamily = graphicsQueueFamilyIndex;
        info.graphicsQueueFamily = graphicsQueueFamilyIndex;
        info.opticalFlowQueueFamily = graphicsQueueFamilyIndex;
        int result = defaultSession.setVulkanInfo(info);
        if (result != 0) {
            SuperResolution.LOGGER.error("slSetVulkanInfo failed. result={} ({})", StreamlineResult.nameOf(result), result);
            return false;
        }
        SuperResolution.LOGGER.info("Streamline Vulkan info set.");
        return true;
    }

    public static int getNewFrameToken(int frameIndex, StreamlineTypes.FrameToken outToken) {
        if (!isInitialized()) {
            return -1;
        }
        return defaultSession.getNewFrameToken(frameIndex, outToken);
    }

    public static int setConstants(
            StreamlineTypes.Constants constants,
            StreamlineTypes.FrameToken frame,
            StreamlineTypes.Viewport viewport
    ) {
        if (!isInitialized()) {
            return -1;
        }
        return defaultSession.setConstants(constants, frame, viewport);
    }

    public static int getLastVkResult() {
        return StreamlineNative.nGetLastVkResult();
    }

    public static boolean isFeatureSupported(int feature, VkPhysicalDevice physicalDevice) {
        if (!isInitialized() || physicalDevice == null) {
            return false;
        }
        boolean[] supported = new boolean[1];
        return defaultSession.isFeatureSupported(feature, physicalDevice.address(), supported) == 0 && supported[0];
    }

    public static boolean isDLSSGSupported() {
        if (!isInitialized()) {
            return false;
        }
        StreamlineTypes.FeatureRequirements requirements = new StreamlineTypes.FeatureRequirements();
        return defaultSession.getFeatureRequirements(StreamlineFeature.DLSS_G, requirements) == 0;
    }

    public static int setDLSSGOptions(boolean enabled, int framesToGenerate) {
        if (!isInitialized()) {
            return -1;
        }
        StreamlineTypes.DlssGOptions options = new StreamlineTypes.DlssGOptions();
        options.mode = enabled ? StreamlineTypes.DlssGMode.ON : StreamlineTypes.DlssGMode.OFF;
        options.numFramesToGenerate = Math.max(1, framesToGenerate);
        return defaultSession.dlssGSetOptions(new StreamlineTypes.Viewport(0), options);
    }

    public static StreamlineDLSSGState getDLSSGState() {
        if (!isInitialized()) {
            return null;
        }
        StreamlineTypes.DlssGState nativeState = new StreamlineTypes.DlssGState();
        int result = defaultSession.dlssGGetState(new StreamlineTypes.Viewport(0), nativeState, null);
        if (result != 0) {
            return null;
        }
        StreamlineDLSSGState state = new StreamlineDLSSGState();
        state.estimatedVramUsage = nativeState.estimatedVramUsage;
        state.status = nativeState.status;
        return state;
    }
}
