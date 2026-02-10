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

package io.homo.superresolution.srapi;

import org.joml.Vector2i;

import java.util.EnumSet;

/**
 * 创建Upscale上下文的描述符
 */
public class SRCreateUpscaleContextDesc {
    /**
     * 渲染API类型
     */
    public SRRenderApiType renderApiType;

    /**
     * OpenGL设备信息（当renderApiType为OPENGL时使用）
     */
    private SROpenGLDeviceInfo openglDeviceInfo;

    /**
     * Vulkan设备信息（当renderApiType为VULKAN时使用）
     */
    private SRVulkanDeviceInfo vulkanDeviceInfo;

    /**
     * 放大后的分辨率
     */
    public Vector2i upscaledSize;

    /**
     * 渲染分辨率
     */
    public Vector2i renderSize;

    /**
     * 消息回调（函数指针，可选）
     */
    public long messageCallback;

    /**
     * 额外参数
     */
    public SRContextExtraParams extraParams;

    /**
     * 标志位
     */
    public EnumSet<SRUpscaleContextCreateFlags> flags;

    /**
     * 创建OpenGL上下文描述符
     */
    public static SRCreateUpscaleContextDesc createOpenGL(
            SROpenGLDeviceInfo deviceInfo,
            Vector2i upscaledSize,
            Vector2i renderSize,
            EnumSet<SRUpscaleContextCreateFlags> flags) {
        SRCreateUpscaleContextDesc desc = new SRCreateUpscaleContextDesc();
        desc.renderApiType = SRRenderApiType.OPENGL;
        desc.openglDeviceInfo = deviceInfo;
        desc.upscaledSize = upscaledSize;
        desc.renderSize = renderSize;
        desc.flags = flags;
        desc.extraParams = new SRContextExtraParams();
        return desc;
    }

    /**
     * 创建Vulkan上下文描述符
     */
    public static SRCreateUpscaleContextDesc createVulkan(
            SRVulkanDeviceInfo deviceInfo,
            Vector2i upscaledSize,
            Vector2i renderSize,
            EnumSet<SRUpscaleContextCreateFlags> flags) {
        SRCreateUpscaleContextDesc desc = new SRCreateUpscaleContextDesc();
        desc.renderApiType = SRRenderApiType.VULKAN;
        desc.vulkanDeviceInfo = deviceInfo;
        desc.upscaledSize = upscaledSize;
        desc.renderSize = renderSize;
        desc.flags = flags;
        desc.extraParams = new SRContextExtraParams();
        return desc;
    }

    private SRCreateUpscaleContextDesc() {
        this.messageCallback = 0;
    }

    public SRRenderApiType getRenderApiType() {
        return renderApiType;
    }

    public SROpenGLDeviceInfo getOpenglDeviceInfo() {
        return openglDeviceInfo;
    }

    public SRVulkanDeviceInfo getVulkanDeviceInfo() {
        return vulkanDeviceInfo;
    }

    public Vector2i getUpscaledSize() {
        return upscaledSize;
    }

    public void setUpscaledSize(Vector2i upscaledSize) {
        this.upscaledSize = upscaledSize;
    }

    public Vector2i getRenderSize() {
        return renderSize;
    }

    public void setRenderSize(Vector2i renderSize) {
        this.renderSize = renderSize;
    }

    public long getMessageCallback() {
        return messageCallback;
    }

    public void setMessageCallback(long messageCallback) {
        this.messageCallback = messageCallback;
    }

    public SRContextExtraParams getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(SRContextExtraParams extraParams) {
        this.extraParams = extraParams;
    }

    public EnumSet<SRUpscaleContextCreateFlags> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<SRUpscaleContextCreateFlags> flags) {
        this.flags = flags;
    }
}
