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

package io.homo.superresolution.core.graphics.impl.device;

import io.homo.superresolution.core.graphics.impl.buffer.BufferDescription;
import io.homo.superresolution.core.graphics.impl.buffer.IBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.command.ICommandDecoder;
import io.homo.superresolution.core.graphics.impl.pipeline.ComputePipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderDescription;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureDescription;
import io.homo.superresolution.core.graphics.impl.vertex.IVertexBuffer;
import io.homo.superresolution.core.graphics.impl.vertex.VertexBufferDescription;

public interface IDevice {
    /**
     * 创建一个纹理。
     *
     * @param description 纹理描述对象
     * @return 新创建的纹理对象
     */
    ITexture createTexture(TextureDescription description);

    /**
     * 创建一个着色器程序。
     *
     * @param description 着色器描述对象
     * @return 新创建的着色器程序对象
     */
    IShaderProgram createShaderProgram(ShaderDescription description);

    /**
     * 创建顶点缓冲区
     *
     * @param description 顶点缓冲区描述对象，包含缓冲区大小和用途等参数
     * @return 新创建的顶点缓冲区对象
     */
    IVertexBuffer createVertexBuffer(VertexBufferDescription description);

    /**
     * 创建缓冲区
     *
     * @param description 缓冲区描述对象，包含缓冲区大小和用途等参数
     * @return 新创建的缓冲区对象
     */
    IBuffer createBuffer(BufferDescription description);

    /**
     * 创建 RenderPass
     *
     * @param builder RenderPass构建器
     * @return 新创建的RenderPass对象
     */
    RenderPass createRenderPass(RenderPass.Builder builder);

    /**
     * 创建 PipelineDescriptorSet
     *
     * @param shader 着色器程序
     * @return 新创建的 PipelineDescriptorSet 对象
     */
    PipelineDescriptorSet createDescriptorSet(IShaderProgram shader);

    /**
     * 创建 ComputePipeline
     *
     * @param builder ComputePipeline构建器
     * @return 新创建的 ComputePipeline 对象
     */
    ComputePipeline createComputePipeline(ComputePipeline.Builder builder);

    /**
     * 创建 GraphicsPipeline
     *
     * @param builder GraphicsPipeline构建器
     * @return 新创建的 GraphicsPipeline 对象
     */
    GraphicsPipeline createGraphicsPipeline(GraphicsPipeline.Builder builder);

    ICommandBuffer createCommandBuffer();

    /**
     * 获取命令解码器
     */
    ICommandDecoder commandDecoder();

    /**
     * 提交命令缓冲区
     */
    void submitCommandBuffer(ICommandBuffer commandBuffer);

}