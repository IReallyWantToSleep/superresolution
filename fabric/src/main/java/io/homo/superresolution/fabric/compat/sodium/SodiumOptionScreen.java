package io.homo.superresolution.fabric.compat.sodium;

import com.google.common.collect.ImmutableList;
#if MC_VER > MC_1_20_4
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
#else
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import toni.sodiumoptionsapi.api.OptionGUIConstruction;

public class SodiumOptionScreen {
    public static void register() {
        OptionGUIConstruction.EVENT.register((pages) -> pages.add(
                        new OptionPage(
                                Component.translatable("superresolution.screen.config.name"), ImmutableList.of(
                                OptionGroup.createBuilder()
                                        .add(OptionImpl.createBuilder(Integer.class, new MinecraftOptionsStorage())
                                                .setBinding((Options o, Integer b) -> {
                                                }, (Options o) -> 1)
                                                .setControl((option) -> new SliderControl(option, 0, Minecraft.getInstance().getWindow().calculateScale(0, Minecraft.getInstance().isEnforceUnicode()), 1,
                                                        (a) -> Component.literal("")
                                                ))
                                                .setName(Component.literal(""))
                                                .setTooltip(Component.literal(""))
                                                .build()
                                        )
                                        .build()

                        ))
                )
        );
    }
}
