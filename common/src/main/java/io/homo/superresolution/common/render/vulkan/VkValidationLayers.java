package io.homo.superresolution.common.render.vulkan;

import io.homo.superresolution.common.impl.Destroyable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkValidationLayers implements Destroyable {
    private static final Set<String> VALIDATION_LAYERS;

    static {
        VALIDATION_LAYERS = new HashSet<>();
        VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
    }

    private final VkApplication application;
    private long debugMessenger;

    public VkValidationLayers(VkApplication application) {
        this.application = application;
    }

    public static boolean checkValidationLayerSupport() {
        try(MemoryStack stack = stackPush()) {
            IntBuffer layerCount = stack.ints(0);
            vkEnumerateInstanceLayerProperties(layerCount, null);
            VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);
            vkEnumerateInstanceLayerProperties(layerCount, availableLayers);
            Set<String> availableLayerNames = availableLayers.stream()
                    .map(VkLayerProperties::layerNameString)
                    .collect(toSet());

            return availableLayerNames.containsAll(VALIDATION_LAYERS);
        }
    }

    public static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        String message = callbackData.pMessageString();
        switch (messageSeverity) {
            case VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT:
                VkApplication.LOGGER.warn("Validation layer: {}", message);
                break;
            case VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT:
                VkApplication.LOGGER.error("Validation layer: {}", message);
                break;
            default:
                VkApplication.LOGGER.info("Validation layer: {}", message);
                break;
        }
        return VK_FALSE;
    }

    private int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                             LongBuffer pDebugMessenger) {
        if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger);
        }
        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    private void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger) {
        if (vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
    }

    public void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(VkValidationLayers::debugCallback);
    }

    public void setupDebugMessenger() {
        try(MemoryStack stack = stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            populateDebugMessengerCreateInfo(createInfo);
            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);
            if(createDebugUtilsMessengerEXT(application.instance, createInfo, pDebugMessenger) != VK_SUCCESS) {
                throw new RuntimeException("Failed to set up debug messenger");
            }
            debugMessenger = pDebugMessenger.get(0);
        }
    }

    public PointerBuffer validationLayersAsPointerBuffer(MemoryStack stack) {

        PointerBuffer buffer = stack.mallocPointer(VALIDATION_LAYERS.size());

        VALIDATION_LAYERS.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }

    public long getDebugMessenger() {
        return debugMessenger;
    }

    @Override
    public void destroy() {
        destroyDebugUtilsMessengerEXT(application.instance,debugMessenger);
    }
}