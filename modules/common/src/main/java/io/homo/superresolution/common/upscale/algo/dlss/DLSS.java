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

package io.homo.superresolution.common.upscale.algo.dlss;

import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.upscale.interoplayer.InteropDispatchResource;
import io.homo.superresolution.common.upscale.interoplayer.InteropResourceAccess;
import io.homo.superresolution.common.upscale.interoplayer.InteropResourceSet;
import io.homo.superresolution.common.upscale.interoplayer.VulkanInteropAlgorithm;
import io.homo.superresolution.common.upscale.interoplayer.VulkanCommandContext;
import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;
import io.homo.superresolution.core.ngx.*;

import static io.homo.superresolution.api.interop.InteropResourceType.*;

public class DLSS extends VulkanInteropAlgorithm {
    private NgxDispatchResources ngxDispatchResource;
    private NgxFeature ngxDlssFeature;
    private NgxParameters ngxParameters;

    private static void closeResource(NgxResourceVK resource) {
        if (resource != null) {
            resource.close();
        }
    }

    private static void requireNgxSuccess(String operation, int result) {
        if (!NgxConstants.succeeded(result)) {
            throw new IllegalStateException(operation + " failed. NGX result: " + result);
        }
    }

    @Override
    protected void createAlgorithmContext(InteropResourceAccess resources) {
        recreateNgxContext(initDesc, resources);
        ngxDispatchResource = new NgxDispatchResources(resources.resources());
    }

    @Override
    protected void destroyAlgorithmContext() {
        destroyNgxContext();
        if (ngxDispatchResource != null) {
            ngxDispatchResource.close();
            ngxDispatchResource = null;
        }
    }

    @Override protected boolean dispatchAlgorithm(InteropDispatchResource<VulkanCommandContext> resource) {
        if (ngxDlssFeature == null || ngxParameters == null) {
            return false;
        }

        try {
            NgxVKDLSSEvalParams evalParams = ngxDispatchResource.evalParams;
            evalParams.feature.sharpness = SuperResolutionConfig.getSharpness();
            evalParams.jitterOffsetX = resource.frameData().jitterOffset().x;
            evalParams.jitterOffsetY = resource.frameData().jitterOffset().y;
            evalParams.renderSubrectDimensions.width = resource.frameData().renderWidth();
            evalParams.renderSubrectDimensions.height = resource.frameData().renderHeight();
            evalParams.motionVectorScaleX = resource.frameData().renderSize().x;
            evalParams.motionVectorScaleY = resource.frameData().renderSize().y;
            evalParams.reset = consumeHistoryReset() ? 1 : 0;
            evalParams.preExposure = resource.frameData().preExposure();
            evalParams.exposureScale = 1.0f;
            evalParams.frameTimeDeltaInMsec = resource.frameData().frameTimeDelta();

            int evaluateResult = NgxVulkan.evaluateDLSS(
                    resource.commandContext().nativeCommandBuffer().address(),
                    ngxDlssFeature,
                    ngxParameters,
                    evalParams
            );
            if (!NgxConstants.succeeded(evaluateResult)) {
                SuperResolution.LOGGER.error("NGX DLSS evaluation failed. Result: {}", evaluateResult);
                return false;
            }
        } catch (Exception e) {
            SuperResolution.LOGGER.error("NGX DLSS evaluation failed.", e);
            return false;
        }
        return true;
    }

    private void recreateNgxContext(InitializationDescription desc, InteropResourceAccess resources) {
        if (!NgxInitializer.initializeIfSupported()) {
            NgxInitializer.shutdown();
            if (!NgxInitializer.initializeIfSupported()) {
                throw new IllegalStateException("NGX is unavailable for the current GPU");
            }
        }

        NgxParameters parameters = new NgxParameters();
        ngxParameters = parameters;
        int parametersResult = NgxVulkan.getCapabilityParameters(parameters);
        requireNgxSuccess("NVSDK_NGX_VULKAN_GetCapabilityParameters", parametersResult);

        NgxFeature feature = new NgxFeature();
        ngxDlssFeature = feature;
        configureNgxRenderPreset(parameters);

        NgxDLSSCreateParams createParams = new NgxDLSSCreateParams();
        createParams.feature.width = resources.executionTexture(Color).getWidth();
        createParams.feature.height = resources.executionTexture(Color).getHeight();
        createParams.feature.targetWidth = resources.outputTexture().getWidth();
        createParams.feature.targetHeight = resources.outputTexture().getHeight();
        createParams.featureCreateFlags = createNgxFeatureFlags(desc);

        executeInteropCommands(commandContext -> requireNgxSuccess(
                "NGX_VULKAN_CREATE_DLSS_EXT", NgxVulkan.createDLSS(
                        commandContext.nativeHandle(), 1, 1, feature, parameters, createParams)));
    }

    private int createNgxFeatureFlags(InitializationDescription desc) {
        int flags = NgxConstants.DLSS_FLAG_MV_LOW_RES;
        if (desc.isAutoExposure()) {
            flags |= NgxConstants.DLSS_FLAG_AUTO_EXPOSURE;
        }
        if (desc.isHdrInput()) {
            flags |= NgxConstants.DLSS_FLAG_HDR;
        }
        if (desc.isMotionJittered()) {
            flags |= NgxConstants.DLSS_FLAG_MV_JITTERED;
        }
        if (desc.isDepthInverted()) {
            flags |= NgxConstants.DLSS_FLAG_DEPTH_INVERTED;
        }
        return flags;
    }

    private void configureNgxRenderPreset(NgxParameters parameters) {
        int preset = SuperResolutionConfig.SPECIAL.DLSS.RENDER_PRESET.get().getCode();
        parameters.setInt("DLSS.Hint.Render.Preset.DLAA", preset);
        parameters.setInt("DLSS.Hint.Render.Preset.Quality", preset);
        parameters.setInt("DLSS.Hint.Render.Preset.Balanced", preset);
        parameters.setInt("DLSS.Hint.Render.Preset.Performance", preset);
        parameters.setInt("DLSS.Hint.Render.Preset.UltraPerformance", preset);
        parameters.setInt("DLSS.Hint.Render.Preset.UltraQuality", preset);
    }

    private void destroyNgxContext() {
        if (ngxDlssFeature != null) {
            int result = ngxDlssFeature.release();
            if (!NgxConstants.succeeded(result)) {
                throw new IllegalStateException("Failed to release the DLSS NGX feature: " + result);
            }
            ngxDlssFeature = null;
        }
        if (ngxParameters != null) {
            int result = ngxParameters.destroy();
            if (!NgxConstants.succeeded(result)) {
                throw new IllegalStateException("Failed to destroy the DLSS NGX parameters: " + result);
            }
            ngxParameters = null;
        }
    }

    private NgxResourceVK createNgxTextureResource(VulkanTexture texture, boolean readWrite) {
        if (texture == null) {
            return null;
        }
        NgxImageSubresourceRange subresourceRange = new NgxImageSubresourceRange();
        subresourceRange.aspectMask = texture.getAspectMask();
        subresourceRange.baseMipLevel = 0;
        subresourceRange.levelCount = texture.getMipmapSettings().getLevels();
        subresourceRange.baseArrayLayer = 0;
        subresourceRange.layerCount = 1;
        return NgxVulkan.createImageViewResourceVK(
                texture.getImageView(),
                texture.handle(),
                subresourceRange,
                texture.getTextureFormat().vk(),
                texture.getWidth(),
                texture.getHeight(),
                readWrite
        );
    }


    private final class NgxDispatchResources implements AutoCloseable {
        private final NgxVKDLSSEvalParams evalParams = new NgxVKDLSSEvalParams();
        private NgxResourceVK color;
        private NgxResourceVK depth;
        private NgxResourceVK motionVectors;
        private NgxResourceVK exposure;
        private NgxResourceVK output;

        private NgxDispatchResources(InteropResourceSet resourceSet) {
            try {
                color = createNgxTextureResource((VulkanTexture) resourceSet.get(Color), true);
                depth = createNgxTextureResource((VulkanTexture) resourceSet.get(Depth), false);
                motionVectors = createNgxTextureResource((VulkanTexture) resourceSet.get(MotionVectors), true);
                exposure = createNgxTextureResource((VulkanTexture) resourceSet.get(Exposure), false);
                output = createNgxTextureResource((VulkanTexture) resourceSet.get(OutputColor), true);

                evalParams.feature.inputColor = color;
                evalParams.feature.output = output;
                evalParams.depth = depth;
                evalParams.motionVectors = motionVectors;
                evalParams.exposureTexture = exposure;
            } catch (RuntimeException | Error e) {
                close();
                throw e;
            }
        }

        @Override
        public void close() {
            closeResource(output);
            closeResource(exposure);
            closeResource(motionVectors);
            closeResource(depth);
            closeResource(color);
            output = null;
            exposure = null;
            motionVectors = null;
            depth = null;
            color = null;
        }
    }
}
