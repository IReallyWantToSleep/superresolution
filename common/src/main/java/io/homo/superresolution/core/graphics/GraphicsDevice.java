package io.homo.superresolution.core.graphics;

import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2;

public class GraphicsDevice {
    public static int UUID_SIZE = 16;

    private final byte[] deviceUUID;
    private final byte[] driverUUID;
    private final String deviceName;

    public GraphicsDevice(
            byte[] deviceUUID,
            byte[] driverUUID,
            String deviceName
    ) {
        this.deviceUUID = deviceUUID;
        this.driverUUID = driverUUID;
        this.deviceName = deviceName;
    }

    public static GraphicsDevice createFromOpenGL(){
        if (GL.getCapabilities().GL_EXT_memory_object){
            byte[] deviceUUID = new byte[UUID_SIZE];
            byte[] driverUUID = new byte[UUID_SIZE];
            try (MemoryStack stack = MemoryStack.stackPush()){
                ByteBuffer deviceUUIDBuf = stack.calloc(UUID_SIZE);
                ByteBuffer driverUUIDBuf = stack.calloc(UUID_SIZE);

                EXTMemoryObject.glGetUnsignedBytevEXT(
                        EXTMemoryObject.GL_DEVICE_UUID_EXT,
                        deviceUUIDBuf
                );
                EXTMemoryObject.glGetUnsignedBytevEXT(
                        EXTMemoryObject.GL_DRIVER_UUID_EXT,
                        driverUUIDBuf
                );
                deviceUUIDBuf.get(deviceUUID);
                driverUUIDBuf.get(driverUUID);
            }
            return new GraphicsDevice(deviceUUID, driverUUID,GL20.glGetString(GL20.GL_RENDERER));
        }
        throw new UnsupportedOperationException("GL_EXT_memory_object is not supported");
    }

    public static GraphicsDevice createFromVulkan(VkPhysicalDevice physicalDevice){
        byte[] deviceUUID = new byte[UUID_SIZE];
        byte[] driverUUID = new byte[UUID_SIZE];
        String deviceName;
        try (MemoryStack stack = MemoryStack.stackPush()){
            VkPhysicalDeviceIDProperties idProperties = VkPhysicalDeviceIDProperties.calloc(stack);
            idProperties.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES);
            VkPhysicalDeviceProperties2 properties2 = VkPhysicalDeviceProperties2.calloc(stack);
            properties2.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2);
            properties2.pNext(idProperties.address());
            VK12.vkGetPhysicalDeviceProperties2(physicalDevice, properties2);
            idProperties.deviceUUID().get(deviceUUID);
            idProperties.driverUUID().get(driverUUID);
            VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.calloc(stack);
            VK12.vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);
            deviceName = deviceProperties.deviceNameString();
        }
        return new GraphicsDevice(deviceUUID, driverUUID, deviceName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GraphicsDevice that = (GraphicsDevice) obj;
        for (int i = 0; i < UUID_SIZE; i++) {
            if (this.deviceUUID[i] != that.deviceUUID[i]) return false;
        }
        return true;
    }

    public byte[] deviceUUID() {
        return deviceUUID;
    }

    public byte[] driverUUID() {
        return driverUUID;
    }

    public String deviceName() {
        return deviceName;
    }
}
