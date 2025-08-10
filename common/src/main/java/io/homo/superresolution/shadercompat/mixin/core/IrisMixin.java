package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.shadercompat.SRShaderCompatConfig;
import io.homo.superresolution.shadercompat.ShaderCompatUpscaleDispatcher;
import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@Mixin(Iris.class)
public class IrisMixin {

    #if MC_VER == MC_1_20_1
    @Inject(
            method = "loadExternalShaderpack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/shaderpack/ShaderPack;<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;)V"
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            remap = false
    )
    #else
    @Inject(
            method = "loadExternalShaderpack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/shaderpack/ShaderPack;<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;Z)V"
            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION,
            remap = false
    )
    #endif
    private static void loadSRShaderCompatConfig(
            String name,
            CallbackInfoReturnable<Boolean> cir,
            Path shaderPackRoot,
            Path shaderPackConfigTxt,
            Path shaderPackPath
    ) {
        try {
            Path srConfigPath = shaderPackPath.resolve("superresolution.json");
            if (Files.exists(srConfigPath)) {
                SRShaderCompatConfig cfg = SRShaderCompatConfig.loadFromJson(srConfigPath.toFile());
                ShaderCompatUpscaleDispatcher.setShaderCompatConfig(cfg);
                SuperResolution.LOGGER.info("光影包 {} 支持超分辨率功能", name);
                return;
            }
        } catch (NoSuchFileException ignored) {
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            SuperResolution.LOGGER.warn("加载 {} 光影包中的 superresolution.json 时发生错误", name);
        }
        ShaderCompatUpscaleDispatcher.setShaderCompatConfig(null);
    }
}
