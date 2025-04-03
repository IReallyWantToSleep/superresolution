package io.homo.superresolution.forge.compat.sodium;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
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
