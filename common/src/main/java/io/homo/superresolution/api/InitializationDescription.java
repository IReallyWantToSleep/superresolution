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

package io.homo.superresolution.api;

/**
 * 算法初始化描述，包含算法在 {@link AbstractAlgorithm#initialize(InitializationDescription)} 时所需的配置标志。
 * <p>
 * 与 {@link io.homo.superresolution.common.upscale.DispatchResource} 在每帧传递运行时参数不同，
 * {@code InitializationDescription} 的数据仅在算法创建/重新初始化时生效。
 * </p>
 */
public class InitializationDescription {

    private boolean isHdrInput;

    public InitializationDescription() {
    }

    /**
     * 创建默认初始化描述（所有标志为 false）。
     * 请使用 {@code SuperResolution.getInitializationDescription()} 获取基于当前配置的描述。
     */
    public static InitializationDescription defaults() {
        return new InitializationDescription();
    }

    /**
     * 输入颜色是否为 HDR。影响算法底层（如 DLSS、XeSS）的初始化标志。
     */
    public boolean isHdrInput() {
        return isHdrInput;
    }

    /**
     * 设置 HDR 输入标志。
     *
     * @return {@code this}，支持链式调用
     */
    public InitializationDescription setHdrInput(boolean isHdrInput) {
        this.isHdrInput = isHdrInput;
        return this;
    }
}
