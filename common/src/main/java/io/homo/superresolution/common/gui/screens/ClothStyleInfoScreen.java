package io.homo.superresolution.common.gui.screens;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;


public class ClothStyleInfoScreen extends ClothStyleConfigScreen {
    public ClothStyleInfoScreen(Screen parent, Component title, Map<String, ConfigCategory> categoryMap, ResourceLocation backgroundLocation) {
        super(parent, title, categoryMap, backgroundLocation);
    }

    @Override
    protected void init() {
        super.init();
        this.removeWidget(exitButton);
        this.removeWidget(saveButton);
        cancelButton.setMessage(Component.translatable("superresolution.screen.button.label.return"));
        cancelButton.setX((width / 2) - (cancelButton.getWidth() / 2));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
    }
}
