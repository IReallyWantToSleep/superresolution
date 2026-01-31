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

package io.homo.superresolution.neoforge.compat.sodium;

#if MC_VER > MC_1_21_10

import io.homo.superresolution.common.SuperResolution;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@ConfigEntryPointForge(SuperResolution.MOD_ID)
public class SodiumConfigBuilder implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder configBuilder) {
        configBuilder.registerOwnModOptions()
                .setIcon(ResourceLocation.parse("super_resolution:textures/gui/logo.png"))
                .addPage(configBuilder.createOptionPage()
                        .setName(Component.translatable("superresolution.name"))
                        .addOption(configBuilder.createExternalButtonOption(ResourceLocation.fromNamespaceAndPath("superresolution", "settings"))
                                .setName(Component.translatable("superresolution.config_gui"))
                        )
                );
    }
}

#else
public class SodiumConfigBuilder {
}
#endif