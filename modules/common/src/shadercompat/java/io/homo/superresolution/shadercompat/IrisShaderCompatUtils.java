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

package io.homo.superresolution.shadercompat;

import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import io.homo.superresolution.common.upscale.AlgorithmManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.shadercompat.mixin.core.ShaderPackAccessor;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class IrisShaderCompatUtils {
    public static @Nullable SRShaderCompatData.WorldProfile getProfileForWorld(SRShaderCompatData data, NamespacedId worldName) {
        String key = Iris.getCurrentPack().isPresent() ?
                ((ShaderPackAccessor) Iris.getCurrentPack().get()).getDimensionMap().get(worldName) :
                null;
        if (key != null)key = key.replace("world","");
        return data.getProfileForWorld(key);
    }

    public static Optional<SRShaderCompatData.WorldProfile> getCurrentConfig() {
        return getCurrentShaderPackConfig().map(data -> getProfileForWorld(data, Iris.getCurrentDimension()));
    }

    public static boolean isFrameGenerationOnlySupported() {
        try {
            return getCurrentConfig()
                    .map(profile -> profile.enabled
                            && profile.upscale != null
                            && profile.upscale.supportsFrameGenerationOnly)
                    .orElse(false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Optional<SRShaderCompatData> getCurrentShaderPackConfig() {
        if (isIrisVulkan() && ShaderCompatHandler.isLoadingShader()) {
            return Optional.ofNullable(ShaderCompatHandler.getShaderCompatData());
        }
        return Optional.ofNullable(
                getCurrentShaderPack().map(
                pack -> ((IrisSRCompatShaderPack) pack)
                        .superresolution$getSuperResolutionComaptConfig()
            ).orElseGet(ShaderCompatHandler::getShaderCompatData)
        );
    }

    public static @NotNull Optional<ShaderPack> getCurrentShaderPack() {
        return Iris.getCurrentPack();
    }

    public static boolean shouldApplySuperResolutionChanges() {
        if (isIrisVulkan() && ShaderCompatHandler.isLoadingShader()) {
            if (ShaderCompatHandler.getCachedShaderPackPath() == null) return false;
            if (ShaderCompatHandler.getShaderCompatData() == null) return true;
        } else if (!(IrisApi.getInstance().isShaderPackInUse() || ShaderCompatHandler.irisHasShaderPack())
                || !getCurrentShaderPack().map(pack -> ((IrisSRCompatShaderPack) pack).superresolution$isSupportsSuperResolution()).orElse(false)) {
            return false;
        }
        return getCurrentConfig().map(profile -> {
            if (!profile.enabled || profile.upscale == null || !profile.upscale.enabled) return false;
            var disabled = profile.upscale.disabledAlgorithms;
            return !isIrisVulkan() || disabled.isEmpty() || !disabled.contains(SuperResolutionConfig.UPSCALE_ALGO.get())
                    || !disabled.contains(SuperResolutionConfig.getDefaultAlgorithm().codeName);
        }).orElse(false);
    }

    public static boolean isIrisVulkan() {
        #if MC_VER == MC_26_3 && IS_VULKAN == 1
        return B3DVulkanBridge.isB3DVulkanBackend();
        #else
        return false;
        #endif
    }

    public static boolean isUpscaleEnabledForPack() {
        return SuperResolutionConfig.isEnableUpscaleOriginal() && (!isIrisVulkan() || shouldApplySuperResolutionChanges());
    }

    public static boolean shouldApplyJitter(AlgorithmDescription<?> algorithm, boolean frameGenerationOnly) {
        return !frameGenerationOnly && (!isIrisVulkan() || AlgorithmManager.supportsJitter(algorithm));
    }

    public static TextureFormat getInternalTextureFormat() {
        if (
                        IrisApi.getInstance().isShaderPackInUse() &&
                        getCurrentShaderPack().isPresent() &&
                        ((IrisSRCompatShaderPack) getCurrentShaderPack().get()).superresolution$isSupportsSuperResolution() &&
                        (((IrisSRCompatShaderPack) getCurrentShaderPack().get()).superresolution$getSuperResolutionComaptConfig() != null || ShaderCompatHandler.getShaderCompatData() != null) &&
                        getCurrentConfig().isPresent() &&
                        getCurrentConfig().get().enabled) {
            return getCurrentConfig().get().upscale.internalFormat;
        }
        return TextureFormat.R11G11B10F;
    }
}
