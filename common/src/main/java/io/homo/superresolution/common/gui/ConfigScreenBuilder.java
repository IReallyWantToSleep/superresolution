package io.homo.superresolution.common.gui;

import io.homo.superresolution.common.gui.impl.ClothConfigBuilder;
import io.homo.superresolution.common.gui.screens.ConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class ConfigScreenBuilder {
    private final boolean clothStyle;

    public ConfigScreenBuilder(boolean clothStyle) {
        this.clothStyle = clothStyle;
    }

    public static ConfigScreenBuilder create() {
        return new ConfigScreenBuilder(true);
    }

    public static ConfigScreenBuilder create(boolean clothStyle) {
        return new ConfigScreenBuilder(clothStyle);
    }

    public Screen build(Screen parentScreen) {
        if (clothStyle) {
            ClothConfigBuilder clothConfigBuilder = new ClothConfigBuilder();
            clothConfigBuilder.setGlobalized(true);
            clothConfigBuilder.setGlobalizedExpanded(true);
            clothConfigBuilder.setParentScreen(parentScreen);
            ClothConfig.add(clothConfigBuilder);
            return clothConfigBuilder.build();
        }
        return new ConfigScreen(parentScreen);
    }
}
