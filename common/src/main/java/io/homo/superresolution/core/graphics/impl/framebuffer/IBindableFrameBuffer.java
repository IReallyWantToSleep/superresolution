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

package io.homo.superresolution.core.graphics.impl.framebuffer;

/**
 * 可绑定帧缓冲接口
 */
public interface IBindableFrameBuffer extends IFrameBuffer {

    /**
     * 绑定帧缓冲区并可选设置视口
     *
     * @param bindPoint   绑定目标（如颜色/深度）
     * @param setViewport true时自动设置视口为缓冲区尺寸
     */
    void bind(FrameBufferBindPoint bindPoint, boolean setViewport);

    /**
     * 绑定帧缓冲区（不修改视口）
     *
     * @param bindPoint 绑定目标（如颜色/深度）
     */
    void bind(FrameBufferBindPoint bindPoint);

    /**
     * 解绑帧缓冲区
     *
     * @param bindPoint 要解绑的目标
     */
    void unbind(FrameBufferBindPoint bindPoint);
}
