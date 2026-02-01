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

package io.homo.superresolution.common.gui;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.gui.impl.ClothConfigBuilder;
import io.homo.superresolution.common.gui.screens.ClothStyleConfigScreen;
import io.homo.superresolution.common.gui.screens.ClothStyleInfoScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreenBuilder {

    public static ConfigScreenBuilder create() {
        return new ConfigScreenBuilder();
    }

    public Screen buildConfigScreen(Screen parentScreen) {
        SuperResolutionConfig.SPEC.load();
        ClothConfigBuilder clothConfigBuilder = new ClothConfigBuilder();
        clothConfigBuilder.setGlobalized(true);
        clothConfigBuilder.setGlobalizedExpanded(true);
        clothConfigBuilder.setParentScreen(parentScreen);
        clothConfigBuilder.setEnableSearch(true);
        clothConfigBuilder.setTitle(Component.translatable("superresolution.screen.config.name"));
        ClothConfig.add(clothConfigBuilder);
        //return TestOptionBuilder.build(parentScreen);
        return clothConfigBuilder.build(ClothStyleConfigScreen.class);
    }

    public Screen buildInfoScreen(Screen parentScreen) {
        ClothConfigBuilder clothConfigBuilder = new ClothConfigBuilder();
        clothConfigBuilder.setGlobalized(true);
        clothConfigBuilder.setGlobalizedExpanded(true);
        clothConfigBuilder.setParentScreen(parentScreen);
        clothConfigBuilder.setEnableSearch(false);
        clothConfigBuilder.setTitle(Component.translatable("superresolution.screen.info.name"));
        ClothConfig.addInfos(clothConfigBuilder);
        return clothConfigBuilder.build(ClothStyleInfoScreen.class);
    }
}
