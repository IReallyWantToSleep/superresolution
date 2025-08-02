package io.homo.superresolution.fabric;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import net.fabricmc.api.ModInitializer;

public final class SuperResolutionFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SuperResolutionConfig.SPEC.load();
        SuperResolution.registerEvents();
    }
}
