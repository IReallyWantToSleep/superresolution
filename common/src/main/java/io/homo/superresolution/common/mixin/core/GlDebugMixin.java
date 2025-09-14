/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.common.mixin.core;
#if MC_VER < MC_1_21_5

import com.mojang.blaze3d.platform.GlDebug;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlDebug.class)
public class GlDebugMixin {
    private static Logger LOGGER = LoggerFactory.getLogger("OpenGLDebug");

    @Inject(method = "printDebugLog", at = @At("TAIL"))
    private static void printGlErrorStackTrace(int source, int type, int id, int severity, int messageLength, long message, long userParam, CallbackInfo ci) {
        if (!SuperResolutionConfig.isEnableDebug()) return;
        StackTraceElement[] elements = SuperResolution.renderThread.getStackTrace();
        LOGGER.error("OpenGL Error!");
        for (StackTraceElement element : elements) {
            LOGGER.error("    {}", element.toString());
        }

    }
}
#else

import com.mojang.blaze3d.opengl.GlDebug;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GlDebug.class)
public class GlDebugMixin {
    private static Logger LOGGER = LoggerFactory.getLogger("OpenGLDebug");

    @Inject(method = "printDebugLog", at = @At("TAIL"))
    private void printGlErrorStackTrace(int source, int type, int id, int severity, int messageLength, long message, long userParam, CallbackInfo ci) {
        if (!SuperResolutionConfig.isEnableDebug()) return;
        StackTraceElement[] elements = SuperResolution.renderThread.getStackTrace();
        LOGGER.error("OpenGL Error!");
        for (StackTraceElement element : elements) {
            LOGGER.error("    {}", element.toString());
        }
    }
}
#endif
