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

package io.homo.superresolution.core.graphics.vulkan;

import io.homo.superresolution.core.graphics.impl.pipeline.GraphicsPipeline;
import io.homo.superresolution.core.graphics.impl.pipeline.PipelineDescriptorSet;
import io.homo.superresolution.core.graphics.impl.pipeline.RenderPass;
import io.homo.superresolution.core.graphics.impl.pipeline.state.*;
import io.homo.superresolution.core.graphics.impl.shader.IShaderProgram;
import io.homo.superresolution.core.graphics.impl.shader.ShaderType;
import io.homo.superresolution.core.graphics.impl.vertex.PrimitiveType;
import io.homo.superresolution.core.graphics.impl.vertex.VertexAttributeFormat;
import io.homo.superresolution.core.graphics.impl.vertex.VertexFormat;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static io.homo.superresolution.core.graphics.vulkan.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanGraphicsPipeline extends GraphicsPipeline {
    private final VulkanDevice device;
    private long pipelineLayout = VK_NULL_HANDLE;
    private long pipeline = VK_NULL_HANDLE;
    private long associatedRenderPass = VK_NULL_HANDLE;

    public VulkanGraphicsPipeline(VulkanDevice device,
                                  IShaderProgram shader,
                                  RenderPass renderPass,
                                  RasterizationState rasterization,
                                  DepthStencilState depthStencil,
                                  ColorBlendState colorBlend,
                                  DynamicStateFlags dynamicStates,
                                  PrimitiveType primitiveType,
                                  VertexFormat vertexFormat,
                                  PipelineDescriptorSet descriptorSet) {
        super(shader, renderPass, rasterization, depthStencil, colorBlend, dynamicStates, primitiveType, vertexFormat, descriptorSet);
        this.device = device;

        if (!(shader instanceof VulkanShaderProgram vkShader)) {
            throw new IllegalArgumentException("Shader must be a VulkanShaderProgram");
        }
        if (!(descriptorSet instanceof VulkanPipelineDescriptorSet vkDescSet)) {
            throw new IllegalArgumentException("DescriptorSet must be a VulkanPipelineDescriptorSet");
        }
        if (!(renderPass instanceof VulkanRenderPass vkRenderPass)) {
            throw new IllegalArgumentException("RenderPass must be a VulkanRenderPass");
        }

        if (!vkShader.isCompiled()) {
            vkShader.compile();
        }

        createPipelineLayout(vkDescSet);
        ensurePipelineCreated(vkRenderPass.getRenderPass());
    }

    private static int toVkFormat(VertexAttributeFormat format) {
        return switch (format) {
            case FLOAT -> VK_FORMAT_R32_SFLOAT;
            case FLOAT2 -> VK_FORMAT_R32G32_SFLOAT;
            case FLOAT3 -> VK_FORMAT_R32G32B32_SFLOAT;
            case FLOAT4 -> VK_FORMAT_R32G32B32A32_SFLOAT;
            case INT -> VK_FORMAT_R32_SINT;
            case INT2 -> VK_FORMAT_R32G32_SINT;
            case INT3 -> VK_FORMAT_R32G32B32_SINT;
            case INT4 -> VK_FORMAT_R32G32B32A32_SINT;
            case UINT -> VK_FORMAT_R32_UINT;
            case UINT2 -> VK_FORMAT_R32G32_UINT;
            case UINT3 -> VK_FORMAT_R32G32B32_UINT;
            case UINT4 -> VK_FORMAT_R32G32B32A32_UINT;
            case BYTE4_NORMALIZED -> VK_FORMAT_R8G8B8A8_SNORM;
            case UBYTE4_NORMALIZED -> VK_FORMAT_R8G8B8A8_UNORM;
            case SHORT2 -> VK_FORMAT_R16G16_SINT;
            case SHORT4 -> VK_FORMAT_R16G16B16A16_SINT;
            case USHORT2 -> VK_FORMAT_R16G16_UINT;
            case USHORT4 -> VK_FORMAT_R16G16B16A16_UINT;
        };
    }

    private static int toVkPolygonMode(PolygonMode mode) {
        return switch (mode) {
            case Fill -> VK_POLYGON_MODE_FILL;
            case Line -> VK_POLYGON_MODE_LINE;
            case Point -> VK_POLYGON_MODE_POINT;
        };
    }

    private static int toVkCullMode(CullMode mode) {
        return switch (mode) {
            case None -> VK_CULL_MODE_NONE;
            case Front -> VK_CULL_MODE_FRONT_BIT;
            case Back -> VK_CULL_MODE_BACK_BIT;
            case FrontAndBack -> VK_CULL_MODE_FRONT_AND_BACK;
        };
    }

    private static int toVkCompareOp(CompareOp op) {
        return switch (op) {
            case Never -> VK_COMPARE_OP_NEVER;
            case Less -> VK_COMPARE_OP_LESS;
            case Equal -> VK_COMPARE_OP_EQUAL;
            case LessEqual -> VK_COMPARE_OP_LESS_OR_EQUAL;
            case Greater -> VK_COMPARE_OP_GREATER;
            case NotEqual -> VK_COMPARE_OP_NOT_EQUAL;
            case GreaterEqual -> VK_COMPARE_OP_GREATER_OR_EQUAL;
            case Always -> VK_COMPARE_OP_ALWAYS;
        };
    }

    private static int toVkBlendFactor(BlendFactor factor) {
        return switch (factor) {
            case Zero -> VK_BLEND_FACTOR_ZERO;
            case One -> VK_BLEND_FACTOR_ONE;
            case SrcColor -> VK_BLEND_FACTOR_SRC_COLOR;
            case OneMinusSrcColor -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
            case DstColor -> VK_BLEND_FACTOR_DST_COLOR;
            case OneMinusDstColor -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
            case SrcAlpha -> VK_BLEND_FACTOR_SRC_ALPHA;
            case OneMinusSrcAlpha -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            case DstAlpha -> VK_BLEND_FACTOR_DST_ALPHA;
            case OneMinusDstAlpha -> VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
            case ConstantColor -> VK_BLEND_FACTOR_CONSTANT_COLOR;
            case OneMinusConstantColor -> VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR;
            case ConstantAlpha -> VK_BLEND_FACTOR_CONSTANT_ALPHA;
            case OneMinusConstantAlpha -> VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA;
            case SrcAlphaSaturate -> VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
        };
    }

    private static int toVkBlendOp(BlendOp op) {
        return switch (op) {
            case Add -> VK_BLEND_OP_ADD;
            case Subtract -> VK_BLEND_OP_SUBTRACT;
            case ReverseSubtract -> VK_BLEND_OP_REVERSE_SUBTRACT;
            case Min -> VK_BLEND_OP_MIN;
            case Max -> VK_BLEND_OP_MAX;
        };
    }

    private static int toVkPrimitiveTopology(PrimitiveType type) {
        return switch (type) {
            case Triangle -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            case TriangleStrip -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
            case Lines -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case Points -> VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
        };
    }

    private static int toVkStencilOp(StencilOp op) {
        return switch (op) {
            case Keep -> VK_STENCIL_OP_KEEP;
            case Zero -> VK_STENCIL_OP_ZERO;
            case Replace -> VK_STENCIL_OP_REPLACE;
            case IncrementAndClamp -> VK_STENCIL_OP_INCREMENT_AND_CLAMP;
            case DecrementAndClamp -> VK_STENCIL_OP_DECREMENT_AND_CLAMP;
            case Invert -> VK_STENCIL_OP_INVERT;
            case IncrementAndWrap -> VK_STENCIL_OP_INCREMENT_AND_WRAP;
            case DecrementAndWrap -> VK_STENCIL_OP_DECREMENT_AND_WRAP;
        };
    }

    private void createPipelineLayout(VulkanPipelineDescriptorSet descriptorSet) {
        try (MemoryStack stack = stackPush()) {
            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(descriptorSet.getDescriptorSetLayout()));

            LongBuffer pLayout = stack.mallocLong(1);
            VK_CHECK(vkCreatePipelineLayout(device.getVkDevice(), layoutInfo, null, pLayout),
                    "Failed to create graphics pipeline layout");
            pipelineLayout = pLayout.get(0);
        }
    }

    public void ensurePipelineCreated(long renderPassHandle) {
        if (pipeline != VK_NULL_HANDLE && associatedRenderPass == renderPassHandle) {
            return;
        }
        if (pipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(device.getVkDevice(), pipeline, null);
            pipeline = VK_NULL_HANDLE;
        }
        createPipeline(renderPassHandle);
        associatedRenderPass = renderPassHandle;
    }

    private void createPipeline(long renderPassHandle) {
        VulkanShaderProgram vkShader = (VulkanShaderProgram) shader();

        try (MemoryStack stack = stackPush()) {
            // Shader stages
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vkShader.getShaderModule(ShaderType.Vertex))
                    .pName(stack.UTF8("main"));
            stages.get(1)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(vkShader.getShaderModule(ShaderType.Fragment))
                    .pName(stack.UTF8("main"));

            // Vertex input
            VertexFormat vf = vertexFormat();
            VkVertexInputBindingDescription.Buffer bindingDesc = VkVertexInputBindingDescription.calloc(1, stack)
                    .binding(0)
                    .stride(vf.stride())
                    .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attrDescs =
                    VkVertexInputAttributeDescription.calloc(vf.attributes().size(), stack);
            for (int i = 0; i < vf.attributes().size(); i++) {
                VertexFormat.VertexAttribute attr = vf.attributes().get(i);
                attrDescs.get(i)
                        .binding(0)
                        .location(attr.location())
                        .format(toVkFormat(attr.format()))
                        .offset(attr.offset());
            }

            VkPipelineVertexInputStateCreateInfo vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(bindingDesc)
                    .pVertexAttributeDescriptions(attrDescs);

            // Input assembly
            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(toVkPrimitiveTopology(primitiveType()))
                    .primitiveRestartEnable(false);

            // Viewport/scissor (dynamic)
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .viewportCount(1)
                    .scissorCount(1);

            // Rasterization
            RasterizationState rast = rasterization();
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(rast.depthClampEnable())
                    .rasterizerDiscardEnable(rast.rasterizerDiscardEnable())
                    .polygonMode(toVkPolygonMode(rast.polygonMode()))
                    .cullMode(toVkCullMode(rast.cullMode()))
                    .frontFace(rast.frontFace() == FrontFace.Clockwise ? VK_FRONT_FACE_CLOCKWISE : VK_FRONT_FACE_COUNTER_CLOCKWISE)
                    .depthBiasEnable(false)
                    .lineWidth(1.0f);

            // Multisampling
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                    .sampleShadingEnable(false);

            // Depth stencil
            DepthStencilState ds = depthStencil();
            VkPipelineDepthStencilStateCreateInfo depthStencilInfo = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(ds.depthTestEnable())
                    .depthWriteEnable(ds.depthWriteEnable())
                    .depthCompareOp(toVkCompareOp(ds.depthCompareOp()))
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(ds.stencilTestEnable());
            depthStencilInfo.front()
                    .failOp(toVkStencilOp(ds.stencilFailOpFront()))
                    .passOp(toVkStencilOp(ds.stencilPassOpFront()))
                    .depthFailOp(toVkStencilOp(ds.stencilDepthFailOpFront()))
                    .compareOp(toVkCompareOp(ds.stencilCompareOpFront()))
                    .compareMask(ds.stencilCompareMask())
                    .writeMask(ds.stencilWriteMask())
                    .reference(ds.stencilReference());
            depthStencilInfo.back()
                    .failOp(toVkStencilOp(ds.stencilFailOpBack()))
                    .passOp(toVkStencilOp(ds.stencilPassOpBack()))
                    .depthFailOp(toVkStencilOp(ds.stencilDepthFailOpBack()))
                    .compareOp(toVkCompareOp(ds.stencilCompareOpBack()))
                    .compareMask(ds.stencilCompareMask())
                    .writeMask(ds.stencilWriteMask())
                    .reference(ds.stencilReference());

            // Color blend
            ColorBlendState blend = colorBlend();
            VkPipelineColorBlendAttachmentState.Buffer blendAttachments =
                    VkPipelineColorBlendAttachmentState.calloc(Math.max(1, blend.attachments().size()), stack);
            if (blend.attachments().isEmpty()) {
                blendAttachments.get(0)
                        .blendEnable(false)
                        .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                                VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
            } else {
                for (int i = 0; i < blend.attachments().size(); i++) {
                    ColorBlendAttachment att = blend.attachments().get(i);
                    blendAttachments.get(i)
                            .blendEnable(att.blendEnable())
                            .srcColorBlendFactor(toVkBlendFactor(att.srcColorBlendFactor()))
                            .dstColorBlendFactor(toVkBlendFactor(att.dstColorBlendFactor()))
                            .colorBlendOp(toVkBlendOp(att.colorBlendOp()))
                            .srcAlphaBlendFactor(toVkBlendFactor(att.srcAlphaBlendFactor()))
                            .dstAlphaBlendFactor(toVkBlendFactor(att.dstAlphaBlendFactor()))
                            .alphaBlendOp(toVkBlendOp(att.alphaBlendOp()))
                            .colorWriteMask(att.colorWriteMask());
                }
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .pAttachments(blendAttachments);

            // Dynamic states
            List<Integer> dynStates = new ArrayList<>();
            DynamicStateFlags dsf = dynamicStates();
            if (dsf.has(DynamicStateFlags.Viewport)) {
                dynStates.add(VK_DYNAMIC_STATE_VIEWPORT);
            }
            if (dsf.has(DynamicStateFlags.Scissor)) {
                dynStates.add(VK_DYNAMIC_STATE_SCISSOR);
            }
            if (dsf.has(DynamicStateFlags.LineWidth)) {
                dynStates.add(VK_DYNAMIC_STATE_LINE_WIDTH);
            }
            if (dsf.has(DynamicStateFlags.BlendConstants)) {
                dynStates.add(VK_DYNAMIC_STATE_BLEND_CONSTANTS);
            }

            var dynamicStatesBuffer = stack.mallocInt(dynStates.size());
            for (int ds2 : dynStates) dynamicStatesBuffer.put(ds2);
            dynamicStatesBuffer.flip();

            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(dynamicStatesBuffer);

            // Create pipeline
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(stages)
                    .pVertexInputState(vertexInput)
                    .pInputAssemblyState(inputAssembly)
                    .pViewportState(viewportState)
                    .pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling)
                    .pDepthStencilState(depthStencilInfo)
                    .pColorBlendState(colorBlending)
                    .pDynamicState(dynamicState)
                    .layout(pipelineLayout)
                    .renderPass(renderPassHandle)
                    .subpass(0);

            LongBuffer pPipeline = stack.mallocLong(1);
            VK_CHECK(vkCreateGraphicsPipelines(device.getVkDevice(), VK_NULL_HANDLE, pipelineInfo, null, pPipeline),
                    "Failed to create graphics pipeline");
            pipeline = pPipeline.get(0);
        }
    }

    public long getPipeline() {
        return pipeline;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }

    @Override
    public void destroy() {
        if (pipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(device.getVkDevice(), pipeline, null);
            pipeline = VK_NULL_HANDLE;
        }
        if (pipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(device.getVkDevice(), pipelineLayout, null);
            pipelineLayout = VK_NULL_HANDLE;
        }
    }
}
