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

#if MC_VER > MC_1_21_5 && MC_VER < MC_1_21_10

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import net.neoforged.neoforge.client.config.NeoForgeClientConfig;
#endif
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiFunction;

//坏狐狸在高版本引入了ValidationLayer，iris恰好跟这玩意冲突
#if MC_VER > MC_1_21_5 && MC_VER < MC_1_21_10
@Mixin(RenderSystem.class)
public class FuckingValidation1218Mixin {
    @Redirect(method = "initRenderer", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;createGpuDevice(JIZLjava/util/function/BiFunction;Z)Lcom/mojang/blaze3d/systems/GpuDevice;"))
    private static GpuDevice fuckingValidation(
            long window,
            int glDebugVerbosity,
            boolean synchronous,
            #if MC_VER > MC_1_21_10
            BiFunction<net.minecraft.resources.Identifier, ShaderType, String> defaultShaderSource,
            #else
            BiFunction<net.minecraft.resources.ResourceLocation, ShaderType, String> defaultShaderSource,
            #endif
            boolean renderDebugLabels
    ) {
        NeoForgeClientConfig.INSTANCE.enableB3DValidationLayer.set(false);
        return new GlDevice(window, glDebugVerbosity, synchronous, defaultShaderSource, renderDebugLabels);
    }
}

#else
@Mixin(Minecraft.class)
public class FuckingValidation1218Mixin {
}
#endif