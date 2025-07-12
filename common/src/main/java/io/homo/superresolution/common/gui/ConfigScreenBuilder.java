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
