package io.homo.superresolution.shadercompat.mixin.core;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftRenderHandle;
import io.homo.superresolution.shadercompat.IrisShaderPipelineHandle;
import io.homo.superresolution.shadercompat.SRCompatShaderPack;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.StringPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(StandardMacros.class)
public class StandardMacrosMixin {
    @Inject(method = "createStandardEnvironmentDefines", at = @At("TAIL"), cancellable = true, remap = false)
    private static void addSRDefines(CallbackInfoReturnable<ImmutableList<StringPair>> cir) {
        if (SuperResolutionConfig.isForceDisableShaderCompat())
            return;

        var defines = new ArrayList<>(cir.getReturnValue());
        defines.add(new StringPair("SRMOD_ENABLED", "1"));
        cir.setReturnValue(ImmutableList.copyOf(defines));
    }
}
