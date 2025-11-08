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

package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;

import java.util.Objects;

public class PipelineClearTextureJob implements IPipelineJob {
    protected float[] clearColor = null;
    protected Float clearDepth = null;
    protected Integer clearStencil = null;
    protected ITexture target = null;

    public PipelineClearTextureJob clearColor(float r, float g, float b, float a) {
        validateColorComponent(r);
        validateColorComponent(g);
        validateColorComponent(b);
        validateColorComponent(a);
        this.clearColor = new float[]{r, g, b, a};
        return this;
    }

    public PipelineClearTextureJob clearDepth(float depth) {
        if (depth < 0 || depth > 1) {
            throw new IllegalArgumentException("深度值必须在[0,1]范围内");
        }
        this.clearDepth = depth;
        return this;
    }


    public PipelineClearTextureJob clearStencil(int stencil) {
        if (stencil < 0 || stencil > 255) {
            throw new IllegalArgumentException("模板值必须在[0,255]范围内");
        }
        this.clearStencil = stencil;
        return this;
    }

    public PipelineClearTextureJob clearTarget(ITexture target) {
        this.target = Objects.requireNonNull(target, "清除目标不能为null");
        return this;
    }

    @Override
    public void execute(ICommandBuffer commandBuffer) {
        validateState();
        boolean cleared = false;

        if (clearColor != null) {
            validateChannels(clearColor.length, target.getTextureFormat().getChannelCount());
            commandBuffer.getEncoder().clearTextureRGBA(commandBuffer, target, clearColor);
            cleared = true;
        }

        if (clearDepth != null) {
            validateDepthSupport();
            commandBuffer.getEncoder().clearTextureDepth(commandBuffer, target, clearDepth);
            cleared = true;
        }

        if (clearStencil != null) {
            validateStencilSupport();
            commandBuffer.getEncoder().clearTextureStencil(commandBuffer, target, clearStencil);
            cleared = true;
        }

        if (!cleared) {
            throw new IllegalStateException("无有效的清除操作配置");
        }
    }

    private void validateState() {
        Objects.requireNonNull(target, "清除目标未设置");
    }

    private void validateColorComponent(float c) {
        if (c < 0 || c > 1) {
            throw new IllegalArgumentException("颜色分量必须在[0,1]范围内");
        }
    }

    private void validateChannels(int actual, int expected) {
        if (actual != expected) {
            throw new IllegalStateException(
                    String.format("颜色分量数(%d)与纹理通道数(%d)不匹配", actual, expected)
            );
        }
    }

    private void validateDepthSupport() {
        if (!target.getTextureFormat().isDepth()) {
            throw new UnsupportedOperationException("纹理不支持深度清除");
        }
    }

    private void validateStencilSupport() {
        if (!target.getTextureFormat().isStencil()) {
            throw new UnsupportedOperationException("纹理不支持模板清除");
        }
    }

    @Override
    public void destroy() {
        clearColor = null;
        clearDepth = null;
        clearStencil = null;
        target = null;
    }
}