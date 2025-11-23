/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.srapi;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

public class SRVulkanDeviceInfo {
    public long instance;
    public long physicalDevice;
    public long device;
    public long initCommandBuffer;
    public long deviceProcAddr;
    public long instanceProcAddr;

    public SRVulkanDeviceInfo() {
        this.instance = 0;
        this.physicalDevice = 0;
        this.device = 0;
        this.initCommandBuffer = 0;
        this.deviceProcAddr = 0;
        this.instanceProcAddr = 0;
    }

    public SRVulkanDeviceInfo(long instance, long physicalDevice, long device,
                              long initCommandBuffer, long deviceProcAddr, long instanceProcAddr) {
        this.instance = instance;
        this.physicalDevice = physicalDevice;
        this.device = device;
        this.initCommandBuffer = initCommandBuffer;
        this.deviceProcAddr = deviceProcAddr;
        this.instanceProcAddr = instanceProcAddr;
    }

    public SRVulkanDeviceInfo(VkInstance instance, VkPhysicalDevice physicalDevice,
                              VkDevice device, VkCommandBuffer initCommandBuffer,
                              long deviceProcAddr, long instanceProcAddr) {
        this.instance = instance != null ? instance.address() : 0;
        this.physicalDevice = physicalDevice != null ? physicalDevice.address() : 0;
        this.device = device != null ? device.address() : 0;
        this.initCommandBuffer = initCommandBuffer != null ? initCommandBuffer.address() : 0;
        this.deviceProcAddr = deviceProcAddr;
        this.instanceProcAddr = instanceProcAddr;
    }

    public long getInstance() {
        return instance;
    }

    public void setInstance(long instance) {
        this.instance = instance;
    }

    public long getPhysicalDevice() {
        return physicalDevice;
    }

    public void setPhysicalDevice(long physicalDevice) {
        this.physicalDevice = physicalDevice;
    }

    public long getDevice() {
        return device;
    }

    public void setDevice(long device) {
        this.device = device;
    }

    public long getInitCommandBuffer() {
        return initCommandBuffer;
    }

    public void setInitCommandBuffer(long initCommandBuffer) {
        this.initCommandBuffer = initCommandBuffer;
    }

    public long getDeviceProcAddr() {
        return deviceProcAddr;
    }

    public void setDeviceProcAddr(long deviceProcAddr) {
        this.deviceProcAddr = deviceProcAddr;
    }

    public long getInstanceProcAddr() {
        return instanceProcAddr;
    }

    public void setInstanceProcAddr(long instanceProcAddr) {
        this.instanceProcAddr = instanceProcAddr;
    }
}
