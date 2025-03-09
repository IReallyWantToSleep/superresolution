package io.homo.superresolution.common.gui;

import io.homo.superresolution.common.gui.impl.ClothConfigBuilder;
import net.minecraft.client.gui.screens.Screen;

public class ConfigScreenBuilder {

    public static ConfigScreenBuilder create() {
        return new ConfigScreenBuilder();
    }

    public Screen build(Screen parentScreen) {

        ClothConfigBuilder clothConfigBuilder = new ClothConfigBuilder();
        clothConfigBuilder.setGlobalized(true);
        clothConfigBuilder.setGlobalizedExpanded(true);
        clothConfigBuilder.setParentScreen(parentScreen);
        ClothConfig.add(clothConfigBuilder);
        return clothConfigBuilder.build();
    }
}
