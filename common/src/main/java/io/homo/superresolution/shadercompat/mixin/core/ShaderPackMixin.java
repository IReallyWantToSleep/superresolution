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

package io.homo.superresolution.shadercompat.mixin.core;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRCompatConfigParser;
import io.homo.superresolution.common.minecraft.handler.shadercompat.SRShaderCompatData;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import io.homo.superresolution.shadercompat.IrisSRCompatShaderPack;
import net.irisshaders.iris.shaderpack.ShaderPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

@Mixin(value = ShaderPack.class, remap = false)
public class ShaderPackMixin implements IrisSRCompatShaderPack {
    @Unique
    private SRShaderCompatData superresolution$config;

    #if MC_VER > MC_1_20_6
    @Inject(method = "<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;Z)V", at = @At("RETURN"), remap = false)
    private void loadSuperResolutionComaptConfig(
            Path root,
            Map<?, ?> changedConfigs,
            ImmutableList<?> environmentDefines,
            boolean isZip,
            CallbackInfo ci
    )
    #else
    @Inject(method = "<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;)V", at = @At("RETURN"), remap = false)
    private void loadSuperResolutionComaptConfig(
            Path root,
            Map<?, ?> changedConfigs,
            ImmutableList<?> environmentDefines,
            CallbackInfo ci
    )
    #endif {
        ShaderCompatHandler.setLoadingShader(true);
        try {
            Path srConfigPath = root.resolve("superresolution.json");
            if (Files.exists(srConfigPath)) {

                superresolution$config = SRCompatConfigParser.load(srConfigPath);
                SuperResolution.LOGGER.info("光影包 {} 支持超分辨率功能", root);
                return;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            SuperResolution.LOGGER.warn("加载 {} 光影包中的 superresolution.json 时发生错误", root);
        }
        superresolution$config = null;
    }

    @Unique
    public SRShaderCompatData superresolution$getSuperResolutionComaptConfig() {
        return SuperResolutionConfig.isForceDisableShaderCompat() ? null : superresolution$config;
    }

    @Unique
    public boolean superresolution$isSupportsSuperResolution() {
        return !SuperResolutionConfig.isForceDisableShaderCompat() && superresolution$config != null;
    }
}
