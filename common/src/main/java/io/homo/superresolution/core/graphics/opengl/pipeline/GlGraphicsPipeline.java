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

package io.homo.superresolution.core.graphics.opengl.pipeline;

import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.state.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.impl.vertex.VertexFormat;
import io.homo.superresolution.core.graphics.opengl.command.GlCommandBuffer;

import static org.lwjgl.opengl.GL41.*;

public class GlGraphicsPipeline extends GraphicsPipeline {
    public GlGraphicsPipeline(
            IShaderProgram shader,
            RenderPass renderPass,
            RasterizationState rasterization,
            DepthStencilState depthStencil,
            ColorBlendState colorBlend,
            DynamicStateFlags dynamicStates,
            PrimitiveType primitiveType,
            VertexFormat vertexFormat,
            PipelineDescriptorSet descriptorSet
    ) {
        super(
                shader,
                renderPass,
                rasterization,
                depthStencil,
                colorBlend,
                dynamicStates,
                primitiveType,
                vertexFormat,
                descriptorSet);
    }

    @Override
    public void destroy() {

    }

    public void setupRenderStates() {
        setupRenderStates(null);
    }

    public void setupRenderStates(GlCommandBuffer.ExecutionStateCache stateCache) {
        applyRasterizationState(rasterization(), stateCache);
        applyDepthStencilState(depthStencil(), stateCache);
        applyColorBlendState(colorBlend(), stateCache);
    }

    private void applyRasterizationState(RasterizationState state, GlCommandBuffer.ExecutionStateCache stateCache) {
        if (stateCache == null || !stateCache.matchesPolygonMode(state.polygonMode())) {
            switch (state.polygonMode()) {
                case Fill -> glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                case Line -> glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                case Point -> glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
            }
        }

        if (stateCache == null || !stateCache.matchesCullMode(state.cullMode())) {
            switch (state.cullMode()) {
                case None -> glDisable(GL_CULL_FACE);
                case Front -> {
                    glEnable(GL_CULL_FACE);
                    glCullFace(GL_FRONT);
                }
                case Back -> {
                    glEnable(GL_CULL_FACE);
                    glCullFace(GL_BACK);
                }
                case FrontAndBack -> {
                    glEnable(GL_CULL_FACE);
                    glCullFace(GL_FRONT_AND_BACK);
                }
            }
        }

        if (stateCache == null || !stateCache.matchesFrontFace(state.frontFace())) {
            switch (state.frontFace()) {
                case Clockwise -> glFrontFace(GL_CW);
                case CounterClockwise -> glFrontFace(GL_CCW);
            }
        }

        if (stateCache == null || !stateCache.matchesDepthClampEnable(state.depthClampEnable())) {
            if (state.depthClampEnable()) {
                glEnable(GL_DEPTH_CLAMP);
            } else {
                glDisable(GL_DEPTH_CLAMP);
            }
        }

        if (stateCache == null || !stateCache.matchesRasterizerDiscardEnable(state.rasterizerDiscardEnable())) {
            if (state.rasterizerDiscardEnable()) {
                glEnable(GL_RASTERIZER_DISCARD);
            } else {
                glDisable(GL_RASTERIZER_DISCARD);
            }
        }

        if (stateCache != null) {
            stateCache.recordRasterizationState(state);
        }
    }

    private void applyDepthStencilState(DepthStencilState state, GlCommandBuffer.ExecutionStateCache stateCache) {
        if (stateCache == null || !stateCache.matchesDepthTestEnable(state.depthTestEnable())) {
            if (state.depthTestEnable()) {
                glEnable(GL_DEPTH_TEST);
            } else {
                glDisable(GL_DEPTH_TEST);
            }
        }

        if (state.depthTestEnable() && (stateCache == null || !stateCache.matchesDepthCompareOp(state.depthCompareOp()))) {
            glDepthFunc(toGLCompareOp(state.depthCompareOp()));
        }

        if (stateCache == null || !stateCache.matchesDepthWriteEnable(state.depthWriteEnable())) {
            glDepthMask(state.depthWriteEnable());
        }

        if (stateCache == null || !stateCache.matchesStencilTestEnable(state.stencilTestEnable())) {
            if (state.stencilTestEnable()) {
                glEnable(GL_STENCIL_TEST);
            } else {
                glDisable(GL_STENCIL_TEST);
            }
        }

        if (state.stencilTestEnable()) {
            if (stateCache == null || !stateCache.matchesStencilWriteMask(true, state.stencilWriteMask())) {
                glStencilMaskSeparate(GL_FRONT, state.stencilWriteMask());
            }
            if (stateCache == null || !stateCache.matchesStencilWriteMask(false, state.stencilWriteMask())) {
                glStencilMaskSeparate(GL_BACK, state.stencilWriteMask());
            }
            if (stateCache == null || !stateCache.matchesStencilFunc(true, state.stencilCompareOpFront(), state.stencilReference(), state.stencilCompareMask())) {
                glStencilFuncSeparate(
                        GL_FRONT,
                        toGLCompareOp(state.stencilCompareOpFront()),
                        state.stencilReference(),
                        state.stencilCompareMask()
                );
            }
            if (stateCache == null || !stateCache.matchesStencilFunc(false, state.stencilCompareOpBack(), state.stencilReference(), state.stencilCompareMask())) {
                glStencilFuncSeparate(
                        GL_BACK,
                        toGLCompareOp(state.stencilCompareOpBack()),
                        state.stencilReference(),
                        state.stencilCompareMask()
                );
            }
            if (stateCache == null || !stateCache.matchesStencilOp(true, state.stencilFailOpFront(), state.stencilDepthFailOpFront(), state.stencilPassOpFront())) {
                glStencilOpSeparate(
                        GL_FRONT,
                        toGLStencilOp(state.stencilFailOpFront()),
                        toGLStencilOp(state.stencilDepthFailOpFront()),
                        toGLStencilOp(state.stencilPassOpFront())
                );
            }
            if (stateCache == null || !stateCache.matchesStencilOp(false, state.stencilFailOpBack(), state.stencilDepthFailOpBack(), state.stencilPassOpBack())) {
                glStencilOpSeparate(
                        GL_BACK,
                        toGLStencilOp(state.stencilFailOpBack()),
                        toGLStencilOp(state.stencilDepthFailOpBack()),
                        toGLStencilOp(state.stencilPassOpBack())
                );
            }
        }

        if (stateCache != null) {
            stateCache.recordDepthStencilState(state);
        }
    }

    private void applyColorBlendState(ColorBlendState state, GlCommandBuffer.ExecutionStateCache stateCache) {
        for (int i = 0; i < state.attachments().size(); i++) {
            ColorBlendAttachment attachment = state.attachments().get(i);
            if (stateCache == null || !stateCache.matchesBlendEnable(i, attachment.blendEnable())) {
                if (attachment.blendEnable()) {
                    glEnablei(GL_BLEND, i);
                } else {
                    glDisablei(GL_BLEND, i);
                }
            }

            if (attachment.blendEnable()) {
                if (stateCache == null || !stateCache.matchesBlendFunction(
                        i,
                        attachment.srcColorBlendFactor(),
                        attachment.dstColorBlendFactor(),
                        attachment.srcAlphaBlendFactor(),
                        attachment.dstAlphaBlendFactor())) {
                    glBlendFuncSeparatei(
                            i,
                            toGLBlendFactor(attachment.srcColorBlendFactor()),
                            toGLBlendFactor(attachment.dstColorBlendFactor()),
                            toGLBlendFactor(attachment.srcAlphaBlendFactor()),
                            toGLBlendFactor(attachment.dstAlphaBlendFactor())
                    );
                }
                if (stateCache == null || !stateCache.matchesBlendEquation(i, attachment.colorBlendOp(), attachment.alphaBlendOp())) {
                    glBlendEquationSeparatei(
                            i,
                            toGLBlendOp(attachment.colorBlendOp()),
                            toGLBlendOp(attachment.alphaBlendOp())
                    );
                }
            }

            int mask = attachment.colorWriteMask();
            if (stateCache == null || !stateCache.matchesColorWriteMask(i, mask)) {
                glColorMaski(
                        i,
                        (mask & ColorComponentFlags.R) != 0,
                        (mask & ColorComponentFlags.G) != 0,
                        (mask & ColorComponentFlags.B) != 0,
                        (mask & ColorComponentFlags.A) != 0
                );
            }
        }

        if (stateCache != null) {
            stateCache.recordColorBlendState(state);
        }
    }

    private int toGLCompareOp(CompareOp op) {
        return switch (op) {
            case Never -> GL_NEVER;
            case Less -> GL_LESS;
            case Equal -> GL_EQUAL;
            case LessEqual -> GL_LEQUAL;
            case Greater -> GL_GREATER;
            case NotEqual -> GL_NOTEQUAL;
            case GreaterEqual -> GL_GEQUAL;
            case Always -> GL_ALWAYS;
        };
    }

    private int toGLBlendFactor(BlendFactor factor) {
        return switch (factor) {
            case Zero -> GL_ZERO;
            case One -> GL_ONE;
            case SrcColor -> GL_SRC_COLOR;
            case OneMinusSrcColor -> GL_ONE_MINUS_SRC_COLOR;
            case DstColor -> GL_DST_COLOR;
            case OneMinusDstColor -> GL_ONE_MINUS_DST_COLOR;
            case SrcAlpha -> GL_SRC_ALPHA;
            case OneMinusSrcAlpha -> GL_ONE_MINUS_SRC_ALPHA;
            case DstAlpha -> GL_DST_ALPHA;
            case OneMinusDstAlpha -> GL_ONE_MINUS_DST_ALPHA;
            case ConstantColor -> GL_CONSTANT_COLOR;
            case OneMinusConstantColor -> GL_ONE_MINUS_CONSTANT_COLOR;
            case ConstantAlpha -> GL_CONSTANT_ALPHA;
            case OneMinusConstantAlpha -> GL_ONE_MINUS_CONSTANT_ALPHA;
            case SrcAlphaSaturate -> GL_SRC_ALPHA_SATURATE;
        };
    }

    private int toGLBlendOp(BlendOp op) {
        return switch (op) {
            case Add -> GL_FUNC_ADD;
            case Subtract -> GL_FUNC_SUBTRACT;
            case ReverseSubtract -> GL_FUNC_REVERSE_SUBTRACT;
            case Min -> GL_MIN;
            case Max -> GL_MAX;
        };
    }

    private int toGLStencilOp(StencilOp op) {
        return switch (op) {
            case Keep -> GL_KEEP;
            case Zero -> GL_ZERO;
            case Replace -> GL_REPLACE;
            case IncrementAndClamp -> GL_INCR;
            case DecrementAndClamp -> GL_DECR;
            case Invert -> GL_INVERT;
            case IncrementAndWrap -> GL_INCR_WRAP;
            case DecrementAndWrap -> GL_DECR_WRAP;
        };
    }
}
