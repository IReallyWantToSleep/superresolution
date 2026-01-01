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
import io.homo.superresolution.core.graphics.impl.pipeline.state.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.vertex.VertexFormat;

import static org.lwjgl.opengl.GL41.*;

public class GlGraphicsPipeline extends GraphicsPipeline {
    public GlGraphicsPipeline(
            IShaderProgram shader,
            RasterizationState rasterization,
            DepthStencilState depthStencil,
            ColorBlendState colorBlend,
            DynamicStateFlags dynamicStates,
            VertexFormat vertexFormat,
            PipelineDescriptorSet descriptorSet
    ) {
        super(
                shader,
                rasterization,
                depthStencil,
                colorBlend,
                dynamicStates,
                vertexFormat,
                descriptorSet);
    }

    @Override
    public void destroy() {

    }

    public void setupRenderStates() {
        applyRasterizationState(rasterization());
        applyDepthStencilState(depthStencil());
        applyColorBlendState(colorBlend());
    }

    private void applyRasterizationState(RasterizationState state) {
        switch (state.polygonMode()) {
            case Fill -> glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            case Line -> glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            case Point -> glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
        }
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

        switch (state.frontFace()) {
            case Clockwise -> glFrontFace(GL_CW);
            case CounterClockwise -> glFrontFace(GL_CCW);
        }

        if (state.depthClampEnable()) {
            glEnable(GL_DEPTH_CLAMP);
        } else {
            glDisable(GL_DEPTH_CLAMP);
        }

        if (state.rasterizerDiscardEnable()) {
            glEnable(GL_RASTERIZER_DISCARD);
        } else {
            glDisable(GL_RASTERIZER_DISCARD);
        }
    }

    private void applyDepthStencilState(DepthStencilState state) {
        if (state.depthTestEnable()) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(toGLCompareOp(state.depthCompareOp()));
        } else {
            glDisable(GL_DEPTH_TEST);
        }

        glDepthMask(state.depthWriteEnable());

        if (state.stencilTestEnable()) {
            glEnable(GL_STENCIL_TEST);
        } else {
            glDisable(GL_STENCIL_TEST);
        }
    }

    private void applyColorBlendState(ColorBlendState state) {
        for (int i = 0; i < state.attachments().size(); i++) {
            ColorBlendAttachment attachment = state.attachments().get(i);
            if (attachment.blendEnable()) {
                glEnablei(GL_BLEND, i);
                glBlendFuncSeparatei(
                        i,
                        toGLBlendFactor(attachment.srcColorBlendFactor()),
                        toGLBlendFactor(attachment.dstColorBlendFactor()),
                        toGLBlendFactor(attachment.srcAlphaBlendFactor()),
                        toGLBlendFactor(attachment.dstAlphaBlendFactor())
                );
                glBlendEquationSeparatei(
                        i,
                        toGLBlendOp(attachment.colorBlendOp()),
                        toGLBlendOp(attachment.alphaBlendOp())
                );
            } else {
                glDisablei(GL_BLEND, i);
            }

            int mask = attachment.colorWriteMask();
            glColorMaski(
                    i,
                    (mask & ColorComponentFlags.R) != 0,
                    (mask & ColorComponentFlags.G) != 0,
                    (mask & ColorComponentFlags.B) != 0,
                    (mask & ColorComponentFlags.A) != 0
            );
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
}
