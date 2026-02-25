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

import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Resizable;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.upscale.DispatchResource;

import java.util.List;

public abstract class AbstractAlgorithm implements Resizable, Destroyable {
    protected InputResourceSet getResources() {
        return resources;
    }

    protected InputResourceSet resources;

    /**
     * 最近一次初始化时使用的描述，子类可在 {@code resize()} 等方法中复用。
     */
    protected InitializationDescription initDesc = new InitializationDescription();

    public AbstractAlgorithm() {

    }

    /**
     * 使用默认初始化描述初始化算法（从全局 {@link SuperResolutionAPI} 读取状态）。
     * <p>由 {@link io.homo.superresolution.api.registry.AlgorithmDescription#createNewInstance()} 和普通创建路径调用。</p>
     */
    public final void initialize() {
        initialize(InitializationDescription.defaults());
    }

    /**
     * 初始化算法。
     *
     * @param desc 初始化描述，包含 HDR 标志等运行时配置。
     */
    public abstract void initialize(InitializationDescription desc);

    /**
     * 运行算法。
     *
     * @param dispatchResource 运行算法所需资源。
     *
     * @return 如果运行成功返回true，否则返回false。
     */
    public boolean dispatch(DispatchResource dispatchResource) {
        this.resources = dispatchResource.resources();
        return true;
    }

    /**
     * 销毁算法，释放资源。
     */
    @Override
    public abstract void destroy();

    /**
     * 调整帧缓冲区的大小。
     *
     * @param width  新的宽度(游戏屏幕宽度)。
     * @param height 新的高度(游戏屏幕高度)。
     */
    @Override
    public abstract void resize(int width, int height);

    /**
     * 获取输出帧缓冲区。
     *
     * @return 输出帧缓冲区。
     */
    public abstract IFrameBuffer getOutputFrameBuffer();

    /**
     * 获取输出帧缓冲区的颜色纹理ID。
     *
     * @return 输出帧缓冲区的颜色纹理ID。
     */
    public int getOutputTextureId() {
        return getOutputFrameBuffer().getTextureId(FrameBufferAttachmentType.Color);
    }

    public boolean isSupportJitter() {
        return false;
    }

    public List<QualityPreset> getQualityPresets() {
        return List.of();
    }

    public boolean isCustomUpscaleRatio() {
        return true;
    }
}