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

package io.homo.superresolution.core.ngx;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.NativeLibManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.SuperResolutionConstants;
import io.homo.superresolution.core.graphics.vulkan.VkReflectionHelper;
import io.homo.superresolution.core.graphics.vulkan.VulkanDevice;
import io.homo.superresolution.core.utils.LargeStackExecutor;

import java.util.HashMap;
import java.util.Map;

public final class NgxInitializer {
    private static final String PROJECT_ID = "3a799712-b54a-407c-82b0-eb3366f0f1e3";
    private static final String ENGINE_VERSION = "11.45.14";
    private static final Object INIT_LOCK = new Object();

    private static boolean bindingLoaded;
    private static boolean initialized;
    private static long initializedDevice;
    private static final Map<Integer, Boolean> featureSupport = new HashMap<>();
    private static long supportCheckedDevice;

    private NgxInitializer() {
    }

    public static boolean initializeIfSupported() {
        return initializeIfSupported(NgxConstants.FEATURE_SUPER_SAMPLING);
    }

    public static boolean initializeIfSupported(int feature) {
        if (!isBindingAvailable() || !RenderSystems.isSupportVulkan()) {
            return false;
        }

        VulkanDevice vulkanDevice = RenderSystems.vulkan().device();
        if (vulkanDevice == null) {
            return false;
        }

        synchronized (INIT_LOCK) {
            long deviceHandle = vulkanDevice.getVkDevice().address();
            if (initialized
                    && initializedDevice == deviceHandle
                    && supportCheckedDevice == deviceHandle
                    && featureSupport.containsKey(feature)) {
                return featureSupport.get(feature);
            }

            if (!loadBinding()) {
                return false;
            }

            if (!initialized || initializedDevice != deviceHandle) {
                try {
                    LargeStackExecutor.run(
                            "SR-DLSS-NGX-Init",
                            () -> initializeForDevice(vulkanDevice, createFeatureInfo())
                    );
                } catch (RuntimeException e) {
                    shutdownLocked(true);
                    SuperResolution.LOGGER.info(
                            "Skipping NGX initialization because the current GPU could not initialize feature {}",
                            feature,
                            e
                    );
                    return false;
                }
            }

            if (supportCheckedDevice != deviceHandle) {
                featureSupport.clear();
                supportCheckedDevice = deviceHandle;
            }
            Boolean cachedSupport = featureSupport.get(feature);
            if (cachedSupport != null) {
                return cachedSupport;
            }

            NgxFeatureRequirement requirements;
            try {
                requirements = getFeatureRequirements(vulkanDevice, createFeatureInfo(), feature);
            } catch (RuntimeException e) {
                featureSupport.put(feature, false);
                SuperResolution.LOGGER.info(
                        "Skipping NGX feature {} because compatibility could not be queried for this GPU",
                        feature,
                        e
                );
                return false;
            }

            boolean supported = requirements.featureSupported == 0;
            featureSupport.put(feature, supported);
            if (!supported) {
                SuperResolution.LOGGER.info(
                        "Skipping NGX feature {} for this GPU. Feature support mask: {}, minimum GPU architecture: {}, minimum OS version: {}",
                        feature,
                        requirements.featureSupported,
                        requirements.minHardwareArchitecture,
                        requirements.minOsVersion
                );
            }
            return supported;
        }
    }

    public static void shutdown() {
        synchronized (INIT_LOCK) {
            shutdownLocked(false);
        }
    }

    private static NgxFeatureRequirement getFeatureRequirements(
            VulkanDevice vulkanDevice,
            NgxFeatureCommonInfo featureInfo,
            int feature
    ) {
        NgxFeatureDiscoveryInfo discoveryInfo = new NgxFeatureDiscoveryInfo();
        discoveryInfo.feature = feature;
        discoveryInfo.identifier.projectId = PROJECT_ID;
        discoveryInfo.identifier.engineType = NgxConstants.ENGINE_CUSTOM;
        discoveryInfo.identifier.engineVersion = ENGINE_VERSION;
        discoveryInfo.applicationDataPath = SuperResolutionConstants.DATA_DIR.getPath().toAbsolutePath().toString();
        discoveryInfo.featureInfo = featureInfo;

        NgxFeatureRequirement requirements = new NgxFeatureRequirement();
        int result = NgxVulkan.getFeatureRequirements(
                RenderSystems.vulkan().getVulkanInstance().address(),
                vulkanDevice.getPhysicalDevice().address(),
                discoveryInfo,
                requirements
        );
        requireSuccess("NVSDK_NGX_VULKAN_GetFeatureRequirements", result);
        return requirements;
    }

    private static NgxFeatureCommonInfo createFeatureInfo() {
        NgxFeatureCommonInfo featureInfo = new NgxFeatureCommonInfo();
        featureInfo.featurePaths = new String[]{
                SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath().toAbsolutePath().toString()
        };
        featureInfo.minimumLoggingLevel = NgxConstants.LOGGING_ON;
        featureInfo.loggingCallback = (message, loggingLevel, sourceFeature) ->
                SuperResolution.LOGGER.info(
                        "NGX [{}:{}] {}",
                        sourceFeatureToString(sourceFeature),
                        loggingLevelToString(loggingLevel),
                        message == null ? "" : message.stripTrailing()
                );
        return featureInfo;
    }

    private static String sourceFeatureToString(int sourceFeature) {
        return switch (sourceFeature) {
            case NgxConstants.FEATURE_SUPER_SAMPLING -> "SUPER_SAMPLING";
            case NgxConstants.FEATURE_IMAGE_SIGNAL_PROCESSING -> "IMAGE_SIGNAL_PROCESSING";
            case NgxConstants.FEATURE_DEEP_RESOLVE -> "DEEP_RESOLVE";
            case NgxConstants.FEATURE_FRAME_GENERATION -> "FRAME_GENERATION";
            case NgxConstants.FEATURE_RAY_RECONSTRUCTION -> "RAY_RECONSTRUCTION";
            default -> "UNKNOWN";
        };
    }

    private static String loggingLevelToString(int loggingLevel) {
        return switch (loggingLevel) {
            case NgxConstants.LOGGING_OFF -> "OFF";
            case NgxConstants.LOGGING_ON -> "ON";
            case NgxConstants.LOGGING_VERBOSE -> "VERBOSE";
            case NgxConstants.LOGGING_NUM -> "NUM";
            default -> "UNKNOWN";
        };
    }

    private static void initializeForDevice(VulkanDevice vulkanDevice, NgxFeatureCommonInfo featureInfo) {
        long deviceHandle = vulkanDevice.getVkDevice().address();
        if (initialized && initializedDevice == deviceHandle) {
            return;
        }
        if (initialized) {
            shutdownLocked(false);
        }

        int result = NgxVulkan.initWithProjectId(
                PROJECT_ID,
                NgxConstants.ENGINE_CUSTOM,
                ENGINE_VERSION,
                SuperResolutionConstants.DATA_DIR.getPath().toAbsolutePath().toString(),
                RenderSystems.vulkan().getVulkanInstance().address(),
                vulkanDevice.getPhysicalDevice().address(),
                deviceHandle,
                VkReflectionHelper.getVkGetInstanceProcAddr(),
                vulkanDevice.getVkDevice().getCapabilities().vkGetDeviceProcAddr,
                featureInfo,
                NgxConstants.VERSION_API
        );
        requireSuccess("NVSDK_NGX_VULKAN_Init_with_ProjectID", result);
        initialized = true;
        initializedDevice = deviceHandle;
    }

    private static boolean loadBinding() {
        if (bindingLoaded) {
            return true;
        }
        try {
            System.load(
                    NativeLibManager.LIB_SUPER_RESOLUTION_NGX
                            .getTargetPath(SuperResolutionConstants.NATIVE_LIBRARIES_DIR.getPath())
                            .toAbsolutePath()
                            .toString()
            );
            bindingLoaded = true;
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    private static void shutdownLocked(boolean force) {
        if (bindingLoaded && (initialized || force)) {
            int shutdownResult = NgxVulkan.shutdown();
            if (!NgxConstants.succeeded(shutdownResult)) {
                SuperResolution.LOGGER.warn("Failed to shut down NGX. Result: {}", shutdownResult);
            }
        }
        initialized = false;
        initializedDevice = 0L;
        featureSupport.clear();
        supportCheckedDevice = 0L;
    }

    private static boolean isBindingAvailable() {
        return NativeLibManager.LIB_SUPER_RESOLUTION_NGX != null;
    }

    private static void requireSuccess(String operation, int result) {
        if (!NgxConstants.succeeded(result)) {
            throw new IllegalStateException(operation + " failed. NGX result: " + result);
        }
    }
}
