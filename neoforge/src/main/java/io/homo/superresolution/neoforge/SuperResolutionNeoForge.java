/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.neoforge;

import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.SuperResolutionKeyMapping;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.neoforge.compat.sodium.SodiumOptionScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@Mod(value = SuperResolution.MOD_ID, dist = Dist.CLIENT)
public final class SuperResolutionNeoForge {
    public SuperResolutionNeoForge(ModContainer container) {
        SuperResolutionConfig.SPEC.load();
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (mc, screen) -> ConfigScreenBuilder.create().buildConfigScreen(screen));
        if (Platform.currentPlatform.isModLoaded("sodiumoptionsapi")) {
            SodiumOptionScreen.register();
        }
        SuperResolution.registerEvents();
        SuperResolutionKeyMapping.registerKeyMapping();
    }
}