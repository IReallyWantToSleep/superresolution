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
import io.homo.superresolution.core.graphics.impl.pipeline.state.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.vertex.VertexFormat;

import java.util.function.Consumer;

public abstract class GraphicsPipeline implements IPipeline {
    private final IShaderProgram shader;
    private final RasterizationState rasterization;
    private final DepthStencilState depthStencil;
    private final ColorBlendState colorBlend;
    private final DynamicStateFlags dynamicStates;
    private final PipelineDescriptorSet descriptorSet;

    public VertexFormat vertexFormat() {
        return vertexFormat;
    }

    private final VertexFormat vertexFormat;

    private final DynamicState currentDynamicState = new DynamicState();

    public GraphicsPipeline(IShaderProgram shader,
                            RasterizationState rasterization,
                            DepthStencilState depthStencil,
                            ColorBlendState colorBlend,
                            DynamicStateFlags dynamicStates,
                            VertexFormat vertexFormat,
                            PipelineDescriptorSet descriptorSet) {
        if (shader == null) {
            throw new IllegalArgumentException("Shader cannot be null");
        }
        this.shader = shader;
        this.rasterization = rasterization != null ? rasterization : RasterizationState.defaults();
        this.depthStencil = depthStencil != null ? depthStencil : DepthStencilState.disabled();
        this.colorBlend = colorBlend != null ? colorBlend : ColorBlendState.defaults();
        this.dynamicStates = dynamicStates != null ? dynamicStates : DynamicStateFlags.ViewportScissor;
        this.descriptorSet = descriptorSet;
        this.vertexFormat = vertexFormat;
    }

    public static Builder builder() {
        return new Builder();
    }

    public IShaderProgram shader() {
        return shader;
    }

    public RasterizationState rasterization() {
        return rasterization;
    }

    public DepthStencilState depthStencil() {
        return depthStencil;
    }

    public ColorBlendState colorBlend() {
        return colorBlend;
    }

    public DynamicStateFlags dynamicStates() {
        return dynamicStates;
    }

    @Override
    public PipelineDescriptorSet descriptorSet() {
        return descriptorSet;
    }

    public GraphicsPipeline setViewport(float x, float y, float width, float height) {
        if (!dynamicStates.has(DynamicStateFlags.Viewport)) {
            throw new IllegalStateException("Viewport is not a dynamic state for this pipeline");
        }
        currentDynamicState.viewportX = x;
        currentDynamicState.viewportY = y;
        currentDynamicState.viewportWidth = width;
        currentDynamicState.viewportHeight = height;
        currentDynamicState.viewportDirty = true;
        return this;
    }

    public GraphicsPipeline setScissor(int x, int y, int width, int height) {
        if (!dynamicStates.has(DynamicStateFlags.Scissor)) {
            throw new IllegalStateException("Scissor is not a dynamic state for this pipeline");
        }
        currentDynamicState.scissorX = x;
        currentDynamicState.scissorY = y;
        currentDynamicState.scissorWidth = width;
        currentDynamicState.scissorHeight = height;
        currentDynamicState.scissorDirty = true;
        return this;
    }

    public GraphicsPipeline setLineWidth(float width) {
        if (!dynamicStates.has(DynamicStateFlags.LineWidth)) {
            throw new IllegalStateException("LineWidth is not a dynamic state for this pipeline");
        }
        currentDynamicState.lineWidth = width;
        currentDynamicState.lineWidthDirty = true;
        return this;
    }

    public GraphicsPipeline setBlendConstants(float r, float g, float b, float a) {
        if (!dynamicStates.has(DynamicStateFlags.BlendConstants)) {
            throw new IllegalStateException("BlendConstants is not a dynamic state for this pipeline");
        }
        currentDynamicState.blendConstantR = r;
        currentDynamicState.blendConstantG = g;
        currentDynamicState.blendConstantB = b;
        currentDynamicState.blendConstantA = a;
        currentDynamicState.blendConstantsDirty = true;
        return this;
    }

    public void applyDynamicStates(ICommandBuffer cmd) {
        if (currentDynamicState.viewportDirty && dynamicStates.has(DynamicStateFlags.Viewport)) {
            cmd.getDecoder().setViewport(cmd, currentDynamicState.viewportX, currentDynamicState.viewportY,
                    currentDynamicState.viewportWidth, currentDynamicState.viewportHeight);
            currentDynamicState.viewportDirty = false;
        }
        if (currentDynamicState.scissorDirty && dynamicStates.has(DynamicStateFlags.Scissor)) {
            cmd.getDecoder().setScissor(cmd, currentDynamicState.scissorX, currentDynamicState.scissorY,
                    currentDynamicState.scissorWidth, currentDynamicState.scissorHeight);
            currentDynamicState.scissorDirty = false;
        }
        if (currentDynamicState.lineWidthDirty && dynamicStates.has(DynamicStateFlags.LineWidth)) {
            cmd.getDecoder().setLineWidth(cmd, currentDynamicState.lineWidth);
            currentDynamicState.lineWidthDirty = false;
        }
        if (currentDynamicState.blendConstantsDirty && dynamicStates.has(DynamicStateFlags.BlendConstants)) {
            cmd.getDecoder().setBlendConstants(cmd, currentDynamicState.blendConstantR,
                    currentDynamicState.blendConstantG,
                    currentDynamicState.blendConstantB,
                    currentDynamicState.blendConstantA);
            currentDynamicState.blendConstantsDirty = false;
        }
    }

    public static class Builder {
        private IShaderProgram shader;
        private RasterizationState rasterization = RasterizationState.defaults();
        private DepthStencilState depthStencil = DepthStencilState.disabled();
        private ColorBlendState colorBlend = ColorBlendState.defaults();
        private DynamicStateFlags dynamicStates = DynamicStateFlags.ViewportScissor;

        public VertexFormat vertexFormat() {
            return vertexFormat;
        }

        public Builder vertexFormat(VertexFormat vertexFormat) {
            this.vertexFormat = vertexFormat;
            return this;
        }

        public Builder vertexFormat(Consumer<VertexFormat.Builder> builderConsumer) {
            VertexFormat.Builder builder = new VertexFormat.Builder();
            builderConsumer.accept(builder);
            this.vertexFormat = builder.build();
            return this;
        }

        private VertexFormat vertexFormat;

        public IShaderProgram shader() {
            return shader;
        }

        public RasterizationState rasterization() {
            return rasterization;
        }

        public DepthStencilState depthStencil() {
            return depthStencil;
        }

        public ColorBlendState colorBlend() {
            return colorBlend;
        }

        public DynamicStateFlags dynamicStates() {
            return dynamicStates;
        }

        public Builder shader(IShaderProgram shader) {
            this.shader = shader;
            return this;
        }

        public Builder rasterization(Consumer<RasterizationState.Builder> config) {
            RasterizationState.Builder builder = new RasterizationState.Builder();
            config.accept(builder);
            this.rasterization = builder.build();
            return this;
        }

        public Builder depthStencil(Consumer<DepthStencilState.Builder> config) {
            DepthStencilState.Builder builder = new DepthStencilState.Builder();
            config.accept(builder);
            this.depthStencil = builder.build();
            return this;
        }

        public Builder colorBlend(Consumer<ColorBlendState.Builder> config) {
            ColorBlendState.Builder builder = new ColorBlendState.Builder();
            config.accept(builder);
            this.colorBlend = builder.build();
            return this;
        }

        public Builder dynamicStates(DynamicStateFlags states) {
            this.dynamicStates = states;
            return this;
        }

        public GraphicsPipeline build(IDevice device) {
            if (shader == null) {
                throw new IllegalStateException("Shader is required");
            }
            if (vertexFormat == null) {
                throw new IllegalStateException("Vertex format is required");
            }
            return device.createGraphicsPipeline(this);
        }
    }

    private static class DynamicState {
        float viewportX = 0, viewportY = 0;
        float viewportWidth = 0, viewportHeight = 0;
        boolean viewportDirty = false;

        int scissorX = 0, scissorY = 0;
        int scissorWidth = 0, scissorHeight = 0;
        boolean scissorDirty = false;

        float lineWidth = 1.0f;
        boolean lineWidthDirty = false;

        float blendConstantR = 0.0f;
        float blendConstantG = 0.0f;
        float blendConstantB = 0.0f;
        float blendConstantA = 0.0f;
        boolean blendConstantsDirty = false;
    }
}
