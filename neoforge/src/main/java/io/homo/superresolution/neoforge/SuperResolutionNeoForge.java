package io.homo.superresolution.neoforge;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.Config;
import io.homo.superresolution.common.config.ConfigFile;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.neoforge.compat.sodium.SodiumOptionScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@Mod(value = SuperResolution.MOD_ID, dist = Dist.CLIENT)
public final class SuperResolutionNeoForge {
    public static SuperResolution mod;

    public SuperResolutionNeoForge(ModContainer container) {
        Config.SPEC.load();
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (mc, screen) -> ConfigScreenBuilder.create().buildConfigScreen(screen));
        mod = new SuperResolution();
        SuperResolution.preInit();
        if (io.homo.superresolution.common.platform.Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) {
            SodiumOptionScreen.register();
        }
    }
}