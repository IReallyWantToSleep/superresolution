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

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.common.minecraft.handler.shadercompat.ShaderCompatHandler;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.StringPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.nio.file.Path;

@Mixin(value = Iris.class,remap = false)
public class IrisMixin {
    private static ImmutableList<StringPair> superresolution$cachedDefines;

    @Inject(method = "loadShaderpack",at= @At(value = "HEAD"))
    private static void gugugagaMixin(CallbackInfo ci) {
        ShaderCompatHandler.setCachedShaderPackPath(null);
        ShaderCompatHandler.setShaderCompatData(null);
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

    #if MC_VER > MC_1_20_1
    @Inject(
            method = "loadExternalShaderpack",
            at= @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/shaderpack/ShaderPack;<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;Z)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    #else
    @Inject(
            method = "loadExternalShaderpack",
            at= @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/shaderpack/ShaderPack;<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;)V"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    #endif
    private static void gugugagaMixin0(
            String name,
            CallbackInfoReturnable<Boolean> cir,
            Path shaderPackRoot,
            Path shaderPackConfigTxt,
            Path shaderPackPath
    ) {
        superresolution$cachedDefines = StandardMacros.createStandardEnvironmentDefines();
        ShaderCompatHandler.setCachedShaderPackPath(shaderPackPath);
        ShaderCompatHandler.loadConfig(
                shaderPackPath,
                superresolution$cachedDefines
        );
    }
    #if MC_VER > MC_1_20_1
    @ModifyArg(
            method = "loadExternalShaderpack",
            at= @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/shaderpack/ShaderPack;<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;Z)V"
            ),
            index = 2
    )
    #else
    @ModifyArg(
            method = "loadExternalShaderpack",
            at= @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/shaderpack/ShaderPack;<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;)V"
            ),
            index = 2
    )
    #endif
    private static ImmutableList<StringPair> genshinimpact(ImmutableList<StringPair> environmentDefines){
        /*
        mixin patched code:
        ImmutableList var10004 = StandardMacros.createStandardEnvironmentDefines();
        handler$bgg000$super_resolution$gugugagaMixin0(name, (CallbackInfoReturnable)null, shaderPackRoot, shaderPackConfigTxt, shaderPackPath);
        currentPack = new ShaderPack(shaderPackPath, changedConfigs, var10004, isZip);
        we need modify it`s argument
        * */
        return ImmutableList.copyOf(superresolution$cachedDefines);
    }
}
