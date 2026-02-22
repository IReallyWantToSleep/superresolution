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
import org.joml.Vector2f;

import java.util.List;

public abstract class AbstractAlgorithm implements Resizable, Destroyable {
    protected InputResourceSet getResources() {
        return resources;
    }

    protected InputResourceSet resources;

    public AbstractAlgorithm() {

    }

    /**
     * 初始化算法。
     */
    public abstract void init();

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

    public Vector2f getJitterOffset(
            int frameCount,
            Vector2f renderSize,
            Vector2f screenSize
    ) {
        return new Vector2f(0);
    }

    public int getJitterSequenceLength(
            int frameCount,
            Vector2f renderSize,
            Vector2f screenSize
    ) {
        return 0;
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