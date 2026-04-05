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

import io.homo.superresolution.core.graphics.impl.sampler.ISampler;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerDescription;
import io.homo.superresolution.core.graphics.impl.sampler.SamplerMipmapMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.nio.LongBuffer;

import static io.homo.superresolution.core.graphics.vulkan.utils.VulkanUtils.VK_CHECK;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSampler implements ISampler {
    private final VulkanDevice device;
    private final SamplerDescription description;
    private long sampler;

    public VulkanSampler(VulkanDevice device, SamplerDescription description) {
        this.device = device;
        this.description = description;

        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(description.getMagFilter().vk())
                    .minFilter(description.getMinFilter().vk())
                    .mipmapMode(description.getMipmapMode().vk())
                    .addressModeU(description.getWrapMode().vk())
                    .addressModeV(description.getWrapMode().vk())
                    .addressModeW(description.getWrapMode().vk())
                    .mipLodBias(description.getLodBias())
                    .anisotropyEnable(description.getMaxAnisotropy() > 1.0f)
                    .maxAnisotropy(description.getMaxAnisotropy())
                    .compareEnable(description.getCompareOp().isPresent())
                    .compareOp(description.getCompareOp().map(op -> op.vk()).orElse(VK_COMPARE_OP_ALWAYS))
                    .minLod(0.0f)
                    .maxLod(description.getMipmapMode() == SamplerMipmapMode.None ? 0.0f : 1000.0f)
                    .borderColor(description.getBorderColor().vk())
                    .unnormalizedCoordinates(false);

            LongBuffer pSampler = stack.mallocLong(1);
            VK_CHECK(vkCreateSampler(device.getVkDevice(), samplerInfo, null, pSampler), "Failed to create sampler");
            sampler = pSampler.get(0);
        }
    }

    @Override
    public SamplerDescription getSamplerDescription() {
        return description;
    }

    @Override
    public long handle() {
        return sampler;
    }

    @Override
    public void destroy() {
        if (sampler != VK_NULL_HANDLE) {
            vkDestroySampler(device.getVkDevice(), sampler, null);
            sampler = VK_NULL_HANDLE;
        }
    }

    @Override
    public String toString() {
        return "VulkanSampler{" +
                "sampler=" + sampler +
                ", description=" + description +
                '}';
    }
}


