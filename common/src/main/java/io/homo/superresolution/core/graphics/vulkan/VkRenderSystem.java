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

import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGeneration;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.core.graphics.GraphicsDevice;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
import io.homo.superresolution.core.streamline.Streamline;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTPrivateData.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PRIVATE_DATA_FEATURES_EXT;
import static org.lwjgl.vulkan.KHRDynamicRendering.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRDynamicRenderingLocalRead.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_LOCAL_READ_FEATURES_KHR;

import static org.lwjgl.vulkan.KHRShaderIntegerDotProduct.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_INTEGER_DOT_PRODUCT_FEATURES_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES;

public class VkRenderSystem implements IRenderSystem {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution/Vulkan");
    public static final boolean ENABLE_VALIDATION = VulkanValidationLayers.checkValidationLayerSupport() &&(
             Platform.currentPlatform.isDevelopmentEnvironment() ||
                      SuperResolutionConfig.isEnableDebug()
    );
    private static final int DEFAULT_API_VERSION = VK_API_VERSION_1_2;

    private final List<String> instanceExtensions = new ArrayList<>();
    private final List<String> deviceExtensions = new ArrayList<>();
    protected VulkanValidationLayers validationLayers;
    protected VkInstance instance;
    protected VulkanCapabilities capabilities = new VulkanCapabilities();
    private VulkanDevice vulkanDevice;
    private boolean borrowed;

    public VkRenderSystem() {
    }

    public static VkRenderSystem borrowed(VkInstance instance, VkPhysicalDevice physicalDevice, VkDevice device, int graphicsQueueFamilyIndex) {
        VkRenderSystem renderSystem = new VkRenderSystem();
        renderSystem.borrowed = true;
        renderSystem.instance = instance;
        renderSystem.capabilities.init(instance, physicalDevice);
        renderSystem.vulkanDevice = new VulkanDevice(instance, physicalDevice, device, graphicsQueueFamilyIndex, false);
        LOGGER.info("Borrowed Vulkan initialization completed");
        return renderSystem;
    }

    private static PointerBuffer asPointerBuffer(MemoryStack stack, List<String> list) {
        PointerBuffer buffer = stack.mallocPointer(list.size());
        list.forEach(e -> buffer.put(stack.UTF8(e)));
        return buffer.rewind();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static String uuidListToHex(List<byte[]> uuids) {
        List<String> uuidStrings = new ArrayList<>(uuids.size());
        for (byte[] uuid : uuids) {
            uuidStrings.add(bytesToHex(uuid));
        }
        return String.join(", ", uuidStrings);
    }

    public VkInstance getVulkanInstance() {
        return instance;
    }

    public VkRenderSystem addInstanceExtension(String ext) {
        if (!instanceExtensions.contains(ext)) {
            instanceExtensions.add(ext);
        }
        return this;
    }

    public VkRenderSystem addDeviceExtension(String ext) {
        if (!deviceExtensions.contains(ext)) {
            deviceExtensions.add(ext);
        }
        return this;
    }

    public VulkanCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public void initRenderSystem() {
        VulkanPresentationFeature.prepare(this);
        createInstance();
        VulkanPresentationFeature.createSurface(this);
        validationLayers = new VulkanValidationLayers(instance);
        if (ENABLE_VALIDATION) {
            validationLayers.setupDebugMessenger();
        }
        VkPhysicalDevice physicalDevice = selectPhysicalDevice();
        capabilities.init(instance, physicalDevice);
        VulkanPresentationFeature.validateDevice(this, physicalDevice);
        this.vulkanDevice = createLogicalDeviceWithCapabilities(physicalDevice);
        VulkanPresentationFeature.completeInitialization(this);
        LOGGER.info("Vulkan initialization completed");
    }

    @Override
    public void destroyRenderSystem() {
        if (vulkanDevice != null) {
            vulkanDevice.destroy();
            if (vulkanDevice.ownsVkDevice()) {
                vkDestroyDevice(vulkanDevice.getVkDevice(), null);
            }
            vulkanDevice = null;
        }
        if (validationLayers != null) {
            validationLayers.destroy();
            validationLayers = null;
        }
        if (instance != null) {
            if (!borrowed) {
                vkDestroyInstance(instance, null);
            }
            instance = null;
        }
        if (capabilities != null) {
            capabilities.destroy();
            capabilities = null;
        }
        LOGGER.info("Vulkan destroyed");
    }

    @Override
    public VulkanDevice device() {
        return vulkanDevice;
    }

    @Override
    public void finish() {
        vkDeviceWaitIdle(vulkanDevice.getVkDevice());
    }

    private void createInstance() {
        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .apiVersion(DEFAULT_API_VERSION)
                    .pEngineName(memUTF8("Engine"))
                    .engineVersion(VK_MAKE_VERSION(0, 1, 0))
                    .pApplicationName(memUTF8("App"))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0));

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(asPointerBuffer(stack, instanceExtensions));

            if (ENABLE_VALIDATION) {
                createInfo.ppEnabledLayerNames(
                        VulkanValidationLayers.getValidationLayersPointerBuffer(stack)
                );
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                VulkanValidationLayers.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer instancePtr = stack.mallocPointer(1);
            if (Streamline.isInitialized()){
                long instance = Streamline.createVkInstance(createInfo.address());
                VK_CHECK(Streamline.getLastVkResult(), "Failed to create VkInstance");
                instancePtr.put(0, instance);
            }else {
                VK_CHECK(vkCreateInstance(createInfo, null, instancePtr), "Failed to create VkInstance");
            }
            instance = VkReflectionHelper.createVkInstanceSafely(instancePtr.get(0), createInfo);
        }
    }

    private VkPhysicalDevice selectPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            VK_CHECK(vkEnumeratePhysicalDevices(instance, deviceCount, null));
            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("No Vulkan-compatible GPU found");
            }
            PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
            VK_CHECK(vkEnumeratePhysicalDevices(instance, deviceCount, devices));
            List<GraphicsDevice> graphicsDevices = new ArrayList<>();
            GraphicsDevice openglDevice = GraphicsDevice.createFromOpenGL();
            LOGGER.info("OpenGL device: {} (Device UUIDs: {}, Driver UUID: {})",
                    openglDevice.deviceName(),
                    uuidListToHex(openglDevice.deviceUUIDs()),
                    bytesToHex(openglDevice.driverUUID())
            );
            for (int i = 0; i < deviceCount.get(0); i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(devices.get(i), instance);
                graphicsDevices.add(GraphicsDevice.createFromVulkan(physicalDevice));
            }
            LOGGER.info("Detected {} Vulkan physical device(s):", graphicsDevices.size());
            for (int i = 0; i < deviceCount.get(0); i++) {
                GraphicsDevice device = graphicsDevices.get(i);
                LOGGER.info("[{}] {} (Device UUIDs: {}, Driver UUID: {})",
                        i,
                        device.deviceName(),
                        uuidListToHex(device.deviceUUIDs()),
                        bytesToHex(device.driverUUID())
                );
            }

            for (int i = 0; i < deviceCount.get(0); i++) {
                if (graphicsDevices.get(i).isCompatibleWith(openglDevice)) {
                    return new VkPhysicalDevice(devices.get(i), instance);
                }
            }
            LOGGER.error("No Vulkan physical device matches both the current OpenGL device and driver UUID; defaulting to the first device");
            return new VkPhysicalDevice(devices.get(0), instance);
        }
    }

    private VulkanDevice createLogicalDeviceWithCapabilities(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            int graphicsFamilyIndex = VulkanPresentationFeature.findGraphicsPresentQueueFamily(
                    stack,
                    VK_QUEUE_GRAPHICS_BIT,
                    physicalDevice
            );
            if (graphicsFamilyIndex == -1) {
                throw new RuntimeException("No suitable queue family found");
            }

            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(
                    physicalDevice,
                    queueFamilyCount,
                    null
            );
            VkQueueFamilyProperties.Buffer queueFamilyProperties =
                    VkQueueFamilyProperties.calloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(
                    physicalDevice,
                    queueFamilyCount,
                    queueFamilyProperties
            );
            int availableGraphicsQueueCount =
                    queueFamilyProperties.get(graphicsFamilyIndex).queueCount();
            FrameGeneration.StartupProviderSelection startupProvider =
                    FrameGeneration.startupProviderSelection();
            boolean asyncDispatchRequested = startupProvider.applicationManagedAsync();

            List<String> enableDeviceExts = new ArrayList<>();
            List<String> supportedDeviceExts = capabilities.getDeviceExtensions();
            for (String ext : deviceExtensions) {
                if (supportedDeviceExts.contains(ext)) {
                    enableDeviceExts.add(ext);
                    LOGGER.info("Enabling device extension: {}", ext);
                } else {
                    LOGGER.warn("Extension {} is not supported by the current physical device; skipping it", ext);
                }
            }

            VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT mutableDescriptorTypeFeaturesEXT =
                    VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT);
            VkPhysicalDevicePrivateDataFeaturesEXT privateDataFeatures =
                    VkPhysicalDevicePrivateDataFeaturesEXT.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PRIVATE_DATA_FEATURES_EXT);
            VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR shaderIntegerDotProductFeaturesKHR =
                    VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_INTEGER_DOT_PRODUCT_FEATURES_KHR)
                            .pNext(privateDataFeatures.address());
            mutableDescriptorTypeFeaturesEXT.pNext(shaderIntegerDotProductFeaturesKHR.address());

            VkPhysicalDeviceDynamicRenderingFeaturesKHR dynamicRenderingFeatures =
                    VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES_KHR)
                            .pNext(mutableDescriptorTypeFeaturesEXT.address());

            VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR dynamicRenderingLocalReadFeatures =
                    VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_LOCAL_READ_FEATURES_KHR)
                            .pNext(dynamicRenderingFeatures.address());

            boolean hasOpticalFlowExtension = supportedDeviceExts.contains("VK_NV_optical_flow");
            boolean hasSynchronization2Extension = supportedDeviceExts.contains("VK_KHR_synchronization2");
            boolean hasPresentIdExtension = enableDeviceExts.contains(KHRPresentId.VK_KHR_PRESENT_ID_EXTENSION_NAME);
            boolean hasLowLatency2Extension = enableDeviceExts.contains(NVLowLatency2.VK_NV_LOW_LATENCY_2_EXTENSION_NAME);
            VkPhysicalDeviceOpticalFlowFeaturesNV opticalFlowFeatures =
                    VkPhysicalDeviceOpticalFlowFeaturesNV.calloc(stack)
                            .sType(NVOpticalFlow.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_OPTICAL_FLOW_FEATURES_NV);
            VkPhysicalDeviceSynchronization2FeaturesKHR synchronization2Features =
                    VkPhysicalDeviceSynchronization2FeaturesKHR.calloc(stack)
                            .sType(KHRSynchronization2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SYNCHRONIZATION_2_FEATURES_KHR);
            VkPhysicalDevicePresentIdFeaturesKHR presentIdFeatures =
                    VkPhysicalDevicePresentIdFeaturesKHR.calloc(stack)
                            .sType(KHRPresentId.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PRESENT_ID_FEATURES_KHR);
            long featureQueryChain = dynamicRenderingLocalReadFeatures.address();
            if (hasSynchronization2Extension) {
                synchronization2Features.pNext(featureQueryChain);
                featureQueryChain = synchronization2Features.address();
            }
            if (hasOpticalFlowExtension) {
                opticalFlowFeatures.pNext(featureQueryChain);
                featureQueryChain = opticalFlowFeatures.address();
            }
            if (hasPresentIdExtension) {
                presentIdFeatures.pNext(featureQueryChain);
                featureQueryChain = presentIdFeatures.address();
            }

            VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                    .pNext(featureQueryChain);

            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(features12.address());

            vkGetPhysicalDeviceFeatures2(physicalDevice, features2);

            boolean deviceSupportsMutableDescriptor = mutableDescriptorTypeFeaturesEXT.mutableDescriptorType();
            boolean deviceSupportsShaderInt8 = features12.shaderInt8();
            boolean deviceSupportsShaderInt16 = features2.features().shaderInt16();
            boolean deviceSupportsShaderFloat16 = features12.shaderFloat16();
            boolean deviceSupportsShaderIntegerDotProduct = shaderIntegerDotProductFeaturesKHR.shaderIntegerDotProduct();
            boolean deviceSupportsShaderStorageImageWriteWithoutFormat = features2.features().shaderStorageImageWriteWithoutFormat();
            boolean deviceSupportsBufferDeviceAddress = features12.bufferDeviceAddress();
            boolean deviceSupportsDescriptorIndexing = features12.descriptorIndexing();
            boolean deviceSupportsDynamicRendering = dynamicRenderingFeatures.dynamicRendering();
            boolean deviceSupportsDynamicRenderingLocalRead = dynamicRenderingLocalReadFeatures.dynamicRenderingLocalRead();
            boolean deviceSupportsPrivateData = privateDataFeatures.privateData();
            boolean deviceSupportsOpticalFlow = hasOpticalFlowExtension && opticalFlowFeatures.opticalFlow();
            boolean deviceSupportsSynchronization2 =
                    hasSynchronization2Extension && synchronization2Features.synchronization2();
            boolean deviceSupportsTimelineSemaphore = features12.timelineSemaphore();
            boolean deviceSupportsPresentId = hasPresentIdExtension && presentIdFeatures.presentId();
            LOGGER.info("Vulkan device feature support:");
            LOGGER.info("  mutableDescriptorType: {}", deviceSupportsMutableDescriptor);
            LOGGER.info("  shaderInt8: {}", deviceSupportsShaderInt8);
            LOGGER.info("  shaderInt16: {}", deviceSupportsShaderInt16);
            LOGGER.info("  shaderFloat16: {}", deviceSupportsShaderFloat16);
            LOGGER.info("  shaderStorageImageWriteWithoutFormat: {}", deviceSupportsShaderStorageImageWriteWithoutFormat);
            LOGGER.info("  shaderIntegerDotProduct: {}", deviceSupportsShaderIntegerDotProduct);
            LOGGER.info("  bufferDeviceAddress: {}", deviceSupportsBufferDeviceAddress);
            LOGGER.info("  descriptorIndexing: {}", deviceSupportsDescriptorIndexing);
            LOGGER.info("  dynamicRendering: {}", deviceSupportsDynamicRendering);
            LOGGER.info("  dynamicRenderingLocalRead: {}",deviceSupportsDynamicRenderingLocalRead);
            LOGGER.info("  privateData: {}", deviceSupportsPrivateData);
            LOGGER.info("  opticalFlow: {}", deviceSupportsOpticalFlow);
            LOGGER.info("  synchronization2: {}", deviceSupportsSynchronization2);
            LOGGER.info("  timelineSemaphore: {}", deviceSupportsTimelineSemaphore);
            LOGGER.info("  presentId: {}", deviceSupportsPresentId);

            boolean createFrameGenerationQueue = asyncDispatchRequested
                    && availableGraphicsQueueCount >= 2
                    && deviceSupportsTimelineSemaphore;
            boolean createPresentQueue = createFrameGenerationQueue
                    && availableGraphicsQueueCount >= 3;
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos =
                    VkDeviceQueueCreateInfo.calloc(1, stack);
            queueCreateInfos.get(0)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsFamilyIndex)
                    .pQueuePriorities(createPresentQueue
                            ? stack.floats(1.0f, 1.0f, 1.0f)
                            : createFrameGenerationQueue
                            ? stack.floats(1.0f, 1.0f)
                            : stack.floats(1.0f));

            VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT deviceMutableFeatures =
                    VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT)
                            .mutableDescriptorType(deviceSupportsMutableDescriptor);
            VkPhysicalDevicePrivateDataFeaturesEXT devicePrivateDataFeatures =
                    VkPhysicalDevicePrivateDataFeaturesEXT.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PRIVATE_DATA_FEATURES_EXT)
                            .privateData(deviceSupportsPrivateData);

            VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR deviceShaderIntFeatures =
                    VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_INTEGER_DOT_PRODUCT_FEATURES_KHR)
                            .pNext(devicePrivateDataFeatures.address())
                            .shaderIntegerDotProduct(deviceSupportsShaderIntegerDotProduct);
            deviceMutableFeatures.pNext(deviceShaderIntFeatures.address());

            VkPhysicalDeviceDynamicRenderingFeaturesKHR deviceDynamicRenderingFeatures =
                    VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES_KHR)
                            .pNext(deviceMutableFeatures.address())
                            .dynamicRendering(deviceSupportsDynamicRendering);
            VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR deviceDynamicRenderingLocalReadFeatures =
                    VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_LOCAL_READ_FEATURES_KHR)
                            .dynamicRenderingLocalRead(deviceSupportsDynamicRenderingLocalRead)
                            .pNext(deviceDynamicRenderingFeatures.address());

            long deviceFeatureChain = deviceDynamicRenderingLocalReadFeatures.address();
            if (deviceSupportsSynchronization2) {
                VkPhysicalDeviceSynchronization2FeaturesKHR deviceSynchronization2Features =
                        VkPhysicalDeviceSynchronization2FeaturesKHR.calloc(stack)
                                .sType(KHRSynchronization2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SYNCHRONIZATION_2_FEATURES_KHR)
                                .synchronization2(true)
                                .pNext(deviceFeatureChain);
                deviceFeatureChain = deviceSynchronization2Features.address();
            }
            if (deviceSupportsOpticalFlow) {
                VkPhysicalDeviceOpticalFlowFeaturesNV deviceOpticalFlowFeatures =
                        VkPhysicalDeviceOpticalFlowFeaturesNV.calloc(stack)
                                .sType(NVOpticalFlow.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_OPTICAL_FLOW_FEATURES_NV)
                                .opticalFlow(true)
                                .pNext(deviceFeatureChain);
                deviceFeatureChain = deviceOpticalFlowFeatures.address();
            }
            if (deviceSupportsPresentId) {
                VkPhysicalDevicePresentIdFeaturesKHR devicePresentIdFeatures =
                        VkPhysicalDevicePresentIdFeaturesKHR.calloc(stack)
                                .sType(KHRPresentId.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PRESENT_ID_FEATURES_KHR)
                                .presentId(true)
                                .pNext(deviceFeatureChain);
                deviceFeatureChain = devicePresentIdFeatures.address();
            }

            VkPhysicalDeviceVulkan12Features deviceFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                    .pNext(deviceFeatureChain)
                    .shaderFloat16(deviceSupportsShaderFloat16)
                    .shaderInt8(deviceSupportsShaderInt8)
                    .bufferDeviceAddress(deviceSupportsBufferDeviceAddress)
                    .timelineSemaphore(deviceSupportsTimelineSemaphore)
                    .descriptorIndexing(deviceSupportsDescriptorIndexing);

            VkPhysicalDeviceFeatures2 deviceFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(deviceFeatures12.address());
            deviceFeatures2.features().shaderInt16(deviceSupportsShaderInt16);
            deviceFeatures2.features().shaderStorageImageWriteWithoutFormat(deviceSupportsShaderStorageImageWriteWithoutFormat);
            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pNext(deviceFeatures2.address())
                    .pQueueCreateInfos(queueCreateInfos)
                    .ppEnabledExtensionNames(asPointerBuffer(stack, enableDeviceExts))
                    .pEnabledFeatures(null);

            //https://docs.vulkan.org/refpages/latest/refpages/source/VkDeviceCreateInfo.html#:~:text=//%20ppEnabledLayerNames%20is%20legacy%20and%20not%20used
            //if (ENABLE_VALIDATION) {
            //    createInfo.ppEnabledLayerNames(VulkanValidationLayers.getValidationLayersPointerBuffer(stack));
            //}

            PointerBuffer pDevice = stack.mallocPointer(1);
            if (Streamline.isInitialized()){
                long device = Streamline.createVkDevice(
                        getVulkanInstance().address(),
                        physicalDevice.address(),
                        createInfo.address()
                );
                VK_CHECK(Streamline.getLastVkResult(), "Failed to create logical device");
                pDevice.put(0, device);
            }else {
                VK_CHECK(vkCreateDevice(physicalDevice, createInfo, null, pDevice),
                        "Failed to create logical device");
            }

            VkDevice logicalDevice = new VkDevice(pDevice.get(0), physicalDevice, createInfo);
            boolean timelineSemaphoreEnabled = deviceSupportsTimelineSemaphore
                    && logicalDevice.getCapabilities().vkSignalSemaphore != VK_NULL_HANDLE
                    && logicalDevice.getCapabilities().vkWaitSemaphores != VK_NULL_HANDLE
                    && logicalDevice.getCapabilities().vkGetSemaphoreCounterValue != VK_NULL_HANDLE;
            VulkanAsyncDispatchCapabilities asyncDispatchCapabilities =
                    VulkanAsyncDispatchCapabilities.evaluate(
                            startupProvider.providerId(),
                            asyncDispatchRequested,
                            graphicsFamilyIndex,
                            availableGraphicsQueueCount,
                            deviceSupportsTimelineSemaphore,
                            timelineSemaphoreEnabled,
                            createFrameGenerationQueue,
                            createPresentQueue
                    );
            VulkanDevice vulkanDevice = new VulkanDevice(
                    instance,
                    physicalDevice,
                    logicalDevice,
                    graphicsFamilyIndex,
                    true,
                    createFrameGenerationQueue,
                    createPresentQueue,
                    asyncDispatchCapabilities
            );
            LOGGER.info(
                    "Vulkan main queue: handle=0x{}, family={}, index={}",
                    Long.toHexString(vulkanDevice.getMainQueue().getQueue().address()),
                    vulkanDevice.getMainQueue().getQueueFamilyIndex(),
                    vulkanDevice.getMainQueue().getQueueIndex()
            );
            if (vulkanDevice.getFrameGenerationQueue() != null) {
                LOGGER.info(
                        "Vulkan FG queue: handle=0x{}, family={}, index={}",
                        Long.toHexString(
                                vulkanDevice.getFrameGenerationQueue().getQueue().address()
                        ),
                        vulkanDevice.getFrameGenerationQueue().getQueueFamilyIndex(),
                        vulkanDevice.getFrameGenerationQueue().getQueueIndex()
                );
            }
            if (vulkanDevice.getDedicatedPresentQueue() != null) {
                LOGGER.info(
                        "Vulkan Present queue: handle=0x{}, family={}, index={}",
                        Long.toHexString(
                                vulkanDevice.getDedicatedPresentQueue().getQueue().address()
                        ),
                        vulkanDevice.getDedicatedPresentQueue().getQueueFamilyIndex(),
                        vulkanDevice.getDedicatedPresentQueue().getQueueIndex()
                );
            }
            if (asyncDispatchRequested
                    && vulkanDevice.asyncDispatchCapabilities().available()
                    && !vulkanDevice.hasDedicatedPresentQueue()) {
                LOGGER.warn(
                        "Vulkan application-managed present queue was not created; "
                                + "falling back to main queue (family={}, index={}) because "
                                + "the selected family exposes only {} queue(s)",
                        vulkanDevice.getMainQueue().getQueueFamilyIndex(),
                        vulkanDevice.getMainQueue().getQueueIndex(),
                        availableGraphicsQueueCount
                );
            }
            if (asyncDispatchRequested
                    && !vulkanDevice.asyncDispatchCapabilities().available()) {
                LOGGER.warn(
                        "Frame generation provider '{}' is unavailable: {}",
                        startupProvider.providerId(),
                        vulkanDevice.asyncDispatchCapabilities().unavailableReason()
                );
            }
            // Native Reflex (VK_NV_low_latency2) needs present ids and timeline
            // semaphores; when Streamline is initialized its interposer owns Reflex,
            // so the raw path stays dormant there.
            VulkanLowLatency.onDeviceCreated(
                    vulkanDevice,
                    hasLowLatency2Extension
                            && deviceSupportsPresentId
                            && deviceSupportsTimelineSemaphore
                            && !Streamline.isInitialized()
            );
            return vulkanDevice;
        }
    }

}
