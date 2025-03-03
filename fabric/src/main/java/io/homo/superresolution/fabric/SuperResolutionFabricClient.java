package io.homo.superresolution.fabric;

import dev.architectury.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.render.renderdoc.RenderDoc;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class SuperResolutionFabricClient implements ClientModInitializer {
    public static SuperResolution mod;

    @Override
    public void onInitializeClient() {
        if (Platform.isDevelopmentEnvironment()) RenderDoc.init();
        SuperResolution.preInit();
        mod = new SuperResolution();
    }
}
