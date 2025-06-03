package io.homo.superresolution.fabric;

import dev.architectury.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.ConfigFile;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import io.homo.superresolution.fabric.compat.sodium.SodiumOptionScreen;
import net.fabricmc.api.ClientModInitializer;

public final class SuperResolutionFabricClient implements ClientModInitializer {
    public static SuperResolution mod;

    @Override
    public void onInitializeClient() {
        ConfigFile.read();
        if (Platform.isDevelopmentEnvironment() && Config.isEnableRenderDoc()) RenderDoc.init();
        SuperResolution.preInit();
        mod = new SuperResolution();
        if (io.homo.superresolution.common.platform.Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) {
            SodiumOptionScreen.register();
        }
    }
}
