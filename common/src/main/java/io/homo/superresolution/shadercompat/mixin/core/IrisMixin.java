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

package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Iris.class,remap = false)
public class IrisMixin {

    @Inject(method = "loadShaderpack",at= @At(value = "INVOKE", target = "Lnet/irisshaders/iris/Iris;loadExternalShaderpack(Ljava/lang/String;)Z"))
    private static void gugugagaMixin(CallbackInfo ci) {
        ShaderCompatHandler.setLoadingShader(true);
    }

    @Inject(method = "loadShaderpack",at=@At("TAIL"))
    private static void loadShaderpackMixin(CallbackInfo ci) {
        ShaderCompatHandler.setLoadingShader(false);
    }

    @Inject(method = "reload",at=@At("TAIL"))
    private static void reloadMixin(CallbackInfo ci) {
        RenderHandlerManager.updateHandler();

        SuperResolution.recreateAlgorithm();
        SuperResolutionConfig.resolutionChangeCallback.run();
    }
}
