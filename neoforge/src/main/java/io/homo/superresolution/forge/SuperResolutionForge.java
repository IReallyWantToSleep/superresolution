package io.homo.superresolution.forge;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.gui.ConfigScreenBuilder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = SuperResolution.MOD_ID, dist = Dist.CLIENT)
public final class SuperResolutionForge {
    public static SuperResolution mod;

    public SuperResolutionForge(ModContainer container) {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (mc, screen) -> ConfigScreenBuilder.create().build(screen));
        mod = new SuperResolution();
        SuperResolution.preInit();

    }
}
