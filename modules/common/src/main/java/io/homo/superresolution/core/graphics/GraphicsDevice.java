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

package io.homo.superresolution.core.graphics;

import org.lwjgl.opengl.EXTMemoryObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import io.homo.superresolution.common.SuperResolution;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES;
import static org.lwjgl.vulkan.VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2;

public class GraphicsDevice {
    public static final int UUID_SIZE = EXTMemoryObject.GL_UUID_SIZE_EXT;

    private final List<byte[]> deviceUUIDs;
    private final byte[] driverUUID;
    private final String deviceName;

    public GraphicsDevice(
            byte[] deviceUUID,
            byte[] driverUUID,
            String deviceName
    ) {
        this(Collections.singletonList(deviceUUID), driverUUID, deviceName);
    }

    public GraphicsDevice(
            List<byte[]> deviceUUIDs,
            byte[] driverUUID,
            String deviceName
    ) {
        this.deviceUUIDs = copyDeviceUUIDs(deviceUUIDs);
        this.driverUUID = Arrays.copyOf(driverUUID, UUID_SIZE);
        this.deviceName = deviceName;
    }

    public static GraphicsDevice createFromOpenGL() {
        if (GL.getCapabilities().GL_EXT_memory_object) {
            List<byte[]> deviceUUIDs = new ArrayList<>();
            byte[] driverUUID = new byte[UUID_SIZE];
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer numDevicesBuf;
                numDevicesBuf = stack.callocInt(1);
                GL11.glGetIntegerv(EXTMemoryObject.GL_NUM_DEVICE_UUIDS_EXT, numDevicesBuf);
                int deviceUUIDCount = numDevicesBuf.get(0);
                ByteBuffer driverUUIDBuf = stack.calloc(UUID_SIZE);

                for (int i = 0; i < deviceUUIDCount; i++) {
                    byte[] deviceUUID = new byte[UUID_SIZE];
                    ByteBuffer deviceUUIDBuf = stack.calloc(UUID_SIZE);
                    EXTMemoryObject.glGetUnsignedBytei_vEXT(
                            EXTMemoryObject.GL_DEVICE_UUID_EXT,
                            i,
                            deviceUUIDBuf
                    );
                    deviceUUIDBuf.get(deviceUUID);
                    deviceUUIDs.add(deviceUUID);
                }
                EXTMemoryObject.glGetUnsignedBytevEXT(
                        EXTMemoryObject.GL_DRIVER_UUID_EXT,
                        driverUUIDBuf
                );
                driverUUIDBuf.get(driverUUID);
            }
            return new GraphicsDevice(deviceUUIDs, driverUUID, GL20.glGetString(GL20.GL_RENDERER));
        }
        throw new UnsupportedOperationException("GL_EXT_memory_object is not supported");
    }

    public static GraphicsDevice createFromVulkan(VkPhysicalDevice physicalDevice) {
        byte[] deviceUUID = new byte[UUID_SIZE];
        byte[] driverUUID = new byte[UUID_SIZE];
        String deviceName;
        try (MemoryStack stack = MemoryStack.stackPush()) {
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

    private static List<byte[]> copyDeviceUUIDs(List<byte[]> deviceUUIDs) {
        List<byte[]> copiedUUIDs = new ArrayList<>(deviceUUIDs.size());
        for (byte[] deviceUUID : deviceUUIDs) {
            copiedUUIDs.add(Arrays.copyOf(deviceUUID, UUID_SIZE));
        }
        return Collections.unmodifiableList(copiedUUIDs);
    }

    public boolean isCompatibleWith(GraphicsDevice that) {
        if (!Arrays.equals(this.driverUUID, that.driverUUID)) {
            return false;
        }
        for (byte[] thisDeviceUUID : this.deviceUUIDs) {
            for (byte[] thatDeviceUUID : that.deviceUUIDs) {
                if (Arrays.equals(thisDeviceUUID, thatDeviceUUID)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GraphicsDevice that = (GraphicsDevice) obj;
        return deviceUUIDsEqual(that.deviceUUIDs) && Arrays.equals(this.driverUUID, that.driverUUID);
    }

    private boolean deviceUUIDsEqual(List<byte[]> thatDeviceUUIDs) {
        if (this.deviceUUIDs.size() != thatDeviceUUIDs.size()) {
            return false;
        }
        for (int i = 0; i < this.deviceUUIDs.size(); i++) {
            if (!Arrays.equals(this.deviceUUIDs.get(i), thatDeviceUUIDs.get(i))) {
                return false;
            }
        }
        return true;
    }

    public byte[] deviceUUID() {
        return Arrays.copyOf(deviceUUIDs.get(0), UUID_SIZE);
    }

    public List<byte[]> deviceUUIDs() {
        return copyDeviceUUIDs(deviceUUIDs);
    }

    public byte[] driverUUID() {
        return Arrays.copyOf(driverUUID, UUID_SIZE);
    }

    public String deviceName() {
        return deviceName;
    }
}
