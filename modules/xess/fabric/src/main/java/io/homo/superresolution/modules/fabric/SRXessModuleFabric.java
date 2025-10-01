package io.homo.superresolution.modules.fabric;

import io.homo.superresolution.modules.SRXessModule;
import net.fabricmc.api.ModInitializer;

public final class SRXessModuleFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SRXessModule.init();
    }
}
