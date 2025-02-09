package io.homo.superresolution.common.gui;

import net.minecraft.client.gui.screens.Screen;

public class ConfigScreenBuilder {
    public static ConfigScreenBuilder create() {
        return new ConfigScreenBuilder();
    }
    public Screen build(Screen parentScreen) {
        return new ConfigScreen(parentScreen);
    }
}
