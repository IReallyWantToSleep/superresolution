package io.homo.superresolution.shadercompat.mixin.core;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.shadercompat.SRShaderCompatConfig;
import io.homo.superresolution.shadercompat.ShaderCompatUpscaleDispatcher;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.option.OrderBackedProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

@Mixin(Iris.class)
public class IrisMixin {
    @Inject(
            method = "loadExternalShaderpack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/irisshaders/iris/shaderpack/ShaderPack;<init>(Ljava/nio/file/Path;Ljava/util/Map;Lcom/google/common/collect/ImmutableList;Z)V"

            ),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private static void loadSRShaderCompatConfig(
            String name,
            CallbackInfoReturnable<Boolean> cir,
            Path shaderPackRoot,
            Path shaderPackConfigTxt,
            Path shaderPackPath
    ) {

        try {
            Path srConfigPath = shaderPackPath.resolve("superresolution.properties");
            String srConfigContext = Files.readString(srConfigPath, StandardCharsets.ISO_8859_1);
            ShaderCompatUpscaleDispatcher.setShaderCompatConfig(SRShaderCompatConfig.load(srConfigContext));
            return;
        } catch (NoSuchFileException ignored) {

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            SuperResolution.LOGGER.info("加载 {} 光影包中的superresolution.properties时发生错误", name);
        }
        ShaderCompatUpscaleDispatcher.setShaderCompatConfig(null);

    }
}
