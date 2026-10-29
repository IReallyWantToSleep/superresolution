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

import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.impl.Destroyable;

public abstract class AbstractAlgorithm implements Destroyable {
    protected InputResourceSet resources;
    protected InitializationDescription initDesc = new InitializationDescription();
    /** true 时下一次 dispatch 请求算法丢弃累积历史。新实例默认 true。 */
    protected boolean needsHistoryReset = true;

    public AbstractAlgorithm() {

    }

    protected InputResourceSet getResources() {
        return resources;
    }

    /** 读取并清除历史复位标志。 */
    protected boolean consumeHistoryReset() {
        boolean r = needsHistoryReset;
        needsHistoryReset = false;
        return r;
    }

    /** 令时序历史失效。世界加载/传送时调用。 */
    public void invalidateHistory() {
        needsHistoryReset = true;
    }

    public final void initialize() {
        initialize(InitializationDescription.defaults());
    }

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
}
