package io.homo.superresolution.modules.neoforge;

import io.homo.superresolution.modules.SRXessModule;
import net.neoforged.fml.common.Mod;

@Mod(SRXessModule.MOD_ID)
public final class SRXessModuleNeoForge {
    public SRXessModuleNeoForge() {
        SRXessModule.init();
    }
}
