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

import io.homo.superresolution.core.graphics.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.core.graphics.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.graphics.glslang.enums.*;
import io.homo.superresolution.core.graphics.impl.shader.*;
import io.homo.superresolution.core.graphics.shader.ShaderCompiler;
import io.homo.superresolution.core.SuperResolutionNative;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.EnumMap;
import java.util.Map;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanShaderProgram implements IShaderProgram {
    private static final Logger LOGGER = LoggerFactory.getLogger(VulkanShaderProgram.class);

    private final VulkanDevice device;
    private final ShaderDescription description;
    private final EnumMap<ShaderType, Long> shaderModules = new EnumMap<>(ShaderType.class);
    private boolean compiled = false;

    public VulkanShaderProgram(VulkanDevice device, ShaderDescription description) {
        this.device = device;
        this.description = description;
    }

    @Override
    public void compile() {
        if (compiled) return;

        for (Map.Entry<ShaderType, ShaderSource> entry : description.sourceMap().entrySet()) {
            ShaderType type = entry.getKey();
            ShaderSource source = entry.getValue();
            source.addDefine("SR_VULKAN", "1");
            long module = compileShaderModule(type, source);
            shaderModules.put(type, module);
        }
        compiled = true;
    }

    private long compileShaderModule(ShaderType type, ShaderSource source) {
        ByteBuffer spirvBuffer = null;
        boolean needsFreeNativeBuffer = false;

        try {
            if (ShaderCompiler.checkVulkanProgramBinary(this)) {
                ShaderCompiler.ShaderBinary cached = ShaderCompiler.getVulkanShaderBinary(this, type);
                if (cached != null) {
                    spirvBuffer = cached.binary();
                    long module = createShaderModule(spirvBuffer);
                    cached.close();
                    return module;
                }
            }

            String glslSource = source.getSource();
            EShLanguage stage = switch (type) {
                case Vertex -> EShLanguage.EShLangVertex;
                case Fragment -> EShLanguage.EShLangFragment;
                case Compute -> EShLanguage.EShLangCompute;
            };

            GlslangCompileShaderResult result = GlslangShaderCompiler.compileShaderToSpirv(
                    glslSource,
                    stage,
                    EShSource.EShSourceGlsl,
                    EShClient.EShClientVulkan,
                    EShTargetClientVersion.EShTargetVulkan_1_2,
                    EShTargetLanguage.EShTargetSpv,
                    EShTargetLanguageVersion.EShTargetSpv_1_4,
                    460,
                    EProfile.ENoProfile,
                    true,
                    false
            );

            if (result.error() != GlslangCompileShaderError.OK) {
                LOGGER.error("Vulkan shader compilation failed for {} [{}]: {}",
                        description.shaderName(), type.name(), result.log());
                throw new ShaderCompileException(
                        "Vulkan shader compilation failed: " + result.log());
            }

            spirvBuffer = result.spirvBuffer();
            needsFreeNativeBuffer = true;

            if (spirvBuffer == null || result.spirVDataSize() <= 0) {
                throw new ShaderCompileException("SPIR-V output is empty for " + type.name());
            }

            long module = createShaderModule(spirvBuffer);

            ShaderCompiler.saveVulkanProgramBinary(this);

            return module;
        } finally {
            if (needsFreeNativeBuffer && spirvBuffer != null) {
                SuperResolutionNative.freeDirectBuffer(spirvBuffer);
            }
        }
    }

    private long createShaderModule(ByteBuffer spirvCode) {
        try (MemoryStack stack = stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(spirvCode);

            LongBuffer pModule = stack.mallocLong(1);
            VK_CHECK(vkCreateShaderModule(device.getVkDevice(), createInfo, null, pModule),
                    "Failed to create shader module");
            return pModule.get(0);
        }
    }

    @Override
    public boolean isCompiled() {
        return compiled;
    }

    @Override
    public void destroy() {
        for (Map.Entry<ShaderType, Long> entry : shaderModules.entrySet()) {
            if (entry.getValue() != VK_NULL_HANDLE) {
                vkDestroyShaderModule(device.getVkDevice(), entry.getValue(), null);
            }
        }
        shaderModules.clear();
        compiled = false;
    }

    @Override
    public ShaderDescription getDescription() {
        return description;
    }

    @Override
    public long handle() {
        return shaderModules.values().stream().findFirst().orElse(VK_NULL_HANDLE);
    }

    public long getShaderModule(ShaderType type) {
        Long module = shaderModules.get(type);
        if (module == null) {
            throw new IllegalStateException("Shader module not found for type: " + type);
        }
        return module;
    }

    public VulkanDevice getDevice() {
        return device;
    }
}
