package io.homo.superresolution.core.graphics.vulkan.utils;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.impl.Destroyable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.Set;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanValidationLayers implements Destroyable {
    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanValidationLayers.class);
    private static final Set<String> REQUIRED_LAYERS = Collections.singleton("VK_LAYER_KHRONOS_validation");

    private final VkInstance instance;
    private long debugMessenger;

    public VulkanValidationLayers(VkInstance instance) {
        this.instance = instance;
    }

    public static boolean checkValidationLayerSupport() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer layerCount = stack.ints(0);
            VK_CHECK(vkEnumerateInstanceLayerProperties(layerCount, null));

            VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);
            VK_CHECK(vkEnumerateInstanceLayerProperties(layerCount, availableLayers));

            return availableLayers.stream()
                    .map(VkLayerProperties::layerNameString)
                    .anyMatch(REQUIRED_LAYERS::contains);
        }
    }

    public void setupDebugMessenger() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer pDebugMessenger = stack.mallocLong(1);
            VK_CHECK(createDebugUtilsMessengerEXT(instance, createInfo, pDebugMessenger), "Failed to set up debug messenger");
            debugMessenger = pDebugMessenger.get(0);
        }
    }

    public static void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT createInfo) {
        createInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                .pfnUserCallback(new VkDebugUtilsMessengerCallbackEXT() {
                    @Override
                    public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
                        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                        String message = callbackData.pMessageString();
                        StackTraceElement[] elements = SuperResolution.renderThread.getStackTrace();

                        if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                            LOGGER.error("[Vulkan Validation Error] {}", message);
                            for (StackTraceElement element : elements) {
                                LOGGER.error("    {}", element.toString());
                            }
                        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                            LOGGER.warn("[Vulkan Validation Warn] {}", message);
                            for (StackTraceElement element : elements) {
                                LOGGER.warn("    {}", element.toString());
                            }
                        } else {
                            LOGGER.info("[Vulkan Validation Debug] {}", message);
                            for (StackTraceElement element : elements) {
                                LOGGER.info("    {}", element.toString());
                            }
                        }

                        return VK_FALSE;
                    }
                });
    }


    private int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        String message = callbackData.pMessageString();

        if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            LOGGER.info("[Vulkan Validation Error] {}", message);
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            LOGGER.info("[Vulkan Validation Warn] {}", message);
        } else {
            LOGGER.info("[Vulkan Validation Debug] {}", message);
        }

        return VK_FALSE;
    }

    public static PointerBuffer getValidationLayersPointerBuffer(MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(REQUIRED_LAYERS.size());
        REQUIRED_LAYERS.forEach(layer -> buffer.put(stack.UTF8(layer)));
        return buffer.rewind();
    }

    @Override
    public void destroy() {
        if (debugMessenger != NULL) {
            destroyDebugUtilsMessengerEXT(instance, debugMessenger);
        }
    }

    private int createDebugUtilsMessengerEXT(VkInstance instance,
                                             VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                             LongBuffer pDebugMessenger) {
        long func = vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT");
        return func != NULL ?
                VK_CHECK(vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger)) :
                VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    private void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger) {
        long func = vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT");
        if (func != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
    }
}