package io.homo.superresolution.neoforge;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.neoforge.compat.sodium.SodiumOptionScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@Mod(value = SuperResolution.MOD_ID, dist = Dist.CLIENT)
public final class SuperResolutionNeoForge {
    public SuperResolutionNeoForge(ModContainer container) {
        SuperResolutionConfig.SPEC.load();
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (mc, screen) -> ConfigScreenBuilder.create().buildConfigScreen(screen));
        if (io.homo.superresolution.common.platform.Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) {
            SodiumOptionScreen.register();
        }
        SuperResolution.registerEvents();
    }
}