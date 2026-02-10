/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.neoforge.mixin.neoforge;


import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;
//坏狐狸在高版本引入了ValidationLayer，iris恰好跟这玩意冲突
#if MC_VER >= MC_1_21_10
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;


@Mixin(ClientHooks.class)
public class FuckingValidationMixin {
    @Inject(method = "createGpuDevice", at = @At("HEAD"), cancellable = true)
    private static void fuckingValidation(
            long window,
            int debugLevel,
            boolean syncDebug,
            #if MC_VER > MC_1_21_10
            com.mojang.blaze3d.shaders.ShaderSource defaultShaderSource,
            #else
            BiFunction<net.minecraft.resources.ResourceLocation, ShaderType, String> defaultShaderSource,
            #endif
            boolean enableDebugLabels,
            CallbackInfoReturnable<GpuDevice> cir
    ) {
        net.neoforged.neoforge.client.config.NeoForgeClientConfig.INSTANCE.enableB3DValidationLayer.set(false);
        cir.setReturnValue(
                new GlDevice(window, debugLevel, syncDebug, defaultShaderSource, enableDebugLabels)
        );
        cir.cancel();
    }
}

#else
@Mixin(Minecraft.class)
public class FuckingValidationMixin {
}
#endif