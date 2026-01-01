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

package io.homo.superresolution.core.graphics.impl.pipeline;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.device.IDevice;
import io.homo.superresolution.core.graphics.impl.framebuffer.ColorAttachment;
import io.homo.superresolution.core.graphics.impl.framebuffer.DepthStencilAttachment;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.impl.Destroyable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class RenderPass implements Destroyable {
    protected final GraphicsPipeline pipeline;
    protected final IFrameBuffer frameBuffer;
    protected final PassClearState clearState;

    protected RenderPass(GraphicsPipeline pipeline,
                         IFrameBuffer frameBuffer,
                         PassClearState clearState) {
        this.pipeline = pipeline;
        this.frameBuffer = frameBuffer;
        this.clearState = clearState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public GraphicsPipeline pipeline() {
        return pipeline;
    }

    public IFrameBuffer frameBuffer() {
        return frameBuffer;
    }

    public List<ColorAttachment> colorAttachments() {
        return frameBuffer.getColorAttachments();
    }

    public DepthStencilAttachment depthStencilAttachment() {
        return frameBuffer.getDepthStencilAttachment();
    }

    public PassClearState clearState() {
        return clearState;
    }

    public abstract void execute(ICommandBuffer cmd, IVertexBuffer vertexBuffer);

    public abstract void execute(ICommandBuffer cmd);

    @Override
    public abstract void destroy();

    public static class Builder {
        private GraphicsPipeline pipeline;
        private IFrameBuffer frameBuffer;
        private final PassClearState.Builder clearStateBuilder = PassClearState.builder();

        public Builder pipeline(GraphicsPipeline pipeline) {
            this.pipeline = pipeline;
            return this;
        }

        public Builder frameBuffer(IFrameBuffer frameBuffer) {
            this.frameBuffer = frameBuffer;
            return this;
        }

        public Builder clearColorOnBegin(int index, float r, float g, float b, float a) {
            clearStateBuilder.clearColorOnBegin(index, r, g, b, a);
            return this;
        }

        public Builder clearDepthOnBegin(float depth) {
            clearStateBuilder.clearDepthOnBegin(depth);
            return this;
        }

        public Builder clearStencilOnBegin(int stencil) {
            clearStateBuilder.clearStencilOnBegin(stencil);
            return this;
        }

        public Builder clearColorOnEnd(int index, float r, float g, float b, float a) {
            clearStateBuilder.clearColorOnEnd(index, r, g, b, a);
            return this;
        }

        public Builder clearDepthOnEnd(float depth) {
            clearStateBuilder.clearDepthOnEnd(depth);
            return this;
        }

        public Builder clearStencilOnEnd(int stencil) {
            clearStateBuilder.clearStencilOnEnd(stencil);
            return this;
        }

        public Builder clearState(Consumer<PassClearState.Builder> config) {
            config.accept(clearStateBuilder);
            return this;
        }

        public RenderPass build(IDevice device) {
            if (pipeline == null) {
                throw new IllegalStateException("Pipeline is required");
            }
            if (frameBuffer == null) {
                throw new IllegalStateException("FrameBuffer is required");
            }
            return device.createRenderPass(this);
        }

        public IPipeline getPipeline() {
            return pipeline;
        }

        public IFrameBuffer getFrameBuffer() {
            return frameBuffer;
        }

        public PassClearState getClearState() {
            return clearStateBuilder.build();
        }
    }
}
