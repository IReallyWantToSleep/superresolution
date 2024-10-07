package io.homo.superresolution.forge;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.gui.ConfigScreenBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.client.ConfigScreenHandler;

@Mod(SuperResolution.MOD_ID)

public final class SuperResolutionForge {
    public static SuperResolution mod;
    public SuperResolutionForge() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> ConfigScreenBuilder.create().build(screen)));
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        SuperResolution.initFSR2Lib();
        MinecraftForge.EVENT_BUS.register(this);
        mod = new SuperResolution();
    }

    @Mod.EventBusSubscriber(modid = SuperResolution.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    static class ModEvent {
    }
}
