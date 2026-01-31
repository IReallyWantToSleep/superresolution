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

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
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
        String key = Optional.ofNullable(
                ((ShaderPackAccessor) Iris.getCurrentPack().orElseThrow())
                        .getDimensionMap().get(worldName)).orElseGet(() -> {
                    //SuperResolution.LOGGER.warn("无法在当前光影包 {} 的维度映射中找到维度 {} 的名称映射，使用默认名称", Iris.getCurrentPackName(), worldName.getName());
                    return null;
                }
        );
        return data.getProfileForWorld(key);
    }

    public static Optional<SRShaderCompatData.WorldProfile> getCurrentConfig() {
        return Optional.ofNullable(
                getProfileForWorld(
                        getCurrentShaderPack().map(
                                pack -> ((IrisSRCompatShaderPack) pack)
                                        .superresolution$getSuperResolutionComaptConfig()
                        ).orElseThrow(),
                        Iris.getCurrentDimension()
                )
        );
    }

    public static Optional<SRShaderCompatData> getCurrentShaderPackConfig() {
        return getCurrentShaderPack().map(
                pack -> ((IrisSRCompatShaderPack) pack)
                        .superresolution$getSuperResolutionComaptConfig()
        );
    }

    public static @NotNull Optional<ShaderPack> getCurrentShaderPack() {
        return Iris.getCurrentPack();
    }

    public static boolean shouldApplySuperResolutionChanges() {
        return !SuperResolutionConfig.isForceDisableShaderCompat() && IrisApi.getInstance().isShaderPackInUse() && getCurrentShaderPack().isPresent() &&
                ((IrisSRCompatShaderPack) getCurrentShaderPack().get()).superresolution$isSupportsSuperResolution()
                && getCurrentConfig().isPresent()
                && getCurrentConfig().get().enabled
                && getCurrentConfig().get().upscale.enabled;
    }

    public static TextureFormat getInternalTextureFormat() {
        if (
                !SuperResolutionConfig.isForceDisableShaderCompat() &&
                        IrisApi.getInstance().isShaderPackInUse() &&
                        getCurrentShaderPack().isPresent() &&
                        ((IrisSRCompatShaderPack) getCurrentShaderPack().get()).superresolution$isSupportsSuperResolution() &&
                        ((IrisSRCompatShaderPack) getCurrentShaderPack().get()).superresolution$getSuperResolutionComaptConfig() != null &&
                        getCurrentConfig().isPresent() &&
                        getCurrentConfig().get().enabled) {
            return getCurrentConfig().get().upscale.internalFormat;
        }
        return TextureFormat.R11G11B10F;
    }
}
