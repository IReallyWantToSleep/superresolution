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

package io.homo.superresolution.core.graphics.system;

import io.homo.superresolution.core.graphics.impl.device.IDevice;

public interface IRenderSystem {
    /**
     * 初始化渲染系统。
     * <p>
     * 在调用任何渲染相关方法前，需先初始化渲染系统。
     */
    void initRenderSystem();

    /**
     * 销毁渲染系统，释放相关资源。
     * <p>
     * 当渲染系统不再使用时应调用此方法，避免资源泄漏。
     */
    void destroyRenderSystem();

    /**
     * 获取硬件
     */
    IDevice device();

    /**
     * 等待GPU执行完渲染指令
     */
    void finish();
}