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

import io.homo.superresolution.core.graphics.GraphicsDevice;
import io.homo.superresolution.core.graphics.system.IRenderSystem;
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
    public static final boolean ENABLE_VALIDATION = VulkanValidationLayers.checkValidationLayerSupport();
    private static final int DEFAULT_API_VERSION = VK_API_VERSION_1_2;

    private final List<String> instanceExtensions = new ArrayList<>();
    private final List<String> deviceExtensions = new ArrayList<>();
    protected VulkanValidationLayers validationLayers;
    protected VkInstance instance;
    protected VulkanCapabilities capabilities = new VulkanCapabilities();
    private VulkanDevice vulkanDevice;

    public VkRenderSystem() {
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

    public VkInstance getVulkanInstance() {
        return instance;
    }

    public VkRenderSystem addInstanceExtension(String ext) {
        instanceExtensions.add(ext);
        return this;
    }

    public VkRenderSystem addDeviceExtension(String ext) {
        deviceExtensions.add(ext);
        return this;
    }

    public VulkanCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public void initRenderSystem() {
        createInstance();
        validationLayers = new VulkanValidationLayers(instance);
        if (ENABLE_VALIDATION) {
            validationLayers.setupDebugMessenger();
        }
        VkPhysicalDevice physicalDevice = selectPhysicalDevice();
        capabilities.init(instance, physicalDevice);
        this.vulkanDevice = createLogicalDeviceWithCapabilities(physicalDevice);
        LOGGER.info("Vulkan 初始化完成");
    }

    @Override
    public void destroyRenderSystem() {
        if (vulkanDevice != null) {
            vulkanDevice.destroy();
            vkDestroyDevice(vulkanDevice.getVkDevice(), null);
            vulkanDevice = null;
        }
        if (validationLayers != null) {
            validationLayers.destroy();
            validationLayers = null;
        }
        if (instance != null) {
            vkDestroyInstance(instance, null);
            instance = null;
        }
        if (capabilities != null) {
            capabilities.destroy();
            capabilities = null;
        }
        LOGGER.info("Vulkan 已销毁");
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
            VK_CHECK(vkCreateInstance(createInfo, null, instancePtr), "Failed to create VkInstance");
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
            LOGGER.info("OpenGL 设备: {} (Device UUID: {}, Driver UUID: {})",
                    openglDevice.deviceName(),
                    bytesToHex(openglDevice.deviceUUID()),
                    bytesToHex(openglDevice.driverUUID())
            );
            for (int i = 0; i < deviceCount.get(0); i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(devices.get(i), instance);
                graphicsDevices.add(GraphicsDevice.createFromVulkan(physicalDevice));
            }
            LOGGER.info("检测到 {} 个 Vulkan 物理设备:", graphicsDevices.size());
            for (int i = 0; i < deviceCount.get(0); i++) {
                GraphicsDevice device = graphicsDevices.get(i);
                LOGGER.info("[{}] {} (Device UUID: {}, Driver UUID: {})",
                        i,
                        device.deviceName(),
                        bytesToHex(device.deviceUUID()),
                        bytesToHex(device.driverUUID())
                );
            }

            for (int i = 0; i < deviceCount.get(0); i++) {
                if (graphicsDevices.get(i).equals(openglDevice)) {
                    return new VkPhysicalDevice(devices.get(i), instance);
                }
            }
            LOGGER.error("未找到与当前 OpenGL 设备匹配的 Vulkan 物理设备，默认选择第一个设备");
            return new VkPhysicalDevice(devices.get(0), instance);
        }
    }

    private VulkanDevice createLogicalDeviceWithCapabilities(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {
            int graphicsFamilyIndex = VulkanQueueUtils.findQueueFamilyIndex(stack, VK_QUEUE_GRAPHICS_BIT, physicalDevice);
            if (graphicsFamilyIndex == -1) {
                throw new RuntimeException("No suitable queue family found");
            }

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);
            queueCreateInfos.get(0)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsFamilyIndex)
                    .pQueuePriorities(stack.floats(1.0f));

            List<String> enableDeviceExts = new ArrayList<>();
            List<String> supportedDeviceExts = capabilities.getDeviceExtensions();
            for (String ext : deviceExtensions) {
                if (supportedDeviceExts.contains(ext)) {
                    enableDeviceExts.add(ext);
                    LOGGER.info("启用设备扩展: {}", ext);
                } else {
                    LOGGER.warn("扩展 {} 不被当前物理设备支持，已跳过", ext);
                }
            }

            VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT mutableDescriptorTypeFeaturesEXT =
                    VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT);
            VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR shaderIntegerDotProductFeaturesKHR =
                    VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_INTEGER_DOT_PRODUCT_FEATURES_KHR)
                            .pNext(mutableDescriptorTypeFeaturesEXT.address());

            VkPhysicalDeviceDynamicRenderingFeaturesKHR dynamicRenderingFeatures =
                    VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES_KHR)
                            .pNext(shaderIntegerDotProductFeaturesKHR.address());

            VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR dynamicRenderingLocalReadFeatures =
                    VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_LOCAL_READ_FEATURES_KHR)
                            .pNext(dynamicRenderingFeatures.address());

            VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                    .pNext(dynamicRenderingLocalReadFeatures.address());

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
            LOGGER.info("Vulkan 设备特性支持状态:");
            LOGGER.info("  mutableDescriptorType: {}", deviceSupportsMutableDescriptor);
            LOGGER.info("  shaderInt8: {}", deviceSupportsShaderInt8);
            LOGGER.info("  shaderInt16: {}", deviceSupportsShaderInt16);
            LOGGER.info("  shaderFloat16: {}", deviceSupportsShaderFloat16);
            LOGGER.info("  shaderStorageImageWriteWithoutFormat: {}", deviceSupportsShaderFloat16);
            LOGGER.info("  shaderIntegerDotProduct: {}", deviceSupportsShaderIntegerDotProduct);
            LOGGER.info("  bufferDeviceAddress: {}", deviceSupportsBufferDeviceAddress);
            LOGGER.info("  descriptorIndexing: {}", deviceSupportsDescriptorIndexing);
            LOGGER.info("  dynamicRendering: {}", deviceSupportsDynamicRendering);
            LOGGER.info("  dynamicRenderingLocalRead: {}",deviceSupportsDynamicRenderingLocalRead);

            VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT deviceMutableFeatures =
                    VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT)
                            .mutableDescriptorType(deviceSupportsMutableDescriptor);

            VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR deviceShaderIntFeatures =
                    VkPhysicalDeviceShaderIntegerDotProductFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_INTEGER_DOT_PRODUCT_FEATURES_KHR)
                            .pNext(deviceMutableFeatures.address())
                            .shaderIntegerDotProduct(deviceSupportsShaderIntegerDotProduct);

            VkPhysicalDeviceDynamicRenderingFeaturesKHR deviceDynamicRenderingFeatures =
                    VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES_KHR)
                            .pNext(deviceShaderIntFeatures.address())
                            .dynamicRendering(deviceSupportsDynamicRendering);
            VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR deviceDynamicRenderingLocalReadFeatures =
                    VkPhysicalDeviceDynamicRenderingLocalReadFeaturesKHR.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_LOCAL_READ_FEATURES_KHR)
                            .dynamicRenderingLocalRead(deviceSupportsDynamicRenderingLocalRead)
                            .pNext(deviceDynamicRenderingFeatures.address());

            VkPhysicalDeviceVulkan12Features deviceFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                    .pNext(deviceDynamicRenderingLocalReadFeatures.address())
                    .shaderFloat16(deviceSupportsShaderFloat16)
                    .shaderInt8(deviceSupportsShaderInt8)
                    .bufferDeviceAddress(deviceSupportsBufferDeviceAddress)
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
            VK_CHECK(vkCreateDevice(physicalDevice, createInfo, null, pDevice),
                    "Failed to create logical device");
            return new VulkanDevice(
                    instance,
                    physicalDevice,
                    new VkDevice(pDevice.get(0), physicalDevice, createInfo),
                    graphicsFamilyIndex
            );
        }
    }

}
