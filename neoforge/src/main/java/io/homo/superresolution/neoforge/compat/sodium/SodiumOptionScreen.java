package io.homo.superresolution.neoforge.compat.sodium;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.SuperResolution;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
#if MC_VER != MC_1_20_4 && MC_VER < MC_1_21_6
import toni.sodiumoptionsapi.api.OptionGUIConstruction;
import toni.sodiumoptionsapi.api.OptionIdentifier;
import toni.sodiumoptionsapi.util.IOptionGroupIdAccessor;
#endif

public class SodiumOptionScreen {
    public static void register() {
        #if MC_VER != MC_1_20_4 && MC_VER < MC_1_21_6
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
        #endif
    }
}
