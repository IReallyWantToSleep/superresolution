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

package io.homo.superresolution.fabric.compat.sodium;

#if MC_VER > MC_1_21_10 || 1

import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SodiumConfigBuilder implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder configBuilder) {
        #if MC_VER > MC_1_21_10
        configBuilder.registerOwnModOptions()
                .setIcon(net.minecraft.resources.Identifier.parse("super_resolution:textures/gui/logo.png"))
                .addPage(configBuilder
                        .createExternalPage()
                        .setName(Component.translatable("superresolution.config_gui"))
                        .setScreenConsumer((screen -> Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildConfigScreen(screen))))
                );
        #else
        configBuilder.registerOwnModOptions()
                .setIcon(net.minecraft.resources.ResourceLocation.parse("super_resolution:textures/gui/logo.png"))
                .addPage(configBuilder
                        .createExternalPage()
                        .setName(Component.translatable("superresolution.config_gui"))
                        .setScreenConsumer((screen -> Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildConfigScreen(screen))))
                );
        #endif
    }
}

#else
public class SodiumConfigBuilder {

}
#endif