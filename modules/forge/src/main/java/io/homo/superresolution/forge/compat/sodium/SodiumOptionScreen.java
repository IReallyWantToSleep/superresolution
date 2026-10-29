/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
