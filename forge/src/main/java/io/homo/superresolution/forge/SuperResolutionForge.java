package io.homo.superresolution.forge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfigBuilder;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;

import io.homo.superresolution.forge.compat.sodium.SodiumOptionScreen;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.IExtensionPoint;

@Mod(value = SuperResolution.MOD_ID)
public final class SuperResolutionForge {

    public SuperResolutionForge() {
        SuperResolutionConfig.SPEC.load();
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> ConfigScreenBuilder.create().buildConfigScreen(screen)));
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        if (io.homo.superresolution.common.platform.Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) {
            SodiumOptionScreen.register();
        }
        SuperResolution.registerKeyMapping();
        SuperResolution.registerEvents();
    }
}
