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

package io.homo.superresolution.core.graphics.opengl.utils;

import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.FullscreenQuad;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.framebuffer.*;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.state.ColorBlendAttachment;
import io.homo.superresolution.core.graphics.impl.pipeline.state.CullMode;
import io.homo.superresolution.core.graphics.impl.pipeline.state.DynamicStateFlags;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.shader.ShaderSource;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlGraphicsPipeline;
import io.homo.superresolution.core.graphics.opengl.pipeline.GlRenderPass;
import io.homo.superresolution.core.graphics.opengl.shader.GlShaderProgram;

import java.util.List;

public class GlBlitRenderer {
    private static IVertexBuffer fullscreenQuad;
    private static RenderPass renderPass;
    private static IBindableFrameBuffer cachedFrameBuffer = new IBindableFrameBuffer() {
        @Override
        public void bind(FrameBufferBindPoint bindPoint, boolean setViewport) {

        }

        @Override
        public void bind(FrameBufferBindPoint bindPoint) {

        }

        @Override
        public void unbind(FrameBufferBindPoint bindPoint) {

        }

        @Override
        public int getWidth() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public void clearFrameBuffer() {

        }

        @Override
        public List<ColorAttachment> getColorAttachments() {
            return List.of();
        }

        @Override
        public DepthStencilAttachment getDepthStencilAttachment() {
            return null;
        }

        @Override
        public int getTextureId(FrameBufferAttachmentType attachmentType) {
            return 0;
        }

        @Override
        public ITexture getTexture(FrameBufferAttachmentType attachmentType) {
            return null;
        }

        @Override
        public void setClearColorRGBA(float red, float green, float blue, float alpha) {

        }

        @Override
        public TextureFormat getColorTextureFormat() {
            return null;
        }

        @Override
        public TextureFormat getDepthTextureFormat() {
            return null;
        }

        @Override
        public long handle() {
            return 0;
        }

        @Override
        public void destroy() {

        }
    };

    private static RenderPass getOrCreateRenderPass() {
        if (renderPass == null) {
            GlShaderProgram program = RenderSystems.opengl().device().createShaderProgram(
                    ShaderDescription.graphics(
                                    new ShaderSource(ShaderType.Fragment, "/shader/blit.frag.glsl", true),
                                    new ShaderSource(ShaderType.Vertex, "/shader/blit.vert.glsl", true)
                            )
                            .uniformSamplerTexture("uTexture", 0)
                            .build()
            );
            program.compile();

            GlGraphicsPipeline graphicsPipeline = (GlGraphicsPipeline) GlGraphicsPipeline.builder()
                    .shader(program)
                    .rasterization(r -> r.cullMode(CullMode.None))
                    .depthStencil(r -> r.depthTestEnable(false).depthWriteEnable(false).stencilTestEnable(false))
                    .dynamicStates(DynamicStateFlags.Viewport)
                    .colorBlend(r -> r.addAttachment(ColorBlendAttachment.noBlend()))
                    .vertexFormat(FullscreenQuad.getVertexFormat())
                    .build(RenderSystems.opengl().device());

            renderPass = RenderPass.builder()
                    .pipeline(graphicsPipeline)
                    .frameBuffer(cachedFrameBuffer)
                    .build(RenderSystems.opengl().device());
        }
        return renderPass;
    }

    public static void blitToScreen(ITexture textureId, int viewWidth, int viewHeight) {
        if (fullscreenQuad == null) {
            fullscreenQuad = FullscreenQuad.create(RenderSystems.opengl().device());
        }

        GlRenderPass pass = (GlRenderPass) getOrCreateRenderPass();
        pass.pipeline().descriptorSet().samplerTexture("uTexture", textureId);
        pass.pipeline().descriptorSet().update();
        ICommandBuffer commandBuffer = RenderSystems.opengl().device().defaultCommandPool().createCommandBuffer();
        commandBuffer.begin();
        RenderSystems.opengl().device().commandDecoder()
                .draw(
                        commandBuffer,
                        pass,
                        PrimitiveType.TriangleStrip,
                        fullscreenQuad,
                        4,
                        0
                );
        commandBuffer.end();
        RenderSystems.opengl().device().submitCommandBuffer(commandBuffer);
    }
}
