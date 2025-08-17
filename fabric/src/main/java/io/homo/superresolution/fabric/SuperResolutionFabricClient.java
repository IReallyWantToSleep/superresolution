package io.homo.superresolution.fabric;

import dev.architectury.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.core.graphics.renderdoc.RenderDoc;
import io.homo.superresolution.fabric.compat.sodium.SodiumOptionScreen;
import net.fabricmc.api.ClientModInitializer;

public final class SuperResolutionFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (io.homo.superresolution.common.platform.Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) {
            SodiumOptionScreen.register();
        }
    }
}
