package io.homo.superresolution.common.render.vulkan;

import io.homo.superresolution.common.impl.Destroyable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.IntStream;

import static io.homo.superresolution.common.render.vulkan.Utils.asPointerBuffer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VkDeviceManager implements Destroyable {
    public final VkApplication application;
    public VkPhysicalDevice physicalDevice;
    public VkDevice device;
    public VkQueue graphicsQueue;
    public long textureSampler;
    public long descriptorPool;
    public VkPhysicalDeviceProperties physicalDeviceProperties;
    public VkPhysicalDeviceMemoryProperties deviceMemoryProperties;
    public QueueFamilyIndices queueFamilyIndices;

    public VkDeviceManager(VkApplication application) {
        this.application = application;
    }

    public ArrayList<String> getRequiredExtensions() {
        return application.deviceRequiredExtensions;
    }

    public void init() {
        pickPhysicalDevice();
        createLogicalDevice();
        createTextureSampler();
        createDescriptorPool();
    }

    private void createDescriptorPool() {
        int POOL_COUNT = 1000;
        VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.create(11);
        poolSize.put(0, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_SAMPLER).descriptorCount(POOL_COUNT));
        poolSize.put(1, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(POOL_COUNT));
        poolSize.put(2, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE).descriptorCount(POOL_COUNT));
        poolSize.put(3, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE).descriptorCount(POOL_COUNT));
        poolSize.put(4, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER).descriptorCount(POOL_COUNT));
        poolSize.put(5, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER).descriptorCount(POOL_COUNT));
        poolSize.put(6, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(POOL_COUNT));
        poolSize.put(7, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(POOL_COUNT));
        poolSize.put(8, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC).descriptorCount(POOL_COUNT));
        poolSize.put(9, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC).descriptorCount(POOL_COUNT));
        poolSize.put(10, VkDescriptorPoolSize.calloc().type(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT).descriptorCount(POOL_COUNT));
        int NUM_POOLS = poolSize.capacity();
        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc();
        poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
        poolInfo.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT);
        poolInfo.maxSets(POOL_COUNT * NUM_POOLS);
        poolInfo.pPoolSizes(poolSize);
        LongBuffer ptr = MemoryStack.stackCallocLong(1);
        vkCreateDescriptorPool(device, poolInfo, null, ptr);
        descriptorPool = ptr.get(0);
    }

    private void createTextureSampler() {
        VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc();
        samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
        samplerInfo.magFilter(VK_FILTER_LINEAR);
        samplerInfo.minFilter(VK_FILTER_LINEAR);
        samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
        samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
        samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
        samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK);
        samplerInfo.unnormalizedCoordinates(false);
        samplerInfo.compareEnable(false);
        samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);
        samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
        LongBuffer ptr = MemoryStack.stackCallocLong(1);
        vkCreateSampler(device, samplerInfo, null, ptr);
        textureSampler = ptr.get(0);
    }

    private void pickPhysicalDevice() {

        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(application.instance, deviceCount, null);
            if (deviceCount.get(0) == 0) {
                throw new VkException("Failed to find GPUs with Vulkan support");
            }
            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(application.instance, deviceCount, ppPhysicalDevices);
            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), application.instance);
                if (isDeviceSuitable(device)) {
                    physicalDevice = device;
                    return;
                }
            }

            throw new VkException("Failed to find a suitable GPU");
        }
    }

    private void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {
            QueueFamilyIndices indices = findQueueFamilies(physicalDevice);
            int[] uniqueQueueFamilies = indices.unique();
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);
            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            //VkPhysicalDeviceVulkan12Features deviceFeatures12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
            //        .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
            //        .shaderFloat16(true); // Enable shaderFloat16 feature
            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            //createInfo.pNext(deviceFeatures12);
            createInfo.ppEnabledExtensionNames(asPointerBuffer(stack, getRequiredExtensions()));
            if (VkApplication.ENABLE_VALIDATION)createInfo.ppEnabledLayerNames(application.validationLayers.validationLayersAsPointerBuffer(stack));

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);
            if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new VkException("Failed to create logical device");
            }
            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            physicalDeviceProperties = VkPhysicalDeviceProperties.calloc();
            deviceMemoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
            vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, deviceMemoryProperties);

            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, indices.graphicsFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);
            queueFamilyIndices = indices;
        }
    }

    private boolean isDeviceSuitable(VkPhysicalDevice device) {
        QueueFamilyIndices indices = findQueueFamilies(device);
        return indices.isComplete() && checkDeviceExtensionSupport(device);
    }

    protected QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        QueueFamilyIndices indices = new QueueFamilyIndices();
        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);
            IntStream.range(0, queueFamilies.capacity())
                    .filter(index -> (queueFamilies.get(index).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
                    .findFirst()
                    .ifPresent(index -> indices.graphicsFamily = index);
            return indices;
        }
    }

    private boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer extensionCount = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);
            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);
            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions);
            ArrayList<String> extensions = new ArrayList<>();
            for (Iterator<VkExtensionProperties> it = availableExtensions.stream().iterator(); it.hasNext(); ) {
                VkExtensionProperties extension = it.next();
                extensions.add(extension.extensionNameString());
            }
            for (String requiredExtension : getRequiredExtensions()) {
                if (!extensions.contains(requiredExtension)) {
                    return false;
                }
            }
            return true;
        }
    }

    private int findMemoryType(MemoryStack stack, int typeFilter, int properties) {

        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable memory type");
    }

    public void createBuffer(long size, int usage, int properties, LongBuffer pBuffer, LongBuffer pBufferMemory) {

        try (MemoryStack stack = stackPush()) {

            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(usage);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create vertex buffer");
            }

            VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device, pBuffer.get(0), memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(findMemoryType(stack, memRequirements.memoryTypeBits(), properties));

            if (vkAllocateMemory(device, allocInfo, null, pBufferMemory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate vertex buffer memory");
            }

            vkBindBufferMemory(device, pBuffer.get(0), pBufferMemory.get(0), 0);
        }
    }

    @Override
    public void destroy() {
        vkDestroyDevice(device, null);
    }
}
