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

package io.homo.superresolution.core.graphics.impl.grape;

import io.homo.superresolution.core.graphics.impl.FullscreenQuad;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.vertex.*;

import java.util.Objects;

public class GrapeGraphicsJob extends GpuComputeJob<GrapeGraphicsJob> implements IGrapeJob {
    protected final float[] viewport = new float[]{-1, -1, -1, -1};
    protected IFrameBuffer frameBuffer;
    protected GraphicsPipeline pipeline = null;
    protected boolean dirty = true;
    protected RenderPass renderPass = null;
    protected IVertexBuffer cachedVertexBuffer = null;

    public GrapeGraphicsJob viewport(float x, float y, float width, float height) {
        this.viewport[0] = x;
        this.viewport[1] = y;
        this.viewport[2] = width;
        this.viewport[3] = height;
        return this;
    }

    public GrapeGraphicsJob targetFrameBuffer(IFrameBuffer frameBuffer) {
        this.frameBuffer = Objects.requireNonNull(frameBuffer, "帧缓冲区不能为null");
        this.dirty = true;
        return this;
    }

    public GrapeGraphicsJob pipeline(GraphicsPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(pipeline, "图形管线不能为null");
        this.dirty = true;
        return this;
    }

    @Override
    public void execute(ICommandBuffer commandBuffer) {
        Objects.requireNonNull(pipeline, "图形通道未设置");
        Objects.requireNonNull(frameBuffer, "帧缓冲区未设置");
        if (dirty) {
            if (renderPass != null) {
                renderPass.destroy();
            }
            renderPass = RenderPass.builder()
                    .frameBuffer(frameBuffer)
                    .pipeline(pipeline)
                    .build(commandBuffer.getDevice());
        }
        setupProgramResources(pipeline);
        if (isViewportValid()) {
            commandBuffer.getDecoder().setViewport(
                    commandBuffer,
                    viewport[0], viewport[1], viewport[2], viewport[3]
            );
        }
        if (cachedVertexBuffer == null) {
            cachedVertexBuffer = FullscreenQuad.create(commandBuffer.getDevice());
        }
        commandBuffer.getDecoder().draw(
                commandBuffer,
                renderPass,
                PrimitiveType.TriangleStrip,
                cachedVertexBuffer,
                4,
                0
        );
    }

    private boolean isViewportValid() {
        return viewport[0] >= 0 && viewport[1] >= 0
                && viewport[2] > 0 && viewport[3] > 0;
    }

    @Override
    public void destroy() {
        pipeline = null;
        frameBuffer = null;
        if (cachedVertexBuffer != null) {
            cachedVertexBuffer.destroy();
        }
        if (renderPass != null) {
            renderPass.destroy();
        }
    }
}