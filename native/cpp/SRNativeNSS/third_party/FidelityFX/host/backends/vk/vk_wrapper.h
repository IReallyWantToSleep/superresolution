// SPDX-FileCopyrightText: Copyright 2025-2026 Arm Limited and/or its affiliates <open-source-office@arm.com>
// SPDX-License-Identifier: MIT

/// @defgroup VKBackend Vulkan Backend
/// FidelityFX SDK native backend implementation for Vulkan.
///
/// @ingroup Backends

#pragma once

#include <vulkan/vulkan.h>
#include <FidelityFX/host/backends/vk/ffx_vk.h>
#include <FidelityFX/host/ffx_assert.h>
#include <stdio.h>

/// @brief Vulkan function table containing all function pointers
typedef struct VkFunctionTable
{
    bool initialized = false;

    PFN_vkEnumerateDeviceExtensionProperties                                vkEnumerateDeviceExtensionProperties                                = 0;
    PFN_vkGetPhysicalDeviceProperties                                       vkGetPhysicalDeviceProperties                                       = 0;
    PFN_vkGetPhysicalDeviceMemoryProperties                                 vkGetPhysicalDeviceMemoryProperties                                 = 0;
    PFN_vkGetPhysicalDeviceProperties2                                      vkGetPhysicalDeviceProperties2                                      = 0;
    PFN_vkGetPhysicalDeviceFeatures2                                        vkGetPhysicalDeviceFeatures2                                        = 0;
    PFN_vkGetPhysicalDeviceQueueFamilyProperties                            vkGetPhysicalDeviceQueueFamilyProperties                            = 0;
    PFN_vkGetPhysicalDeviceSurfaceSupportKHR                                vkGetPhysicalDeviceSurfaceSupportKHR                                = 0;
    PFN_vkGetDeviceProcAddr                                                 vkGetDeviceProcAddr                                                 = 0;
    PFN_vkSetDebugUtilsObjectNameEXT                                        vkSetDebugUtilsObjectNameEXT                                        = 0;
    PFN_vkCreateDescriptorPool                                              vkCreateDescriptorPool                                              = 0;
    PFN_vkCreateSampler                                                     vkCreateSampler                                                     = 0;
    PFN_vkCreateDescriptorSetLayout                                         vkCreateDescriptorSetLayout                                         = 0;
    PFN_vkCreateBuffer                                                      vkCreateBuffer                                                      = 0;
    PFN_vkCreateBufferView                                                  vkCreateBufferView                                                  = 0;
    PFN_vkCreateImage                                                       vkCreateImage                                                       = 0;
    PFN_vkCreateImageView                                                   vkCreateImageView                                                   = 0;
    PFN_vkGetImageSubresourceLayout                                         vkGetImageSubresourceLayout                                         = 0;
    PFN_vkCreateShaderModule                                                vkCreateShaderModule                                                = 0;
    PFN_vkCreatePipelineLayout                                              vkCreatePipelineLayout                                              = 0;
    PFN_vkCreateComputePipelines                                            vkCreateComputePipelines                                            = 0;
    PFN_vkCmdPipelineBarrier2                                               vkCmdPipelineBarrier2                                               = 0;
    PFN_vkCmdPushConstants                                                  vkCmdPushConstants                                                  = 0;
    PFN_vkCreateGraphicsPipelines                                           vkCreateGraphicsPipelines                                           = 0;
    PFN_vkCreateRenderPass                                                  vkCreateRenderPass                                                  = 0;
    PFN_vkCreateFramebuffer                                                 vkCreateFramebuffer                                                 = 0;
    PFN_vkDestroyPipelineLayout                                             vkDestroyPipelineLayout                                             = 0;
    PFN_vkDestroyPipeline                                                   vkDestroyPipeline                                                   = 0;
    PFN_vkDestroyRenderPass                                                 vkDestroyRenderPass                                                 = 0;
    PFN_vkDestroyFramebuffer                                                vkDestroyFramebuffer                                                = 0;
    PFN_vkDestroyImage                                                      vkDestroyImage                                                      = 0;
    PFN_vkDestroyImageView                                                  vkDestroyImageView                                                  = 0;
    PFN_vkDestroyBuffer                                                     vkDestroyBuffer                                                     = 0;
    PFN_vkDestroyBufferView                                                 vkDestroyBufferView                                                 = 0;
    PFN_vkDestroyDescriptorSetLayout                                        vkDestroyDescriptorSetLayout                                        = 0;
    PFN_vkDestroyDescriptorPool                                             vkDestroyDescriptorPool                                             = 0;
    PFN_vkDestroySampler                                                    vkDestroySampler                                                    = 0;
    PFN_vkDestroyShaderModule                                               vkDestroyShaderModule                                               = 0;
    PFN_vkGetBufferMemoryRequirements                                       vkGetBufferMemoryRequirements                                       = 0;
    PFN_vkGetBufferMemoryRequirements2                                      vkGetBufferMemoryRequirements2                                      = 0;
    PFN_vkGetImageMemoryRequirements                                        vkGetImageMemoryRequirements                                        = 0;
    PFN_vkGetImageMemoryRequirements2                                       vkGetImageMemoryRequirements2                                       = 0;
    PFN_vkAllocateDescriptorSets                                            vkAllocateDescriptorSets                                            = 0;
    PFN_vkFreeDescriptorSets                                                vkFreeDescriptorSets                                                = 0;
    PFN_vkAllocateMemory                                                    vkAllocateMemory                                                    = 0;
    PFN_vkFreeMemory                                                        vkFreeMemory                                                        = 0;
    PFN_vkMapMemory                                                         vkMapMemory                                                         = 0;
    PFN_vkUnmapMemory                                                       vkUnmapMemory                                                       = 0;
    PFN_vkBindBufferMemory                                                  vkBindBufferMemory                                                  = 0;
    PFN_vkBindImageMemory                                                   vkBindImageMemory                                                   = 0;
    PFN_vkBindImageMemory2                                                  vkBindImageMemory2                                                  = 0;
    PFN_vkUpdateDescriptorSets                                              vkUpdateDescriptorSets                                              = 0;
    PFN_vkFlushMappedMemoryRanges                                           vkFlushMappedMemoryRanges                                           = 0;
    PFN_vkCmdPipelineBarrier                                                vkCmdPipelineBarrier                                                = 0;
    PFN_vkCmdBindPipeline                                                   vkCmdBindPipeline                                                   = 0;
    PFN_vkCmdBindDescriptorSets                                             vkCmdBindDescriptorSets                                             = 0;
    PFN_vkCmdDispatch                                                       vkCmdDispatch                                                       = 0;
    PFN_vkCmdDispatchIndirect                                               vkCmdDispatchIndirect                                               = 0;
    PFN_vkCmdCopyBuffer                                                     vkCmdCopyBuffer                                                     = 0;
    PFN_vkCmdCopyImage                                                      vkCmdCopyImage                                                      = 0;
    PFN_vkCmdCopyBufferToImage                                              vkCmdCopyBufferToImage                                              = 0;
    PFN_vkCmdClearColorImage                                                vkCmdClearColorImage                                                = 0;
    PFN_vkCmdFillBuffer                                                     vkCmdFillBuffer                                                     = 0;
    PFN_vkCmdBeginRenderPass                                                vkCmdBeginRenderPass                                                = 0;
    PFN_vkCmdEndRenderPass                                                  vkCmdEndRenderPass                                                  = 0;
    PFN_vkCmdDraw                                                           vkCmdDraw                                                           = 0;
    PFN_vkWaitSemaphores                                                    vkWaitSemaphores                                                    = 0;
    PFN_vkDestroySwapchainKHR                                               vkDestroySwapchainKHR                                               = 0;
    PFN_vkQueuePresentKHR                                                   vkQueuePresentKHR                                                   = 0;
    PFN_vkCreateSemaphore                                                   vkCreateSemaphore                                                   = 0;
    PFN_vkCreateSwapchainKHR                                                vkCreateSwapchainKHR                                                = 0;
    PFN_vkGetSwapchainImagesKHR                                             vkGetSwapchainImagesKHR                                             = 0;
    PFN_vkDestroySemaphore                                                  vkDestroySemaphore                                                  = 0;
    PFN_vkQueueWaitIdle                                                     vkQueueWaitIdle                                                     = 0;
    PFN_vkDeviceWaitIdle                                                    vkDeviceWaitIdle                                                    = 0;
    PFN_vkAcquireNextImageKHR                                               vkAcquireNextImageKHR                                               = 0;
    PFN_vkFreeCommandBuffers                                                vkFreeCommandBuffers                                                = 0;
    PFN_vkDestroyCommandPool                                                vkDestroyCommandPool                                                = 0;
    PFN_vkCreateCommandPool                                                 vkCreateCommandPool                                                 = 0;
    PFN_vkAllocateCommandBuffers                                            vkAllocateCommandBuffers                                            = 0;
    PFN_vkResetCommandPool                                                  vkResetCommandPool                                                  = 0;
    PFN_vkResetCommandBuffer                                                vkResetCommandBuffer                                                = 0;
    PFN_vkBeginCommandBuffer                                                vkBeginCommandBuffer                                                = 0;
    PFN_vkEndCommandBuffer                                                  vkEndCommandBuffer                                                  = 0;
    PFN_vkGetSemaphoreCounterValue                                          vkGetSemaphoreCounterValue                                          = 0;
    PFN_vkQueueSubmit                                                       vkQueueSubmit                                                       = 0;
    PFN_vkCmdSetViewport                                                    vkCmdSetViewport                                                    = 0;
    PFN_vkCmdSetScissor                                                     vkCmdSetScissor                                                     = 0;
    PFN_vkDestroyFence                                                      vkDestroyFence                                                      = 0;
    PFN_vkGetFenceStatus                                                    vkGetFenceStatus                                                    = 0;
    PFN_vkCreateFence                                                       vkCreateFence                                                       = 0;
    PFN_vkCmdCopyImageToBuffer                                              vkCmdCopyImageToBuffer                                              = 0;
    PFN_vkCmdWriteBufferMarkerAMD                                           vkCmdWriteBufferMarkerAMD                                           = 0;
    PFN_vkCmdWriteBufferMarker2AMD                                          vkCmdWriteBufferMarker2AMD                                          = 0;
    PFN_vkCmdBeginDebugUtilsLabelEXT                                        vkCmdBeginDebugUtilsLabelEXT                                        = 0;
    PFN_vkCmdEndDebugUtilsLabelEXT                                          vkCmdEndDebugUtilsLabelEXT                                          = 0;
    PFN_vkWaitForFences                                                     vkWaitForFences                                                     = 0;
    // VK_ARM_tensors. Still used: several NSS passes take tensor descriptors for the
    // coefficient / feedback tensors. Only the DATA GRAPH half of the Arm ML extensions
    // is gone -- the inference graph now runs on the portable dp4a backend.
    PFN_vkCreateTensorARM                                     vkCreateTensorARM                                     = 0;
    PFN_vkCreateTensorViewARM                                 vkCreateTensorViewARM                                 = 0;
    PFN_vkGetTensorMemoryRequirementsARM                      vkGetTensorMemoryRequirementsARM                      = 0;
    PFN_vkBindTensorMemoryARM                                 vkBindTensorMemoryARM                                 = 0;
    PFN_vkDestroyTensorARM                                    vkDestroyTensorARM                                    = 0;
    PFN_vkDestroyTensorViewARM                                vkDestroyTensorViewARM                                = 0;
} VkFunctionTable;

/// @brief Initialize the Vulkan wrapper with the provided device context.
///
/// This function sets up the Vulkan wrapper system using the specified device context
///
/// @param [in] deviceContext The Vulkan device context containing device and related information.
///
/// @return true if initialization was successful, false otherwise.
bool InitVulkanWrapper(const VkDeviceContext& deviceContext);

/// @brief Returns a reference to the singleton Vulkan function table
/// @return Reference to VkFunctionTable containing all Vulkan function pointers
///
/// Example usage:
/// @code
/// VulkanWrapper().vkCreateRenderPass(device, &createInfo, nullptr, &renderPass);
/// @endcode
const VkFunctionTable& VulkanWrapper();
